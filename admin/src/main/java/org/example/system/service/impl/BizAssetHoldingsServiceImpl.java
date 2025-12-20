package org.example.system.service.impl;

import java.util.HashMap;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizAssetHoldingsMapper;
import org.example.system.domain.BizAssetHoldings;
import org.example.system.domain.BizCryptoMetrics;
import org.example.system.service.IBizAssetHoldingsService;
import org.example.system.service.IBizCryptoMetricsService;

@Service
public class BizAssetHoldingsServiceImpl implements IBizAssetHoldingsService
{
    @Autowired
    private BizAssetHoldingsMapper bizAssetHoldingsMapper;

    @Autowired
    private IBizCryptoMetricsService metricsService;

    @Override
    public BizAssetHoldings selectBizAssetHoldingsById(Long id)
    {
        BizAssetHoldings holding = bizAssetHoldingsMapper.selectBizAssetHoldingsById(id);
        if (holding != null) {
            updateValuation(holding);
        }
        return holding;
    }

    @Override
    public List<BizAssetHoldings> selectBizAssetHoldingsList(BizAssetHoldings bizAssetHoldings)
    {
        List<BizAssetHoldings> list = bizAssetHoldingsMapper.selectBizAssetHoldingsList(bizAssetHoldings);
        
        // 获取最新行情（每个币种只取最新的一条）
        List<BizCryptoMetrics> metrics = metricsService.selectLatestMetrics();
        
        // 使用 Map 存储行情
        Map<String, BizCryptoMetrics> metricsMap = new HashMap<>();
        Map<String, BizCryptoMetrics> nameMetricsMap = new HashMap<>();
        
        for (BizCryptoMetrics m : metrics) {
            if (m.getSymbol() != null && !m.getSymbol().isEmpty() && !"UNKNOWN".equals(m.getSymbol())) {
                metricsMap.put(m.getSymbol().toUpperCase(), m);
            }
            if (m.getName() != null && !m.getName().isEmpty()) {
                nameMetricsMap.put(m.getName(), m);
            }
        }

        // 动态计算估值并填充市场数据
        for (BizAssetHoldings holding : list) {
            BizCryptoMetrics m = null;
            String coin = holding.getCoin();
            
            if (coin != null) {
                // 1. 尝试符号匹配
                m = metricsMap.get(coin.toUpperCase());
                
                // 2. 尝试名称匹配
                if (m == null) {
                    m = nameMetricsMap.get(coin);
                }
            }
            
            if (m != null) {
                BigDecimal price = m.getPriceUsd();
                if (price != null) {
                    holding.setCurrentPrice(price);
                    if (holding.getAmount() != null) {
                        holding.setUsdtValue(holding.getAmount().multiply(price));
                    }
                }
                holding.setChange24h(m.getChange24h());
            }
        }
        
        return list;
    }

    /**
     * 更新单个持仓的估值
     */
    private void updateValuation(BizAssetHoldings holding) {
        if (holding == null || holding.getCoin() == null) return;
        
        List<BizCryptoMetrics> metrics = metricsService.selectLatestMetrics();
        for (BizCryptoMetrics m : metrics) {
            // 鲁棒性检查：避免 symbol 为空
            boolean symbolMatch = m.getSymbol() != null && m.getSymbol().equalsIgnoreCase(holding.getCoin());
            boolean nameMatch = m.getName() != null && m.getName().equals(holding.getCoin());
            
            if (symbolMatch || nameMatch) {
                if (m.getPriceUsd() != null) {
                    holding.setCurrentPrice(m.getPriceUsd());
                    if (holding.getAmount() != null) {
                        holding.setUsdtValue(holding.getAmount().multiply(m.getPriceUsd()));
                    }
                }
                holding.setChange24h(m.getChange24h());
                break;
            }
        }
    }

    @Override
    public int insertBizAssetHoldings(BizAssetHoldings bizAssetHoldings)
    {
        bizAssetHoldings.setCreateTime(DateUtils.getNowDate());
        return bizAssetHoldingsMapper.insertBizAssetHoldings(bizAssetHoldings);
    }

    @Override
    public int updateBizAssetHoldings(BizAssetHoldings bizAssetHoldings)
    {
        bizAssetHoldings.setUpdateTime(DateUtils.getNowDate());
        return bizAssetHoldingsMapper.updateBizAssetHoldings(bizAssetHoldings);
    }

    @Override
    public int deleteBizAssetHoldingsByIds(Long[] ids)
    {
        return bizAssetHoldingsMapper.deleteBizAssetHoldingsByIds(ids);
    }

    @Override
    public int deleteBizAssetHoldingsById(Long id)
    {
        return bizAssetHoldingsMapper.deleteBizAssetHoldingsById(id);
    }
}