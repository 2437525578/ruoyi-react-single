package org.example.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.example.common.core.annotation.Excel;
import org.example.common.core.web.domain.BaseEntity;

/**
 * 数字货币市场消息对象 biz_crypto_message
 */
public class BizCryptoMessage extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 原始消息ID */
    @Excel(name = "原始消息ID")
    private String originId;

    /** 币种 */
    @Excel(name = "币种")
    private String coin;

    /** 消息内容 */
    @Excel(name = "消息内容")
    private String content;

    /** 情感倾向 */
    @Excel(name = "情感倾向")
    private String sentiment;

    /** 影响程度 */
    @Excel(name = "影响程度")
    private String impactScore;

    /** 数据来源 */
    @Excel(name = "数据来源")
    private String source;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发布时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date publishTime;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setOriginId(String originId) { this.originId = originId; }
    public String getOriginId() { return originId; }

    public void setCoin(String coin) { this.coin = coin; }
    public String getCoin() { return coin; }

    public void setContent(String content) { this.content = content; }
    public String getContent() { return content; }

    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public String getSentiment() { return sentiment; }

    public void setImpactScore(String impactScore) { this.impactScore = impactScore; }
    public String getImpactScore() { return impactScore; }

    public void setSource(String source) { this.source = source; }
    public String getSource() { return source; }

    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Date getPublishTime() { return publishTime; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("originId", getOriginId())
                .append("coin", getCoin())
                .append("content", getContent())
                .append("sentiment", getSentiment())
                .append("impactScore", getImpactScore())
                .append("source", getSource())
                .append("publishTime", getPublishTime())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .toString();
    }
}