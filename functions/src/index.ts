import * as functions from "firebase-functions/v1";
import * as admin from "firebase-admin";
import axios from "axios";
import { OpenAI } from "openai";
import { buildPrompt } from "./buildPrompt";
import { extractMainText } from "./extractMainText";

admin.initializeApp();
const db = admin.firestore();

// 声明依赖 secret
export const generateAIAssist = functions
  .runWith({
    secrets: ["OPENAI_API_KEY"],  // 👈 使用 secret
    memory: "512MB",              // 可选：防止 OOM
    timeoutSeconds: 60,           // 可选：防止超时
  })
  .https.onCall(async (request, context) => {
    const { itemId, bias } = request.data as { itemId: string; bias: string };

    // 从 secret 取出 API key
    const apiKey = process.env.OPENAI_API_KEY;
    if (!apiKey) {
      throw new functions.https.HttpsError("failed-precondition", "Missing OPENAI_API_KEY secret");
    }

    const openai = new OpenAI({ apiKey });

    // Firestore 文档
    const docRef = db.collection("public/info/items").doc(itemId);
    const doc = await docRef.get();
    if (!doc.exists) {
      throw new functions.https.HttpsError("not-found", "Item not found");
    }

    const url = doc.get("url");
    if (!url) {
      throw new functions.https.HttpsError("invalid-argument", "No URL field in document");
    }

    // 抓取正文
    let content = "";
    try {
      const response = await axios.get(url);
      content = extractMainText(response.data);
    } catch (e) {
      throw new functions.https.HttpsError("internal", "Failed to fetch or parse article");
    }

    // Prompt
    const prompt = buildPrompt(content, bias);

    // OpenAI 调用
    let text = "";
    try {
      const completion = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: "你是一个帮助用户理解科学文章的助手。" },
          { role: "user", content: prompt },
        ],
        max_tokens: 500,
      });
      text = completion.choices[0].message?.content ?? "";
    } catch (e) {
      throw new functions.https.HttpsError("internal", "OpenAI request failed");
    }

    // 回写 Firestore
    await docRef.set(
      {
        aiAssist: {
          [bias]: text,
        },
      },
      { merge: true }
    );

    return { result: text };
  });
