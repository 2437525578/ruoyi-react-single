package org.example.controller;

import java.util.List;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.service.IBizCryptoMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 虚拟货币行情消息Controller
 */
@RestController
@RequestMapping("/crypto/message")
public class BizCryptoMessageController {

    @Autowired
    private IBizCryptoMessageService bizCryptoMessageService;

    /**
     * 获取虚拟货币行情消息列表
     */
    @GetMapping("/list")
    public List<BizCryptoMessage> list(BizCryptoMessage bizCryptoMessage) {
        return bizCryptoMessageService.selectBizCryptoMessageList(bizCryptoMessage);
    }

    /**
     * 获取虚拟货币行情消息详情
     */
    @GetMapping("/info/{id}")
    public BizCryptoMessage getInfo(@PathVariable("id") Long id) {
        return bizCryptoMessageService.selectBizCryptoMessageById(id);
    }

    /**
     * 新增虚拟货币行情消息
     */
    @PostMapping("/add")
    public int add(@RequestBody BizCryptoMessage bizCryptoMessage) {
        return bizCryptoMessageService.insertBizCryptoMessage(bizCryptoMessage);
    }

    /**
     * 修改虚拟货币行情消息
     */
    @PostMapping("/edit")
    public int edit(@RequestBody BizCryptoMessage bizCryptoMessage) {
        return bizCryptoMessageService.updateBizCryptoMessage(bizCryptoMessage);
    }

    /**
     * 删除虚拟货币行情消息
     */
    @DeleteMapping("/delete/{ids}")
    public int delete(@PathVariable("ids") Long[] ids) {
        return bizCryptoMessageService.deleteBizCryptoMessageByIds(ids);
    }
    
    /**
     * 手动触发AI收集新闻
     */
    @PostMapping("/collect")
    public int collectCryptoMessages() {
        return bizCryptoMessageService.collectCryptoMessages();
    }
}
