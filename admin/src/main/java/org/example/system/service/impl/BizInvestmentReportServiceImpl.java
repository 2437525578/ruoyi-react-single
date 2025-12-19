package org.example.system.service.impl;

import java.util.List;

import cn.hutool.json.JSONUtil;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizInvestmentReportMapper;
import org.example.system.domain.BizInvestmentReport;
import org.example.system.service.IBizInvestmentReportService;
import org.example.system.service.IBizAssetHoldingsService;
import org.example.system.service.IBizCryptoMessageService;
import org.example.system.domain.BizAssetHoldings;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.utils.DifyUtils;
@Service
public class BizInvestmentReportServiceImpl implements IBizInvestmentReportService
{
    @Autowired
    private BizInvestmentReportMapper bizInvestmentReportMapper;
    @Autowired
    private IBizAssetHoldingsService holdingsService;
    @Autowired
    private IBizCryptoMessageService messageService;
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
     * 模拟自动执行交易 (简化版)
     */
    private void executeAutoTrade(Long reportId) {
        // 这里只是演示，实际可以解析 AI 的建议内容(比如 "买入 0.1 BTC")
        // 然后调用 holdingsService 更新数据库
        System.out.println(">>> 报告 " + reportId + " 已审核通过，正在自动调整持仓...");

        // 示例：简单地给所有持仓增加 1% 的数量（假装执行了 AI 的增值建议）
        // 实际开发中，你应该让 AI 返回结构化的 JSON 建议（如 {"action":"BUY", "amount":0.1}）并在此处解析
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

    public void generateReport(Long messageId) {
        // 1. 获取新闻
        BizCryptoMessage message = messageService.selectBizCryptoMessageById(messageId);

        // 2. 获取当前所有持仓
        BizAssetHoldings query = new BizAssetHoldings();
        List<BizAssetHoldings> holdings = holdingsService.selectBizAssetHoldingsList(query);
        String holdingsJson = JSONUtil.toJsonStr(holdings);

        // 3. 构建 Prompt (第二次分析：持仓匹配)
        String prompt = String.format(
                "【角色】你是一位资深数字货币投资顾问。\n" +
                        "【背景】我收到一条新闻：%s (涉及币种:%s, 情感:%s)。\n" +
                        "【现状】我的当前持仓如下：%s。\n" +
                        "【任务】请结合新闻和持仓，给出一份详细的自然语言投资建议。请分析新闻对该币种的影响，考虑当前持仓情况，并给出具体的买卖建议。\n" +
                        "【输出格式要求】请直接输出纯自然语言文本，不要返回任何JSON格式或代码格式。建议内容要专业、清晰、有条理，使用与用户提供的例子类似的表述方式。",
                message.getContent(), message.getCoin(), message.getSentiment(), holdingsJson
        );

        // 4. 调用 AI
        String advice = difyUtils.sendRequest(prompt, "system_analyst");

        // 5. 保存报告
        BizInvestmentReport report = new BizInvestmentReport();
        report.setMessageId(messageId);
        report.setAnalysisResult(message.getSentiment()); // 沿用新闻的情感
        report.setAdviceContent(advice);
        report.setStatus("0"); // 0 = 待审核
        report.setCreateTime(DateUtils.getNowDate());

        this.insertBizInvestmentReport(report);
    }
}