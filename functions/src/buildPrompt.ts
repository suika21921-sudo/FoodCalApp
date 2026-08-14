// buildPrompt.ts
/**
 * 根据文章正文和偏向（bias）生成不同风格的 prompt
 * bias: "neutral" | "positive" | "critical" | "simplified"
 */
export function buildPrompt(content: string, bias: string): string {
  let instruction = "";

  switch (bias) {
    case "positive":
      instruction = "请用积极正面的角度，总结并强调该文章中对健康、科学进步或社会有益的观点。";
      break;
    case "critical":
      instruction = "请用批判性思维，总结该文章可能存在的不足、偏见或证据不足之处。";
      break;
    case "simplified":
      instruction = "请用简单易懂的方式（适合普通大众或小白读者），解释文章的主要内容。";
      break;
    case "neutral":
    default:
      instruction = "请保持中立和客观，总结文章的核心观点与关键信息。";
      break;
  }

  return `
文章内容如下：
----------------
${content}
----------------

你的任务：
${instruction}
请生成一段简明扼要的介绍，字数控制在300字以内。
`;
}
