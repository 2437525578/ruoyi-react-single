package org.example.system.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DifyAIService {

    @Value("${dify.api.url}")
    private String difyApiUrl;

    @Value("${dify.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用 Dify 工作流，对输入文本进行分析
     * @param inputText 用户输入的消息内容
     * @return AI 分析结果（JSON 字符串）
     */
    public String analyzeMessage(String inputText) {

        if (inputText == null || inputText.trim().isEmpty()) {
            return "消息内容为空，无法进行 AI 分析";
        }
        String url = difyApiUrl + "/api/v1/workflow/run";

        // 构造请求体（根据你的 Dify workflow 调整）
        String requestBody = String.format(
                "{\"inputs\":{\"text\":\"%s\"},\"response_mode\":\"blocking\",\"user\":\"system\"}",
                inputText.replace("\"", "\\\"") // 转义引号
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return extractAnswer(response.getBody());
            } else {
                return "AI 分析失败: " + response.getStatusCode();
            }
        } catch (Exception e) {
            return "AI 调用异常: " + e.getMessage();
        }
    }

    /**
     * 从 Dify 返回的 JSON 中提取 answer 内容
     * 示例响应结构：
     * { "data": { "outputs": { "text": "分析结果..." } } }
     */
    private String extractAnswer(String responseBody) {
        // 简单解析（生产环境建议用 Jackson）
        int start = responseBody.indexOf("\"text\":\"");
        if (start == -1) return "无法解析 AI 响应";
        start += 8;
        int end = responseBody.indexOf("\"", start);
        return responseBody.substring(start, end).replace("\\n", "\n");
    }
}