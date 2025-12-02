package org.example.system.mapper;

import java.util.List;
import org.example.system.domain.BizInvestmentReport;

public interface BizInvestmentReportMapper
{
    public BizInvestmentReport selectBizInvestmentReportById(Long id);
    public List<BizInvestmentReport> selectBizInvestmentReportList(BizInvestmentReport bizInvestmentReport);
    public int insertBizInvestmentReport(BizInvestmentReport bizInvestmentReport);
    public int updateBizInvestmentReport(BizInvestmentReport bizInvestmentReport);
    public int deleteBizInvestmentReportById(Long id);
    public int deleteBizInvestmentReportByIds(Long[] ids);
}