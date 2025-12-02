package org.example.system.mapper;

import java.util.List;
import org.example.system.domain.BizAssetHoldings;

public interface BizAssetHoldingsMapper
{
    public BizAssetHoldings selectBizAssetHoldingsById(Long id);
    public List<BizAssetHoldings> selectBizAssetHoldingsList(BizAssetHoldings bizAssetHoldings);
    public int insertBizAssetHoldings(BizAssetHoldings bizAssetHoldings);
    public int updateBizAssetHoldings(BizAssetHoldings bizAssetHoldings);
    public int deleteBizAssetHoldingsById(Long id);
    public int deleteBizAssetHoldingsByIds(Long[] ids);
}