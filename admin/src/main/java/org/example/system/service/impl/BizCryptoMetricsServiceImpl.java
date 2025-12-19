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

    @Scheduled(cron = "0 30 8 * * ?")
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

            for (Object obj : dataArray) {
                JSONObject json = (JSONObject) obj;

                BizCryptoMetrics metrics = new BizCryptoMetrics();

                String name = json.getStr("name");
                if (name == null || name.isEmpty()) continue;
                metrics.setName(name);

                // 当前价格
                BigDecimal priceUsd = json.getBigDecimal("priceUsd");
                if (priceUsd == null) priceUsd = json.getBigDecimal("price_usd");
                if (priceUsd == null) continue;
                metrics.setPriceUsd(priceUsd);

                // 市值
                BigDecimal marketCap = json.getBigDecimal("marketCap");
                if (marketCap == null) marketCap = json.getBigDecimal("market_cap");
                metrics.setMarketCap(marketCap);

                // 24h涨跌幅
                BigDecimal change24h = json.getBigDecimal("change24h");
                if (change24h == null) change24h = json.getBigDecimal("24h_change");
                metrics.setChange24h(change24h);

                // 流通量
                String transactionCount = json.getStr("circulatingSupply");
                if (transactionCount == null || transactionCount.isEmpty()) {
                    transactionCount = json.getStr("transaction_count");
                }
                metrics.setTransactionCount(transactionCount);

                // 历史最高价
                BigDecimal athPrice = json.getBigDecimal("athPrice");
                if (athPrice == null) athPrice = json.getBigDecimal("ath_price");
                metrics.setAthPrice(athPrice);

                // 哈希率变化 - 兼容两种格式
                BigDecimal hashRate = json.getBigDecimal("hashRate");
                if (hashRate == null) hashRate = json.getBigDecimal("hash_rate");
                metrics.setHashRate(hashRate);

                // 总手续费 - 兼容两种格式
                BigDecimal totalFeesBtc = json.getBigDecimal("totalFeesBtc");
                if (totalFeesBtc == null) totalFeesBtc = json.getBigDecimal("total_fees_btc");
                metrics.setTotalFeesBtc(totalFeesBtc);

                // 区块相关指标 - 兼容两种格式
                BigDecimal blockCount = json.getBigDecimal("blockCount");
                if (blockCount == null) blockCount = json.getBigDecimal("block_count");
                metrics.setBlockCount(blockCount);

                metrics.setSymbol(getSymbolByCoinName(name));
                metrics.setSnapshotTime(new Date());
                metrics.setCreateTime(new Date());
                metrics.setUpdateTime(new Date());
                metrics.setCreateBy("dify_crawler");
                metrics.setUpdateBy("dify_crawler");

                metricsList.add(metrics);
                System.out.println(">>>> 添加成功: " + name +
                        " hashRate=" + hashRate +
                        " totalFees=" + totalFeesBtc +
                        " blockCount=" + blockCount);
            }

            if (metricsList.isEmpty()) {
                System.err.println(">>> 无有效数据入库");
                return;
            }

            System.out.println(">>> 开始入库 " + metricsList.size() + " 条");
            bizCryptoMetricsMapper.deleteAll();
            int rows = bizCryptoMetricsMapper.batchInsertBizCryptoMetrics(metricsList);
            System.out.println(">>> 成功入库 " + rows + " 条记录");

        } catch (Exception e) {
            System.err.println(">>> 解析失败: " + aiResponse);
            e.printStackTrace();
        }
    }
}