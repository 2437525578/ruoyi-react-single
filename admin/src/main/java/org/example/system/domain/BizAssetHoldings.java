package org.example.system.domain;

import java.math.BigDecimal;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.example.common.core.annotation.Excel;
import org.example.common.core.web.domain.BaseEntity;

/**
 * 企业持仓资产对象 biz_asset_holdings
 */
public class BizAssetHoldings extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 币种 */
    @Excel(name = "币种")
    private String coin;

    /** 持有数量 */
    @Excel(name = "持有数量")
    private BigDecimal amount;

    /** 当前估值(USDT) */
    @Excel(name = "当前估值(USDT)")
    private BigDecimal usdtValue;

    /** 平均持仓成本 */
    @Excel(name = "平均持仓成本")
    private BigDecimal costPrice;

    /** 当前市场价格 (不对应数据库字段) */
    private BigDecimal currentPrice;

    /** 24h 涨跌幅 (不对应数据库字段) */
    private BigDecimal change24h;

    public void setId(Long id) { this.id = id; }
    public Long getId() { return id; }

    public void setCoin(String coin) { this.coin = coin; }
    public String getCoin() { return coin; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getAmount() { return amount; }

    public void setUsdtValue(BigDecimal usdtValue) { this.usdtValue = usdtValue; }
    public BigDecimal getUsdtValue() { return usdtValue; }

    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getCostPrice() { return costPrice; }

    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }

    public void setChange24h(BigDecimal change24h) { this.change24h = change24h; }
    public BigDecimal getChange24h() { return change24h; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("coin", getCoin())
                .append("amount", getAmount())
                .append("usdtValue", getUsdtValue())
                .append("costPrice", getCostPrice())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}