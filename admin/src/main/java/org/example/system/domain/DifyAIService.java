package org.example.system.domain;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DifyAIService {

    @Value("${dify.api.url}")
    private String difyApiUrl;

    @Value("${dify.api.report-key}")
    private String reportApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 调用 Dify 助手，对输入文本进行分析
     * @param inputText 用户输入的消息内容
     * @return AI 分析结果（JSON 字符串）
     */
    public String analyzeMessage(String inputText) {

        if (inputText == null || inputText.trim().isEmpty()) {
            return "消息内容为空，无法进行 AI 分析";
        }
        
        // 注意：聊天助手的接口地址是 /chat-messages
        String url = difyApiUrl + "/chat-messages";

        // 构造请求体
        JSONObject body = new JSONObject();
        JSONObject inputs = new JSONObject();
        inputs.set("text", inputText);
        
        body.set("inputs", inputs);
        body.set("query", "请根据提供的持仓和市场数据生成分析报告"); // 聊天助手必须有 query
        body.set("response_mode", "blocking");
        body.set("user", "system");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + reportApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject resJson = JSONUtil.parseObj(response.getBody());
                // 聊天助手的结果在 answer 字段中
                return resJson.getStr("answer");
            } else {
                return "AI 分析失败: " + response.getStatusCode();
            }
        } catch (Exception e) {
            return "AI 调用异常: " + e.getMessage();
        }
    }
}