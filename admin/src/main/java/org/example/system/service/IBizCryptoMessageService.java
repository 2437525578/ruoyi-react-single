package org.example.system.service;

import java.util.List;
import org.example.system.domain.BizCryptoMessage;

public interface IBizCryptoMessageService
{
    public BizCryptoMessage selectBizCryptoMessageById(Long id);
    public List<BizCryptoMessage> selectBizCryptoMessageList(BizCryptoMessage bizCryptoMessage);
    public int insertBizCryptoMessage(BizCryptoMessage bizCryptoMessage);
    public int updateBizCryptoMessage(BizCryptoMessage bizCryptoMessage);
    public int deleteBizCryptoMessageByIds(Long[] ids);
    public int deleteBizCryptoMessageById(Long id);
    void autoCollectNews();
}