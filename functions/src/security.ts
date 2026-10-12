import { lookup } from "node:dns/promises";
import { request as httpsRequest } from "node:https";
import type { LookupOptions } from "node:dns";
import ipaddr from "ipaddr.js";

export const ALLOWED_BIASES = ["neutral", "positive", "critical", "simplified"] as const;
export type AIAssistBias = (typeof ALLOWED_BIASES)[number];

export interface AIAssistInput {
  itemId: string;
  bias: AIAssistBias;
}

const MAX_RESPONSE_BYTES = 1_000_000;
const MAX_REDIRECTS = 3;
const FETCH_TIMEOUT_MS = 10_000;

export function parseAIAssistInput(value: unknown): AIAssistInput | null {
  if (typeof value !== "object" || value === null) return null;

  const candidate = value as Record<string, unknown>;
  const itemId = typeof candidate.itemId === "string" ? candidate.itemId.trim() : "";
  const bias = candidate.bias;

  if (!/^[A-Za-z0-9_-]{1,128}$/.test(itemId)) return null;
  if (typeof bias !== "string" || !ALLOWED_BIASES.includes(bias as AIAssistBias)) return null;

  return { itemId, bias: bias as AIAssistBias };
}

export function isPublicIpAddress(address: string): boolean {
  const normalized = address.toLowerCase().split("%")[0];

  try {
    const parsed = ipaddr.parse(normalized);
    if (parsed instanceof ipaddr.IPv6 && parsed.isIPv4MappedAddress()) {
      return isPublicIpAddress(parsed.toIPv4Address().toString());
    }
    return parsed.range() === "unicast";
  } catch {
    return false;
  }
}

async function resolvePublicAddress(hostname: string): Promise<{ address: string; family: number }> {
  const addresses = await lookup(hostname, { all: true, verbatim: true });
  if (addresses.length === 0 || addresses.some(({ address }) => !isPublicIpAddress(address))) {
    throw new Error("Destination is not publicly routable");
  }
  return addresses[0];
}

function validateHttpsUrl(rawUrl: string): URL {
  let url: URL;
  try {
    url = new URL(rawUrl);
  } catch {
    throw new Error("Invalid article URL");
  }

  if (
    url.protocol !== "https:" ||
    url.username !== "" ||
    url.password !== "" ||
    (url.port !== "" && url.port !== "443")
  ) {
    throw new Error("Only credential-free HTTPS URLs on port 443 are allowed");
  }

  return url;
}

async function fetchOnce(url: URL): Promise<{ body?: string; redirect?: URL }> {
  const pinned = await resolvePublicAddress(url.hostname);

  return new Promise((resolve, reject) => {
    const req = httpsRequest(
      url,
      {
        headers: {
          Accept: "text/html,application/xhtml+xml;q=0.9,text/plain;q=0.8",
          "User-Agent": "Mealoo-AIAssist/1.0",
        },
        lookup: (
          _hostname: string,
          _options: LookupOptions,
          callback: (error: NodeJS.ErrnoException | null, address: string, family: number) => void
        ) => callback(null, pinned.address, pinned.family),
      },
      (response) => {
        const status = response.statusCode ?? 0;
        if (status >= 300 && status < 400) {
          const location = response.headers.location;
          response.resume();
          if (!location) {
            reject(new Error("Redirect response did not include a location"));
            return;
          }
          try {
            resolve({ redirect: new URL(location, url) });
          } catch {
            reject(new Error("Redirect URL is invalid"));
          }
          return;
        }

        if (status < 200 || status >= 300) {
          response.resume();
          reject(new Error(`Article server returned HTTP ${status}`));
          return;
        }

        const contentType = String(response.headers["content-type"] ?? "").toLowerCase();
        if (!contentType.includes("text/html") && !contentType.includes("text/plain")) {
          response.resume();
          reject(new Error("Article response is not text or HTML"));
          return;
        }

        const chunks: Buffer[] = [];
        let totalBytes = 0;
        response.on("data", (chunk: Buffer) => {
          totalBytes += chunk.length;
          if (totalBytes > MAX_RESPONSE_BYTES) {
            response.destroy(new Error("Article response exceeded the size limit"));
            return;
          }
          chunks.push(chunk);
        });
        response.on("end", () => resolve({ body: Buffer.concat(chunks).toString("utf8") }));
        response.on("error", reject);
      }
    );

    req.setTimeout(FETCH_TIMEOUT_MS, () => req.destroy(new Error("Article request timed out")));
    req.on("error", reject);
    req.end();
  });
}

export async function fetchPublicArticle(rawUrl: string): Promise<string> {
  let current = validateHttpsUrl(rawUrl);

  for (let redirectCount = 0; redirectCount <= MAX_REDIRECTS; redirectCount += 1) {
    const result = await fetchOnce(current);
    if (result.body !== undefined) return result.body;
    if (!result.redirect) throw new Error("Article request returned no content");
    current = validateHttpsUrl(result.redirect.toString());
  }

  throw new Error("Article request exceeded the redirect limit");
}
