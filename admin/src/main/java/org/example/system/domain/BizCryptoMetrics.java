package org.example.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.example.common.core.annotation.Excel;
import org.example.common.core.annotation.Excel.ColumnType;
import org.example.common.core.web.domain.BaseEntity;

public class BizCryptoMetrics extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long id;

    @Excel(name = "币种符号")
    private String symbol;

    @Excel(name = "币种名称")
    private String name;

    @Excel(name = "当前价格(USD)", cellType = ColumnType.NUMERIC)
    private BigDecimal priceUsd;

    @Excel(name = "市值(亿美元)", cellType = ColumnType.NUMERIC)
    private BigDecimal marketCap;

    @Excel(name = "哈希率变化(%)", cellType = ColumnType.NUMERIC)
    private BigDecimal hashRate;

    @Excel(name = "24h涨跌幅(%)", cellType = ColumnType.NUMERIC)
    private BigDecimal change24h;

    @Excel(name = "流通量/交易量")
    private String transactionCount;

    @Excel(name = "总手续费", cellType = ColumnType.NUMERIC)
    private BigDecimal totalFeesBtc;

    @Excel(name = "区块相关指标", cellType = ColumnType.NUMERIC)
    private BigDecimal blockCount;

    @Excel(name = "历史最高价(USD)", cellType = ColumnType.NUMERIC)
    private BigDecimal athPrice;

    @Excel(name = "数据爬取时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date snapshotTime;

    // getter & setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPriceUsd() {
        return priceUsd;
    }

    public void setPriceUsd(BigDecimal priceUsd) {
        this.priceUsd = priceUsd;
    }

    public BigDecimal getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(BigDecimal marketCap) {
        this.marketCap = marketCap;
    }

    public BigDecimal getHashRate() {
        return hashRate;
    }

    public void setHashRate(BigDecimal hashRate) {
        this.hashRate = hashRate;
    }

    public BigDecimal getChange24h() {
        return change24h;
    }

    public void setChange24h(BigDecimal change24h) {
        this.change24h = change24h;
    }

    public String getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(String transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getTotalFeesBtc() {
        return totalFeesBtc;
    }

    public void setTotalFeesBtc(BigDecimal totalFeesBtc) {
        this.totalFeesBtc = totalFeesBtc;
    }

    public BigDecimal getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(BigDecimal blockCount) {
        this.blockCount = blockCount;
    }

    public BigDecimal getAthPrice() {
        return athPrice;
    }

    public void setAthPrice(BigDecimal athPrice) {
        this.athPrice = athPrice;
    }

    public Date getSnapshotTime() {
        return snapshotTime;
    }

    public void setSnapshotTime(Date snapshotTime) {
        this.snapshotTime = snapshotTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("symbol", getSymbol())
                .append("name", getName())
                .append("priceUsd", getPriceUsd())
                .append("marketCap", getMarketCap())
                .append("hashRate", getHashRate())
                .append("change24h", getChange24h())
                .append("transactionCount", getTransactionCount())
                .append("totalFeesBtc", getTotalFeesBtc())
                .append("blockCount", getBlockCount())
                .append("athPrice", getAthPrice())
                .append("snapshotTime", getSnapshotTime())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("remark", getRemark())
                .toString();
    }
}