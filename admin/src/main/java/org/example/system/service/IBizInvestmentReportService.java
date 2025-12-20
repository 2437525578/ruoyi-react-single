package org.example.system.service;

import java.util.List;
import org.example.system.domain.BizInvestmentReport;

public interface IBizInvestmentReportService
{
    public BizInvestmentReport selectBizInvestmentReportById(Long id);
    public List<BizInvestmentReport> selectBizInvestmentReportList(BizInvestmentReport bizInvestmentReport);
    public int insertBizInvestmentReport(BizInvestmentReport bizInvestmentReport);
    public int updateBizInvestmentReport(BizInvestmentReport bizInvestmentReport);
    public int deleteBizInvestmentReportByIds(Long[] ids);
    public int deleteBizInvestmentReportById(Long id);
    public void generateReport(Long messageId);

    /**
     * 生成汇总投资建议报告（基于最新所有情报和行情）
     */
    public void generateSummaryReport();
}