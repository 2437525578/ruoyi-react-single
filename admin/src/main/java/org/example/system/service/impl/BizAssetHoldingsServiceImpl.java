package org.example.system.service.impl;

import java.util.List;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizAssetHoldingsMapper;
import org.example.system.domain.BizAssetHoldings;
import org.example.system.service.IBizAssetHoldingsService;

@Service
public class BizAssetHoldingsServiceImpl implements IBizAssetHoldingsService
{
    @Autowired
    private BizAssetHoldingsMapper bizAssetHoldingsMapper;

    @Override
    public BizAssetHoldings selectBizAssetHoldingsById(Long id)
    {
        return bizAssetHoldingsMapper.selectBizAssetHoldingsById(id);
    }

    @Override
    public List<BizAssetHoldings> selectBizAssetHoldingsList(BizAssetHoldings bizAssetHoldings)
    {
        return bizAssetHoldingsMapper.selectBizAssetHoldingsList(bizAssetHoldings);
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