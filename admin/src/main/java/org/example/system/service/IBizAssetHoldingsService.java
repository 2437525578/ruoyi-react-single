package org.example.system.service;

import java.util.List;
import org.example.system.domain.BizAssetHoldings;

public interface IBizAssetHoldingsService
{
    public BizAssetHoldings selectBizAssetHoldingsById(Long id);
    public List<BizAssetHoldings> selectBizAssetHoldingsList(BizAssetHoldings bizAssetHoldings);
    public int insertBizAssetHoldings(BizAssetHoldings bizAssetHoldings);
    public int updateBizAssetHoldings(BizAssetHoldings bizAssetHoldings);
    public int deleteBizAssetHoldingsByIds(Long[] ids);
    public int deleteBizAssetHoldingsById(Long id);
}