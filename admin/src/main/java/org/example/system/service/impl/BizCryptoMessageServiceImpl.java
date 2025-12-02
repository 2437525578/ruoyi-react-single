package org.example.system.service.impl;

import java.util.List;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizCryptoMessageMapper;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.service.IBizCryptoMessageService;
import org.example.system.service.IBizInvestmentReportService;
import org.example.system.utils.DifyUtils;
import org.springframework.scheduling.annotation.Scheduled;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
@Service
public class BizCryptoMessageServiceImpl implements IBizCryptoMessageService
{
    @Autowired
    private BizCryptoMessageMapper bizCryptoMessageMapper;
    @Autowired
    private IBizInvestmentReportService reportService;
    @Autowired
    private DifyUtils difyUtils;
    @Override
    public BizCryptoMessage selectBizCryptoMessageById(Long id)
    {
        return bizCryptoMessageMapper.selectBizCryptoMessageById(id);
    }

    @Override
    public List<BizCryptoMessage> selectBizCryptoMessageList(BizCryptoMessage bizCryptoMessage)
    {
        return bizCryptoMessageMapper.selectBizCryptoMessageList(bizCryptoMessage);
    }

    @Override
    public int insertBizCryptoMessage(BizCryptoMessage bizCryptoMessage)
    {
        bizCryptoMessage.setCreateTime(DateUtils.getNowDate());
        return bizCryptoMessageMapper.insertBizCryptoMessage(bizCryptoMessage);
    }

    @Override
    public int updateBizCryptoMessage(BizCryptoMessage bizCryptoMessage)
    {
        bizCryptoMessage.setUpdateTime(DateUtils.getNowDate());
        return bizCryptoMessageMapper.updateBizCryptoMessage(bizCryptoMessage);
    }

    @Override
    public int deleteBizCryptoMessageByIds(Long[] ids)
    {
        return bizCryptoMessageMapper.deleteBizCryptoMessageByIds(ids);
    }

    @Override
    public int deleteBizCryptoMessageById(Long id)
    {
        return bizCryptoMessageMapper.deleteBizCryptoMessageById(id);
    }


    /**
     * 定时任务：每天早上 8 点自动采集
     * 对应任务书 3.1 消息采集模块
     */
    @Scheduled(cron = "0 0 8 * * ?")
    // 开发测试时可以用 "0/30 * * * * ?" (每30秒执行一次)
    public void autoCollectNews() {
        System.out.println(">>> 开始执行 AI 新闻采集任务...");

        // 1. 构建 Prompt (提示词)
        String prompt = "请作为一位加密货币分析师，搜集当前最新的市场消息。" +
                "请必须严格按照 JSON 数组格式返回，不要包含 Markdown 代码块标记。" +
                "格式示例：[{'coin':'BTC', 'content':'消息内容', 'sentiment':'POSITIVE', 'source':'Binance'}]。" +
                "sentiment 只能是 POSITIVE(利好), NEGATIVE(利空), NEUTRAL(中性)。";

        // 2. 调用 AI
        String aiResponse = difyUtils.sendRequest(prompt, "system_cron");

        // 清理一下可能存在的 markdown 标记 (```json ... ```)
        aiResponse = aiResponse.replace("```json", "").replace("```", "").trim();

        // 3. 解析并入库
        try {
            JSONArray newsList = JSONUtil.parseArray(aiResponse);
            for (Object item : newsList) {
                JSONObject newsJson = (JSONObject) item;

                BizCryptoMessage msg = new BizCryptoMessage();
                msg.setCoin(newsJson.getStr("coin"));
                msg.setContent(newsJson.getStr("content"));
                msg.setSentiment(newsJson.getStr("sentiment"));
                msg.setSource(newsJson.getStr("source"));
                msg.setPublishTime(DateUtils.getNowDate());
                msg.setCreateTime(DateUtils.getNowDate());

                this.insertBizCryptoMessage(msg);
                if (msg.getId() != null) {
                    System.out.println(">>> 正在为新闻 ID: " + msg.getId() + " 生成投资建议...");
                    reportService.generateReport(msg.getId());
                }
            }
            System.out.println(">>> 新闻采集完成，入库 " + newsList.size() + " 条");
        } catch (Exception e) {
            System.err.println("解析 AI 返回数据失败，原始数据: " + aiResponse);
            e.printStackTrace();
        }
    }

}
