package org.example.system.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DifyUtils {

    private static final Logger log = LoggerFactory.getLogger(DifyUtils.class);

    // 在 application.yml 中配置
    @Value("${dify.api.key}")
    private String apiKey;

    @Value("${dify.api.url}")
    private String apiUrl; // 例如: https://api.dify.ai/v1

    /**
     * 发送消息给 Dify 聊天助手
     * @param query 用户提示词
     * @param user 用户标识
     * @return AI 的回答
     */
    public String sendRequest(String query, String user) {
        JSONObject body = new JSONObject();
        body.set("inputs", new JSONObject());
        body.set("query", query);
        body.set("response_mode", "blocking");
        body.set("user", user);

        // 拼接完整的 API 地址 (假设使用 Chat-Messages 接口)
        String requestUrl = apiUrl + "/chat-messages";

        try {
            log.info("正在请求 Dify AI: {}", query);
            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .execute();

            if (response.isOk()) {
                String resStr = response.body();
                JSONObject json = JSONUtil.parseObj(resStr);
                // Dify 返回结构通常是 answer 字段
                return json.getStr("answer");
            } else {
                log.error("Dify 请求失败: {}", response.getStatus());
            }
        } catch (Exception e) {
            log.error("Dify 连接异常", e);
        }
        return "AI 服务暂时不可用，请检查网络或 Key 配置。";
    }
}
