// extractMainText.ts
import * as cheerio from "cheerio";

/**
 * 从 HTML 提取正文文本（简化版）
 * - 去掉脚本/样式
 * - 按段落合并
 */
export function extractMainText(html: string): string {
  const $ = cheerio.load(html);

  // 移除无用标签
  $("script, style, noscript, iframe, header, footer, nav, aside").remove();

  // 尝试提取 <article> 或正文段落
  let mainText = "";

  if ($("article").length > 0) {
    mainText = $("article").text();
  } else {
    // 否则拼接所有段落
    $("p").each((_, el) => {
      const text = $(el).text().trim();
      if (text.length > 0) {
        mainText += text + "\n\n";
      }
    });
  }

  // 压缩多余空格和换行
  return mainText.replace(/\s+/g, " ").trim();
}
