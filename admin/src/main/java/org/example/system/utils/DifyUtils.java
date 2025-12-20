package org.example.system.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DifyUtils {

    private static final Logger log = LoggerFactory.getLogger(DifyUtils.class);

    @Value("${dify.api.news-key}")
    private String newsApiKey;

    @Value("${dify.api.metrics-key}")
    private String metricsApiKey;

    @Value("${dify.api.report-key}")
    private String reportApiKey;

    @Value("${dify.api.url}")
    private String apiUrl;

    /**
     * 发送消息给 Dify 聊天助手（用于市场情报新闻）
     */
    public String sendRequest(String query, String user) {
        return sendToDify(query, user, newsApiKey, "市场情报");
    }

    /**
     * 发送消息给 Dify 聊天助手（用于投资建议报告）
     */
    public String sendReportRequest(String query, String user) {
        return sendToDify(query, user, reportApiKey, "投资建议报告");
    }

    /**
     * 通用的 Dify 请求方法
     */
    private String sendToDify(String query, String user, String apiKey, String logTag) {
        JSONObject inputs = new JSONObject();
        inputs.set("coin_name", "ALL");

        JSONObject body = new JSONObject();
        body.set("inputs", inputs);
        body.set("query", query);
        body.set("response_mode", "blocking");
        body.set("user", user);

        String requestUrl = apiUrl + "/chat-messages";
        try {
            log.info("正在请求 Dify AI {}: {}", logTag, query);
            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(300000) // 5分钟超时
                    .execute();

            if (response.isOk()) {
                String resStr = response.body();
                JSONObject json = JSONUtil.parseObj(resStr);
                return json.getStr("answer");
            } else {
                log.error("Dify {} 请求失败: {}，响应内容: {}", logTag, response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("Dify {} 连接异常", logTag, e);
        }
        return null;
    }

    /**
     * 采集最新比特币市场数据（使用行情专用 Key）
     */
    public String collectBtcMetrics() {
        String query = "请立即返回当前比特币最新行情，**严格只输出纯 JSON**，不要有任何解释或标签：\n" +
                "{\n" +
                " \"name\": \"比特币\",\n" +
                " \"price_usd\": 当前价格,\n" +
                " \"market_cap\": 当前市值亿美元,\n" +
                " \"hash_rate\": 哈希率变化,\n" +
                " \"24h_change\": 24小时涨跌幅,\n" +
                " \"transaction_count\": \"流通量\",\n" +
                " \"total_fees_btc\": 总手续费,\n" +
                " \"block_count\": 区块指标,\n" +
                " \"ath_price\": 历史最高价\n" +
                "}";

        JSONObject inputs = new JSONObject();
        inputs.set("coin_name", "BTC");

        JSONObject body = new JSONObject();
        body.set("inputs", inputs);
        body.set("query", query);
        body.set("response_mode", "blocking");
        body.set("user", "btc-collector");

        String requestUrl = apiUrl + "/chat-messages";
        try {
            log.info("正在采集比特币市场数据（使用行情专用 Key）");
            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("Authorization", "Bearer " + metricsApiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(300000)
                    .execute();

            if (response.isOk()) {
                String resStr = response.body();
                JSONObject json = JSONUtil.parseObj(resStr);
                String answer = json.getStr("answer");
                log.info("Dify 返回比特币数据: {}", answer);

                String cleanJson = answer.trim();

                if (cleanJson.contains("<think>")) {
                    cleanJson = cleanJson.replaceAll("<think>.*?</think>", "").trim();
                }

                Matcher matcher = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(cleanJson);
                if (matcher.find()) {
                    cleanJson = matcher.group(0);
                }

                log.info("提取后的纯 JSON: {}", cleanJson);
                return cleanJson;
            } else {
                log.error("比特币数据采集请求失败: {}，响应内容: {}", response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("采集比特币市场数据异常", e);
        }
        return null;
    }

    /**
     * 采集多个主流虚拟货币的最新行情数据（强制返回下划线格式，包含所有字段）
     */
    public String collectMultiCryptoMetrics() {
        String query = "你必须严格按照以下格式立即返回15种主流加密货币的最新行情数据，**字段名必须使用下划线**，禁止使用驼峰格式，禁止返回任何解释、思考、<think>标签或额外文字，只返回纯 JSON：\n" +
                "{\n" +
                "  \"data\": [\n" +
                "    {\"name\": \"比特币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"以太坊\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"泰达币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"币安币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"索拉纳\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"瑞波币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"USDC\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"狗狗币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"艾达币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"波场\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"吨币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"波卡\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"链节币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"柴犬币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元},\n" +
                "    {\"name\": \"莱特币\", \"price_usd\": 当前价格, \"market_cap\": 当前市值亿美元, \"hash_rate\": 7天涨跌幅百分比, \"24h_change\": 24小时涨跌幅百分比, \"transaction_count\": \"流通量字符串\", \"total_fees_btc\": 24h最高价, \"block_count\": 24h最低价, \"ath_price\": 历史最高价美元}\n" +
                "  ]\n" +
                "}\n" +
                "注意：hash_rate字段请填充7天涨跌幅百分比数值，total_fees_btc填充24h最高价，block_count填充24h最低价。所有数字字段必须是数字。必须使用最新真实数据填充。直接输出 JSON。";


        JSONObject inputs = new JSONObject();
        inputs.set("coin_name", "MULTI");

        JSONObject body = new JSONObject();
        body.set("inputs", inputs);
        body.set("query", query);
        body.set("response_mode", "blocking");
        body.set("user", "metrics-collector");

        String requestUrl = apiUrl + "/chat-messages";

        try {
            log.info("正在采集多币种行情数据...");

            // 第一次请求
            HttpResponse response = HttpRequest.post(requestUrl)
                    .header("Authorization", "Bearer " + metricsApiKey)
                    .header("Content-Type", "application/json")
                    .body(body.toString())
                    .timeout(300000)
                    .execute();

            // 重试一次
            if (!response.isOk()) {
                log.warn("第一次请求失败（{}），准备重试...", response.getStatus());
                response = HttpRequest.post(requestUrl)
                        .header("Authorization", "Bearer " + metricsApiKey)
                        .header("Content-Type", "application/json")
                        .body(body.toString())
                        .timeout(300000)
                        .execute();
            }

            if (response.isOk()) {
                String resStr = response.body();
                JSONObject json = JSONUtil.parseObj(resStr);
                String answer = json.getStr("answer");

                if (answer == null || answer.trim().isEmpty()) {
                    log.error("Dify 返回 answer 为空");
                    return null;
                }

                String cleanJson = answer.trim();

                if (cleanJson.contains("<think>")) {
                    cleanJson = cleanJson.replaceAll("<think>.*?</think>", "").trim();
                }

                Matcher matcher = Pattern.compile("\\{.*\\}", Pattern.DOTALL).matcher(cleanJson);
                if (matcher.find()) {
                    cleanJson = matcher.group(0);
                } else {
                    log.error("未能提取到有效的 JSON 对象结构");
                    return null;
                }

                try {
                    JSONUtil.parseObj(cleanJson);
                    log.info("成功提取多币种行情纯 JSON: {}", cleanJson);
                    return cleanJson;
                } catch (Exception e) {
                    log.error("提取的 JSON 格式无效: {}", cleanJson);
                    return null;
                }
            } else {
                log.error("多币种行情采集失败（含重试）: {}，响应: {}", response.getStatus(), response.body());
            }
        } catch (Exception e) {
            log.error("采集多币种行情异常", e);
        }
        return null;
    }
}