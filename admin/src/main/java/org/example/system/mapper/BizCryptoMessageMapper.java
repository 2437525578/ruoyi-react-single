package org.example.system.mapper;

import java.util.List;
import org.example.system.domain.BizCryptoMessage;

public interface BizCryptoMessageMapper
{
    public BizCryptoMessage selectBizCryptoMessageById(Long id);
    public List<BizCryptoMessage> selectBizCryptoMessageList(BizCryptoMessage bizCryptoMessage);
    public int insertBizCryptoMessage(BizCryptoMessage bizCryptoMessage);
    public int updateBizCryptoMessage(BizCryptoMessage bizCryptoMessage);
    public int deleteBizCryptoMessageById(Long id);
    public int deleteBizCryptoMessageByIds(Long[] ids);
}