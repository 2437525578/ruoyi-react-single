package org.example.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.example.system.domain.BizCryptoMessage;

@org.apache.ibatis.annotations.Mapper
public interface BizCryptoMessageMapper
{
    public BizCryptoMessage selectBizCryptoMessageById(Long id);

    public List<BizCryptoMessage> selectBizCryptoMessageList(BizCryptoMessage bizCryptoMessage);

    public int insertBizCryptoMessage(BizCryptoMessage bizCryptoMessage);

    public int updateBizCryptoMessage(BizCryptoMessage bizCryptoMessage);

    public int deleteBizCryptoMessageById(Long id);

    public int deleteBizCryptoMessageByIds(Long[] ids);

    /**
     * 清空所有新闻（每日全量更新用）
     */
    @Delete("DELETE FROM biz_crypto_message")
    int deleteAll();
}