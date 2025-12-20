package org.example.system.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.example.common.core.utils.DateUtils;
import org.example.system.domain.BizCryptoMetrics;
import org.example.system.mapper.BizCryptoMetricsMapper;
import org.example.system.service.IBizCryptoMetricsService;
import org.example.system.utils.DifyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BizCryptoMetricsServiceImpl implements IBizCryptoMetricsService {

    @Autowired
    private BizCryptoMetricsMapper bizCryptoMetricsMapper;

    @Autowired
    private DifyUtils difyUtils;

    private String getSymbolByCoinName(String coinName) {
        if (coinName == null || coinName.isEmpty()) return "UNKNOWN";
        switch (coinName) {
            case "比特币": return "BTC";
            case "以太坊": return "ETH";
            case "泰达币": return "USDT";
            case "币安币": return "BNB";
            case "瑞波币": return "XRP";
            case "USDC": return "USDC";
            case "索拉纳": return "SOL";
            case "狗狗币": return "DOGE";
            case "艾达币": return "ADA";
            case "波场": return "TRX";
            case "吨币": return "TON";
            case "波卡": return "DOT";
            case "链节币": return "LINK";
            case "柴犬币": return "SHIB";
            case "莱特币": return "LTC";
            default: return "UNKNOWN";
        }
    }

    @Override
    public BizCryptoMetrics selectBizCryptoMetricsById(Long id) {
        return bizCryptoMetricsMapper.selectBizCryptoMetricsById(id);
    }

    @Override
    public List<BizCryptoMetrics> selectBizCryptoMetricsList(BizCryptoMetrics bizCryptoMetrics) {
        return bizCryptoMetricsMapper.selectBizCryptoMetricsList(bizCryptoMetrics);
    }

    @Override
    public List<BizCryptoMetrics> selectLatestMetrics() {
        return bizCryptoMetricsMapper.selectLatestMetrics();
    }

    @Override
    public int insertBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics) {
        bizCryptoMetrics.setCreateTime(DateUtils.getNowDate());
        return bizCryptoMetricsMapper.insertBizCryptoMetrics(bizCryptoMetrics);
    }

    @Override
    public int updateBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics) {
        bizCryptoMetrics.setUpdateTime(DateUtils.getNowDate());
        return bizCryptoMetricsMapper.updateBizCryptoMetrics(bizCryptoMetrics);
    }

    @Override
    public int deleteBizCryptoMetricsByIds(Long[] ids) {
        return bizCryptoMetricsMapper.deleteBizCryptoMetricsByIds(ids);
    }

    @Override
    public int deleteBizCryptoMetricsById(Long id) {
        return bizCryptoMetricsMapper.deleteBizCryptoMetricsById(id);
    }

    @Override
    public int collectCryptoMetrics() {
        autoCollectMetrics();
        return 1;
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void autoCollectMetrics() {
        System.out.println(">>> 开始执行 AI 行情采集任务...");
        String aiResponse = difyUtils.collectMultiCryptoMetrics();
        if (aiResponse == null || aiResponse.isEmpty()) {
            System.err.println(">>> AI 服务请求失败，未返回有效数据");
            return;
        }

        try {
            JSONObject root = JSONUtil.parseObj(aiResponse);
            JSONArray dataArray = root.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                System.err.println(">>> AI 返回数据中未找到 data 数组");
                return;
            }

            List<BizCryptoMetrics> metricsList = new ArrayList<>();
            java.util.Set<String> processedSymbols = new java.util.HashSet<>();
            Date now = new Date();

            for (Object obj : dataArray) {
                JSONObject json = (JSONObject) obj;

                BizCryptoMetrics metrics = new BizCryptoMetrics();

                String name = json.getStr("name");
                if (name == null || name.isEmpty()) continue;
                
                String symbol = getSymbolByCoinName(name);
                // 如果是未知币种，或者是重复的币种，则跳过，避免数据库唯一索引冲突
                if ("UNKNOWN".equals(symbol) || processedSymbols.contains(symbol)) {
                    continue;
                }

                // 当前价格
                BigDecimal priceUsd = json.getBigDecimal("priceUsd");
                if (priceUsd == null) priceUsd = json.getBigDecimal("price_usd");
                
                // 如果价格为0或空，且不是稳定币，可能数据无效，跳过
                if ((priceUsd == null || priceUsd.compareTo(BigDecimal.ZERO) <= 0) 
                    && !name.contains("泰达") && !name.contains("USDC")) {
                    continue;
                }
                
                metrics.setName(name);
                metrics.setPriceUsd(priceUsd != null ? priceUsd : BigDecimal.ZERO);

                // 市值
                BigDecimal marketCap = json.getBigDecimal("marketCap");
                if (marketCap == null) marketCap = json.getBigDecimal("market_cap");
                metrics.setMarketCap(marketCap != null ? marketCap : BigDecimal.ZERO);

                // 24h涨跌幅
                BigDecimal change24h = json.getBigDecimal("change24h");
                if (change24h == null) change24h = json.getBigDecimal("24h_change");
                metrics.setChange24h(change24h != null ? change24h : BigDecimal.ZERO);

                // 流通量
                String transactionCount = json.getStr("circulatingSupply");
                if (transactionCount == null || transactionCount.isEmpty()) {
                    transactionCount = json.getStr("transaction_count");
                }
                metrics.setTransactionCount(transactionCount);

                // 历史最高价
                BigDecimal athPrice = json.getBigDecimal("athPrice");
                if (athPrice == null) athPrice = json.getBigDecimal("ath_price");
                metrics.setAthPrice(athPrice != null ? athPrice : BigDecimal.ZERO);

                // 哈希率变化 - 兼容两种格式
                BigDecimal hashRate = json.getBigDecimal("hashRate");
                if (hashRate == null) hashRate = json.getBigDecimal("hash_rate");
                metrics.setHashRate(hashRate != null ? hashRate : BigDecimal.ZERO);

                // 总手续费 - 兼容两种格式
                BigDecimal totalFeesBtc = json.getBigDecimal("totalFeesBtc");
                if (totalFeesBtc == null) totalFeesBtc = json.getBigDecimal("total_fees_btc");
                metrics.setTotalFeesBtc(totalFeesBtc != null ? totalFeesBtc : BigDecimal.ZERO);

                // 区块相关指标 - 兼容两种格式
                BigDecimal blockCount = json.getBigDecimal("blockCount");
                if (blockCount == null) blockCount = json.getBigDecimal("block_count");
                metrics.setBlockCount(blockCount != null ? blockCount : BigDecimal.ZERO);

                metrics.setSymbol(symbol);
                metrics.setSnapshotTime(now);
                metrics.setCreateTime(now);
                metrics.setUpdateTime(now);
                metrics.setCreateBy("dify_crawler");
                metrics.setUpdateBy("dify_crawler");

                metricsList.add(metrics);
                processedSymbols.add(symbol);
            }

            if (metricsList.isEmpty()) {
                return;
            }

            bizCryptoMetricsMapper.deleteAll();
            bizCryptoMetricsMapper.batchInsertBizCryptoMetrics(metricsList);
        } catch (Exception e) {
            System.err.println(">>> 解析失败: " + aiResponse);
            e.printStackTrace();
        }
    }
}