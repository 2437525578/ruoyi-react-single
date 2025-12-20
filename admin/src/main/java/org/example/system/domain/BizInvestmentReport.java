package org.example.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.example.common.core.annotation.Excel;
import org.example.common.core.web.domain.BaseEntity;

/**
 * AI投资建议报告对象 biz_investment_report
 */
public class BizInvestmentReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 关联的消息ID */
    @Excel(name = "关联的消息ID")
    private Long messageId;

    /** AI分析结果 */
    @Excel(name = "AI分析结果")
    private String analysisResult;

    /** AI建议内容 */
    @Excel(name = "AI建议内容")
    private String adviceContent;

    /** 审核状态 */
    @Excel(name = "审核状态", readConverterExp = "0=待审核,1=已通过,2=已驳回")
    private String status;

    /** 审核人 */
    @Excel(name = "审核人")
    private String auditBy;

    /** 审核时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审核时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;

    /** 驳回原因 */
    @Excel(name = "驳回原因")
    private String rejectReason;

    /** 可执行的交易指令(JSON) */
    private String executeJson;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getMessageId() { return messageId; }

    public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }
    public String getAnalysisResult() { return analysisResult; }

    public void setAdviceContent(String adviceContent) { this.adviceContent = adviceContent; }
    public String getAdviceContent() { return adviceContent; }

    public void setStatus(String status) { this.status = status; }
    public String getStatus() { return status; }

    public void setAuditBy(String auditBy) { this.auditBy = auditBy; }
    public String getAuditBy() { return auditBy; }

    public void setAuditTime(Date auditTime) { this.auditTime = auditTime; }
    public Date getAuditTime() { return auditTime; }

    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public String getRejectReason() { return rejectReason; }

    public void setExecuteJson(String executeJson) { this.executeJson = executeJson; }
    public String getExecuteJson() { return executeJson; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("messageId", getMessageId())
                .append("analysisResult", getAnalysisResult())
                .append("adviceContent", getAdviceContent())
                .append("status", getStatus())
                .append("auditBy", getAuditBy())
                .append("auditTime", getAuditTime())
                .toString();
    }
}