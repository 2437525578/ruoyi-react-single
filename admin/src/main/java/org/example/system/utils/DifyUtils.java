package org.example.system.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.sf.jsqlparser.util.validation.metadata.NamedObject.user;

@Component
public class DifyUtils {

    private static final Logger log = LoggerFactory.getLogger(DifyUtils.class);

    @Value("${dify.api.key}")
    private String apiKey;

    @Value("${dify.api.url}")
    private String apiUrl;

    /**
     * 发送消息给 Dify Completion 类型应用（支持 inputs 变量）
     *
     * @param query    用户输入的新闻内容
     * @param coinName 币种名称，如 "BTC"、"ETH"
     * @return AI 的回答文本
     */
    public String sendRequest(String query, String coinName) {
        // 构造 inputs，传入 coin_name
        JSONObject inputs = new JSONObject();
        inputs.set("coin_name", coinName); // ← 关键！必须传 coin_name

        JSONObject body = new JSONObject();
        body.set("inputs", inputs);
        body.set("query", query);
        body.set("response_mode", "blocking");
        body.set("user", user);

        // ✅ 正确拼接 URL：确保以 /v1/completion-messages 结尾
        String baseUrl = apiUrl.replaceFirst("/$", ""); // 去掉末尾 /
        String requestUrl = baseUrl + "";

        try {
            log.info("正在请求 Dify AI，URL: {}", requestUrl);
            log.debug("请求体: {}", body.toString());

            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(30000) // 建议增加到 30 秒
                    .body(body.toString())
                    .execute();

            if (response.isOk()) {
                String resStr = response.body();
                log.debug("Dify 原始响应: {}", resStr);
                JSONObject json = JSONUtil.parseObj(resStr);
                return json.getStr("answer", "");
            } else {
                log.error("Dify 请求失败，状态码: {}, 响应: {}", response.getStatus(), response.body());
                return "Dify 返回错误: " + response.getStatus() + " - " + response.body();
            }
        } catch (Exception e) {
            log.error("Dify 连接异常", e);
            return "AI 服务暂时不可用，请检查网络或 Key 配置。";
        }
    }
}