package org.example.system.service.impl;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizInvestmentReportMapper;
import org.example.system.domain.BizInvestmentReport;
import org.example.system.service.IBizInvestmentReportService;
import org.example.system.service.IBizAssetHoldingsService;
import org.example.system.service.IBizCryptoMessageService;
import org.example.system.service.IBizCryptoMetricsService;
import org.example.system.domain.BizAssetHoldings;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.domain.BizCryptoMetrics;
import org.example.system.utils.DifyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BizInvestmentReportServiceImpl implements IBizInvestmentReportService
{
    private static final Logger log = LoggerFactory.getLogger(BizInvestmentReportServiceImpl.class);

    @Autowired
    private BizInvestmentReportMapper bizInvestmentReportMapper;
    @Autowired
    private IBizAssetHoldingsService holdingsService;
    @Autowired
    private IBizCryptoMessageService messageService;
    @Autowired
    private IBizCryptoMetricsService metricsService;
    @Autowired
    private DifyUtils difyUtils;
    @Override
    public BizInvestmentReport selectBizInvestmentReportById(Long id)
    {
        return bizInvestmentReportMapper.selectBizInvestmentReportById(id);
    }

    @Override
    public List<BizInvestmentReport> selectBizInvestmentReportList(BizInvestmentReport bizInvestmentReport)
    {
        return bizInvestmentReportMapper.selectBizInvestmentReportList(bizInvestmentReport);
    }

    @Override
    public int insertBizInvestmentReport(BizInvestmentReport bizInvestmentReport)
    {
        bizInvestmentReport.setCreateTime(DateUtils.getNowDate());
        return bizInvestmentReportMapper.insertBizInvestmentReport(bizInvestmentReport);
    }

    @Override
    // 建议加上事务注解
    @org.springframework.transaction.annotation.Transactional
    public int updateBizInvestmentReport(BizInvestmentReport report)
    {
        // 1. 先执行更新状态
        report.setUpdateTime(DateUtils.getNowDate());
        int rows = bizInvestmentReportMapper.updateBizInvestmentReport(report);

        // 2. 如果状态变为 "1" (已通过)，执行持仓调整逻辑
        if ("1".equals(report.getStatus())) {
            executeAutoTrade(report.getId());
        }

        return rows;
    }

    /**
     * 执行 AI 建议的自动交易指令
     */
    private void executeAutoTrade(Long reportId) {
        BizInvestmentReport report = this.selectBizInvestmentReportById(reportId);
        if (report == null || report.getExecuteJson() == null || report.getExecuteJson().isEmpty()) {
            return;
        }

        try {
            JSONArray actions = JSONUtil.parseArray(report.getExecuteJson());
            for (int i = 0; i < actions.size(); i++) {
                JSONObject action = actions.getJSONObject(i);
                String type = action.getStr("type");
                String coin = action.getStr("coin");
                BigDecimal amount = action.getBigDecimal("amount", BigDecimal.ZERO);
                BigDecimal price = action.getBigDecimal("price", BigDecimal.ZERO);

                if (coin == null || coin.isEmpty() || amount.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 在现有持仓中寻找匹配的币种 (优先完全匹配，其次忽略大小写匹配，同时检查 Symbol 和 Name)
                BizAssetHoldings targetHolding = null;
                List<BizAssetHoldings> allHoldings = holdingsService.selectBizAssetHoldingsList(new BizAssetHoldings());
                
                // 获取行情数据以便更准确地匹配
                List<BizCryptoMetrics> latestMetrics = metricsService.selectLatestMetrics();
                Map<String, String> nameToSymbolMap = new HashMap<>();
                for (BizCryptoMetrics m : latestMetrics) {
                    if (m.getName() != null && m.getSymbol() != null) {
                        nameToSymbolMap.put(m.getName(), m.getSymbol());
                    }
                }

                String searchSymbol = coin;
                if (nameToSymbolMap.containsKey(coin)) {
                    searchSymbol = nameToSymbolMap.get(coin);
                }

                for (BizAssetHoldings h : allHoldings) {
                    String hCoin = h.getCoin();
                    String hSymbol = nameToSymbolMap.getOrDefault(hCoin, hCoin);
                    
                    if (hCoin.equalsIgnoreCase(coin) || hSymbol.equalsIgnoreCase(searchSymbol)) {
                        targetHolding = h;
                        break;
                    }
                }

                if ("BUY".equalsIgnoreCase(type)) {
                    if (targetHolding != null) {
                        // 更新现有持仓
                        BigDecimal oldAmount = targetHolding.getAmount() != null ? targetHolding.getAmount() : BigDecimal.ZERO;
                        BigDecimal oldCost = targetHolding.getCostPrice() != null ? targetHolding.getCostPrice() : BigDecimal.ZERO;
                        BigDecimal newAmount = oldAmount.add(amount);
                        
                        // 计算平均持仓成本
                        if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
                            BigDecimal totalCost = oldAmount.multiply(oldCost).add(amount.multiply(price));
                            targetHolding.setCostPrice(totalCost.divide(newAmount, 8, java.math.RoundingMode.HALF_UP));
                        }
                        
                        targetHolding.setAmount(newAmount);
                        if (price.compareTo(BigDecimal.ZERO) > 0) {
                            targetHolding.setUsdtValue(newAmount.multiply(price));
                        }
                        holdingsService.updateBizAssetHoldings(targetHolding);
                        log.info("买入成功: {}, 增加数量: {}, 新总量: {}, 新均价: {}", coin, amount, newAmount, targetHolding.getCostPrice());
                    } else {
                        // 新增持仓
                        BizAssetHoldings h = new BizAssetHoldings();
                        h.setCoin(coin);
                        h.setAmount(amount);
                        h.setCostPrice(price);
                        if (price.compareTo(BigDecimal.ZERO) > 0) {
                            h.setUsdtValue(amount.multiply(price));
                        }
                        h.setCreateBy("AI_AUTO");
                        holdingsService.insertBizAssetHoldings(h);
                        log.info("新增持仓: {}, 数量: {}, 价格: {}", coin, amount, price);
                    }
                } else if ("SELL".equalsIgnoreCase(type)) {
                    if (targetHolding != null) {
                        BigDecimal currentAmount = targetHolding.getAmount();
                        // 卖出数量不能超过持有数量
                        BigDecimal sellAmount = amount.compareTo(currentAmount) > 0 ? currentAmount : amount;
                        BigDecimal newAmount = currentAmount.subtract(sellAmount);
                        
                        if (newAmount.compareTo(BigDecimal.ZERO) <= 0) {
                            // 如果清仓了，直接删除记录
                            holdingsService.deleteBizAssetHoldingsById(targetHolding.getId());
                            log.info("卖出成功: {} 已清仓，记录已移除", targetHolding.getCoin());
                        } else {
                            // 部分卖出
                            targetHolding.setAmount(newAmount);
                            if (price.compareTo(BigDecimal.ZERO) > 0) {
                                targetHolding.setUsdtValue(newAmount.multiply(price));
                            }
                            holdingsService.updateBizAssetHoldings(targetHolding);
                            log.info("卖出成功: {}, 减少数量: {}, 剩余数量: {}", coin, sellAmount, newAmount);
                        }
                    } else {
                        log.warn("卖出失败: 未找到币种 {} 的持仓", coin);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("执行自动调仓失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int deleteBizInvestmentReportByIds(Long[] ids)
    {
        return bizInvestmentReportMapper.deleteBizInvestmentReportByIds(ids);
    }

    @Override
    public int deleteBizInvestmentReportById(Long id)
    {
        return bizInvestmentReportMapper.deleteBizInvestmentReportById(id);
    }

    @Override
    public void generateSummaryReport() {
        log.info(">>> 开始生成汇总投资建议报告...");

        // 1. 获取最近的 20 条市场情报
        BizCryptoMessage messageQuery = new BizCryptoMessage();
        // 这里假设 selectBizCryptoMessageList 返回的是按时间倒序或者我们可以自己处理
        List<BizCryptoMessage> messages = messageService.selectBizCryptoMessageList(messageQuery);
        if (messages.size() > 20) {
            messages = messages.subList(0, 20);
        }
        
        StringBuilder newsSummary = new StringBuilder();
        for (BizCryptoMessage msg : messages) {
            newsSummary.append(String.format("- [%s] %s (影响分数: %s)\n", 
                msg.getCoin(), msg.getContent(), msg.getImpactScore()));
        }

        // 2. 获取当前所有持仓
        BizAssetHoldings holdingsQuery = new BizAssetHoldings();
        List<BizAssetHoldings> holdings = holdingsService.selectBizAssetHoldingsList(holdingsQuery);
        String holdingsJson = JSONUtil.toJsonStr(holdings);

        // 3. 获取最新行情数据
        List<BizCryptoMetrics> metrics = metricsService.selectBizCryptoMetricsList(new BizCryptoMetrics());
        String metricsJson = JSONUtil.toJsonStr(metrics);

        // 4. 构建汇总 Prompt
        String prompt = String.format(
                "【角色】你是一位资深的加密货币策略分析师。\n" +
                        "【背景】你需要基于以下多维数据给出综合投资策略建议。\n" +
                        "【市场情报摘要】\n%s\n" +
                        "【当前持仓】%s\n" +
                        "【最新行情】%s\n" +
                        "【任务】请综合分析上述所有情报、当前持仓以及市场行情，给出最终的投资策略。\n" +
                        "【硬性约束】\n" +
                        "1. 如果你建议卖出（SELL），建议卖出的数量（amount）绝对不能超过该币种的当前持有数量。\n" +
                        "2. 如果该币种当前持仓为 0，你不能建议卖出（SELL），只能建议买入（BUY）或持有（HOLD）。\n" +
                        "3. 请务必平衡多条新闻的影响。如果新闻之间存在矛盾，请给出你的专业判断。\n" +
                        "4. 分析整体市场趋势，而不仅仅是单一币种。\n" +
                        "【输出格式要求】请务必返回一个合法的 JSON 字符串，包含以下字段：\n" +
                        "1. advice: 综合建议内容，要求逻辑清晰、专业、深入，包含对整体趋势的判断。\n" +
                        "2. actions: 一个数组，包含具体的操作指令。每个操作包含：\n" +
                        "   - type: 操作类型，可选值：BUY, SELL, HOLD\n" +
                        "   - coin: 币种名称（请务必使用上述【当前持仓】中提供的 coin 字段值，如“比特币”或“ETH”）\n" +
                        "   - amount: 建议操作的数量（必须大于0，SELL时不能超过持仓量）\n" +
                        "   - price: 建议操作的价格（参考当前行情）\n" +
                        "示例格式：{\"advice\": \"...\", \"actions\": [{\"type\": \"BUY\", \"coin\": \"BTC\", \"amount\": 0.05, \"price\": 65000}]}\n" +
                        "请直接输出 JSON，不要包含 Markdown 代码块标记。",
                newsSummary.toString(), holdingsJson, metricsJson
        );

        // 5. 调用 AI
        String aiResponse = difyUtils.sendReportRequest(prompt, "system_summary_analyst");
        
        processAndSaveReport(aiResponse, null, "综合分析");
    }

    /**
     * 通用的处理和保存报告逻辑
     */
    private void processAndSaveReport(String aiResponse, Long messageId, String defaultAnalysis) {
        String advice = "解析建议失败";
        String actionsJson = "[]";

        if (aiResponse != null && !aiResponse.isEmpty()) {
            log.info("Dify 返回原始数据: {}", aiResponse);
            String jsonContent = extractJson(aiResponse);
            log.info("提取后的 JSON: {}", jsonContent);
            
            try {
                JSONObject json = JSONUtil.parseObj(jsonContent);
                advice = json.getStr("advice");
                actionsJson = JSONUtil.toJsonStr(json.getJSONArray("actions"));
            } catch (Exception e) {
                log.error("解析 JSON 失败. 提取内容: {}", jsonContent, e);
                advice = aiResponse;
            }
        }

        BizInvestmentReport report = new BizInvestmentReport();
        report.setMessageId(messageId);
        report.setAnalysisResult(defaultAnalysis);
        report.setAdviceContent(advice);
        report.setExecuteJson(actionsJson);
        report.setStatus("0");
        report.setCreateTime(DateUtils.getNowDate());

        this.insertBizInvestmentReport(report);
        log.info(">>> 投资建议报告已生成并保存。");
    }

    @Override
    public void generateReport(Long messageId) {
        // 1. 获取新闻
        BizCryptoMessage message = messageService.selectBizCryptoMessageById(messageId);
        if (message == null) return;

        // 2. 获取当前所有持仓
        BizAssetHoldings holdingsQuery = new BizAssetHoldings();
        List<BizAssetHoldings> holdings = holdingsService.selectBizAssetHoldingsList(holdingsQuery);
        String holdingsJson = JSONUtil.toJsonStr(holdings);

        // 3. 获取最新行情数据
        List<BizCryptoMetrics> metrics = metricsService.selectBizCryptoMetricsList(new BizCryptoMetrics());
        String metricsJson = JSONUtil.toJsonStr(metrics);

        // 4. 构建 Prompt (针对单条新闻)
        String prompt = String.format(
                "【角色】你是一位严谨的数字货币投资顾问。\n" +
                        "【背景】我收到一条新闻：%s (涉及币种:%s, 情感:%s)。\n" +
                        "【现状】我的当前持有资产如下：%s。\n" +
                        "【行情】最新市场行情如下：%s。\n" +
                        "【任务】请结合新闻、当前持仓以及市场行情，给出投资建议。\n" +
                        "【硬性约束】\n" +
                        "1. 如果你建议卖出（SELL），建议卖出的数量（amount）绝对不能超过该币种的当前持有数量。\n" +
                        "2. 如果该币种当前持仓为 0，你不能建议卖出（SELL），只能建议买入（BUY）或持有（HOLD）。\n" +
                        "3. 请分析新闻对该币种的具体影响，结合当前价格和你的持仓成本进行盈亏分析。\n" +
                        "【输出格式要求】请务必返回一个合法的 JSON 字符串，包含以下字段：\n" +
                        "1. advice: 建议内容，要求专业、逻辑严密，并解释为什么要操作这个数量。\n" +
                        "2. actions: 一个数组，包含具体的操作指令。每个操作包含：\n" +
                        "   - type: 操作类型，可选值：BUY, SELL, HOLD\n" +
                        "   - coin: 币种名称（请务必使用上述【现状】中提供的 coin 字段值，如“比特币”或“ETH”）\n" +
                        "   - amount: 建议操作的数量（必须大于0，SELL时不能超过持仓量）\n" +
                        "   - price: 建议操作的价格（参考当前行情）\n" +
                        "示例格式：{\"advice\": \"...\", \"actions\": [{\"type\": \"SELL\", \"coin\": \"BTC\", \"amount\": 0.1, \"price\": 27000}]}\n" +
                        "请直接输出 JSON，不要包含 Markdown 代码块标记。",
                message.getContent(), message.getCoin(), message.getSentiment(), holdingsJson, metricsJson
        );

        // 5. 调用 AI
        String aiResponse = difyUtils.sendReportRequest(prompt, "system_analyst");
        
        processAndSaveReport(aiResponse, messageId, message.getSentiment());
    }

    /**
     * 从字符串中提取 JSON 内容
     */
    private String extractJson(String text) {
        if (text == null) return null;

        String cleanText = text.trim();

        // 1. 移除 DeepSeek 等模型的 <think> 标签
        if (cleanText.contains("<think>")) {
            cleanText = cleanText.replaceAll("<think>.*?</think>", "").trim();
        }

        // 2. 尝试直接去掉 markdown 标记
        String cleaned = cleanText.replace("```json", "").replace("```", "").trim();
        if (cleaned.startsWith("{") && cleaned.endsWith("}")) {
            return cleaned;
        }

        // 3. 正则提取第一个 { ... }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\{.*\\}", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher matcher = pattern.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group();
        }

        return cleaned;
    }
}