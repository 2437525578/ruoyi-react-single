package org.example.system.service;



import java.util.List;
import org.example.system.domain.BizCryptoMetrics;

public interface IBizCryptoMetricsService
{
    BizCryptoMetrics selectBizCryptoMetricsById(Long id);

    List<BizCryptoMetrics> selectBizCryptoMetricsList(BizCryptoMetrics bizCryptoMetrics);

    /**
     * 查询每个币种最新的行情
     */
    List<BizCryptoMetrics> selectLatestMetrics();

    int insertBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics);

    int updateBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics);

    int deleteBizCryptoMetricsByIds(Long[] ids);

    int deleteBizCryptoMetricsById(Long id);

    /** 手动触发采集行情 */
    int collectCryptoMetrics();
}