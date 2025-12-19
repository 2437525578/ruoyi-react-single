package org.example.system.mapper;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.jdbc.SQL;
import org.example.system.domain.BizCryptoMetrics;
import java.util.List;
import java.util.Map;

@Mapper
public interface BizCryptoMetricsMapper {

    /**
     * 根据ID查询
     */
    @Select("SELECT id, symbol, name, price_usd AS priceUsd, market_cap AS marketCap, " +
            "hash_rate AS hashRate, `24h_change` AS change24h, transaction_count AS transactionCount, " +
            "total_fees_btc AS totalFeesBtc, block_count AS blockCount, ath_price AS athPrice, " +
            "snapshot_time AS snapshotTime, create_by AS createBy, create_time AS createTime, " +
            "update_by AS updateBy, update_time AS updateTime " +
            "FROM biz_crypto_metrics WHERE id = #{id}")
    BizCryptoMetrics selectBizCryptoMetricsById(Long id);

    /**
     * 查询列表
     */
    @Select("SELECT id, symbol, name, price_usd AS priceUsd, market_cap AS marketCap, " +
            "hash_rate AS hashRate, `24h_change` AS change24h, transaction_count AS transactionCount, " +
            "total_fees_btc AS totalFeesBtc, block_count AS blockCount, ath_price AS athPrice, " +
            "snapshot_time AS snapshotTime, create_by AS createBy, create_time AS createTime, " +
            "update_by AS updateBy, update_time AS updateTime " +
            "FROM biz_crypto_metrics " +
            "WHERE 1=1 " +
            "AND (#{symbol} IS NULL OR #{symbol} = '' OR symbol LIKE CONCAT('%', #{symbol}, '%')) " +
            "AND (#{name} IS NULL OR #{name} = '' OR name LIKE CONCAT('%', #{name}, '%')) " +
            "ORDER BY snapshot_time DESC, market_cap DESC")
    List<BizCryptoMetrics> selectBizCryptoMetricsList(BizCryptoMetrics bizCryptoMetrics);

    /**
     * 插入数据
     */
    @Insert("INSERT INTO biz_crypto_metrics " +
            "(symbol, name, price_usd, market_cap, hash_rate, `24h_change`, transaction_count, " +
            "total_fees_btc, block_count, ath_price, snapshot_time, create_by, update_by) " +
            "VALUES " +
            "(#{symbol}, #{name}, #{priceUsd}, #{marketCap}, #{hashRate}, #{change24h}, #{transactionCount}, " +
            "#{totalFeesBtc}, #{blockCount}, #{athPrice}, #{snapshotTime}, #{createBy}, #{updateBy})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics);

    /**
     * 更新数据
     */
    @Update("UPDATE biz_crypto_metrics SET " +
            "symbol = #{symbol}, name = #{name}, price_usd = #{priceUsd}, market_cap = #{marketCap}, " +
            "hash_rate = #{hashRate}, `24h_change` = #{change24h}, transaction_count = #{transactionCount}, " +
            "total_fees_btc = #{totalFeesBtc}, block_count = #{blockCount}, ath_price = #{athPrice}, " +
            "snapshot_time = #{snapshotTime}, update_by = #{updateBy} " +
            "WHERE id = #{id}")
    int updateBizCryptoMetrics(BizCryptoMetrics bizCryptoMetrics);

    /**
     * 删除单个数据
     */
    @Delete("DELETE FROM biz_crypto_metrics WHERE id = #{id}")
    int deleteBizCryptoMetricsById(Long id);

    /**
     * 批量删除数据
     */
    @DeleteProvider(type = SqlProvider.class, method = "deleteByIds")
    int deleteBizCryptoMetricsByIds(Long[] ids);

    /**
     * 清空表后重新插入（用于每日全量更新）
     */
    @Delete("TRUNCATE TABLE biz_crypto_metrics")
    int deleteAll();

    /**
     * 批量插入（已匹配最新数据库结构，包含 create_time）
     */
    @Insert("<script>" +
            "INSERT INTO biz_crypto_metrics " +
            "(symbol, name, price_usd, market_cap, hash_rate, `24h_change`, transaction_count, " +
            "total_fees_btc, block_count, ath_price, snapshot_time, create_by, create_time, update_by, update_time) " +
            "VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(" +
            "#{item.symbol}, #{item.name}, #{item.priceUsd}, #{item.marketCap}, #{item.hashRate}, " +
            "#{item.change24h}, #{item.transactionCount}, #{item.totalFeesBtc}, #{item.blockCount}, " +
            "#{item.athPrice}, #{item.snapshotTime}, #{item.createBy}, #{item.createTime}, " +
            "#{item.updateBy}, #{item.updateTime}" +
            ")" +
            "</foreach>" +
            "</script>")
    int batchInsertBizCryptoMetrics(@Param("list") List<BizCryptoMetrics> list);

    /**
     * SQL提供者类，用于处理复杂的动态SQL
     */
    class SqlProvider {
        public String deleteByIds(Map<String, Object> map) {
            Long[] ids = (Long[]) map.get("ids");
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM biz_crypto_metrics WHERE id IN (");
            for (int i = 0; i < ids.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("#{ids[").append(i).append("]}");
            }
            sql.append(")");
            return sql.toString();
        }
    }
}