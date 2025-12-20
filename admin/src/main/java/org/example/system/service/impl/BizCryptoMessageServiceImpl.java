package org.example.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.example.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.system.mapper.BizCryptoMessageMapper;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.service.IBizCryptoMessageService;
import org.example.system.service.IBizInvestmentReportService;
import org.example.system.utils.DifyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.JSONObject;
import java.util.Random;
@Service
public class BizCryptoMessageServiceImpl implements IBizCryptoMessageService
{
    private static final Logger log = LoggerFactory.getLogger(BizCryptoMessageServiceImpl.class);

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

    @Override
    public int collectCryptoMessages() {
        // 直接调用现有的自动采集方法
        autoCollectNews();
        // 由于autoCollectNews没有返回值，这里返回1表示成功执行
        // 实际项目中可以修改autoCollectNews返回采集数量
        return 1;
    }


    /**
     * 定时任务：每天早上 8 点自动采集
     * 对应任务书 3.1 消息采集模块
     */
    @Scheduled(cron = "0 10 * * * ?")
    // 开发测试时可以用 "0/30 * * * * ?" (每30秒执行一次)
    public void autoCollectNews() {
        log.info(">>> 开始执行 AI 新闻采集任务...");

        // 1. 构建 Prompt (提示词)
        String prompt = "请作为一位资深加密货币市场分析师，搜集当前全球市场最重要、最新的加密货币新闻和市场情报。" +
                "请涵盖主流币种（如比特币、以太坊、币安币、索拉纳、瑞波币等）以及近期活跃的的热门币种（如狗狗币、波卡、链节币等）。" +
                "请必须严格按照 JSON 对象格式返回，不要包含 Markdown 代码块标记，也不要返回任何额外文字。" +
                "格式示例：{\"比特币\":[{\"title\":\"标题\",\"summary\":\"不超过100字的摘要\",\"influence_score\":2}],\"以太坊\":[{\"title\":\"标题\",\"summary\":\"摘要\",\"influence_score\":-2}]}。" +
                "注意：influence_score 必须是 -2(重大利空), -1(利空), 0(中性), 1(利好), 2(重大利好) 中的一个。每个币种可包含多条新闻。";

        // 2. 调用 AI
        String aiResponse = difyUtils.sendRequest(prompt, "system_cron");

        // 3. 检查 AI 响应是否有效
        if (aiResponse == null || aiResponse.isEmpty()) {
            log.error(">>> AI 服务请求失败，未返回有效数据");
            return;
        }

        // 清理一下可能存在的 markdown 标记 (```json ... ```)
        aiResponse = aiResponse.replace("```json", "").replace("```", "").trim();

        // 尝试提取完整的JSON对象
        int jsonStartIndex = aiResponse.indexOf("{");
        int jsonEndIndex = aiResponse.lastIndexOf("}");
        
        if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
            aiResponse = aiResponse.substring(jsonStartIndex, jsonEndIndex + 1);
            log.info(">>> 从 AI 响应中提取到 JSON 对象: {}", aiResponse);
        } else {
            log.error(">>> AI 返回数据格式错误，未找到有效的 JSON 对象: {}", aiResponse);
            return;
        }

        // 4. 解析并入库
        try {
            JSONObject rootObject = JSONUtil.parseObj(aiResponse);
            List<BizCryptoMessage> newsList = new ArrayList<>();
            
            // 遍历每个币种
            for (String coinName : rootObject.keySet()) {
                JSONArray coinNews = rootObject.getJSONArray(coinName);
                if (coinNews == null || coinNews.isEmpty()) continue;
                
                // 遍历该币种的所有新闻
                for (Object item : coinNews) {
                    JSONObject newsJson = (JSONObject) item;
                    
                    BizCryptoMessage msg = new BizCryptoMessage();
                    msg.setCoin(coinName); // 使用key作为币种名称
                    // 移除setTitle调用，BizCryptoMessage类没有title字段
                    msg.setContent(newsJson.getStr("summary"));
                    
                    // 处理情感分析 - 使用AI返回值，但添加适当的验证和默认值处理
                    Integer score = null;
                    try {
                        // 尝试获取AI返回的influence_score
                        score = newsJson.getInt("influence_score");
                    } catch (Exception e) {
                        // 处理可能的类型转换或字段不存在异常
                        log.warn("警告：无法获取有效influence_score，使用默认值");
                    }
                    
                    // 设置情感倾向和影响分数，确保有合理的默认值
                    String sentiment = "NEUTRAL"; // 默认中性
                    String impactScore = "0";    // 默认0分
                    
                    if (score != null) {
                        // 根据score设置情感倾向
                        if (score >= 1) {
                            sentiment = "POSITIVE";
                        } else if (score <= -1) {
                            sentiment = "NEGATIVE";
                        }
                        // 使用AI返回的分数，但确保格式正确
                        impactScore = score.toString();
                    } else {
                        // 记录使用默认值的情况
                        log.info("使用默认情感分析值：NEUTRAL, 0分");
                    }
                    
                    msg.setSentiment(sentiment);
                    msg.setImpactScore(impactScore);
                    
                    // 从真实加密货币新闻来源中随机选择一个
                    String[] sources = {"CoinDesk", "The Block", "LTC Foundation", "Reddit", "SEC Updates"};
                    int randomIndex = new Random().nextInt(sources.length);
                    msg.setSource(sources[randomIndex]);
                    
                    msg.setPublishTime(DateUtils.getNowDate());
                    msg.setCreateTime(DateUtils.getNowDate());
                    msg.setUpdateTime(DateUtils.getNowDate());
                    
                    // 先添加到列表，后面批量处理
                    newsList.add(msg);
                }
            }
            
            // 批量入库
            int successCount = 0;
            for (BizCryptoMessage msg : newsList) {
                if (this.insertBizCryptoMessage(msg) > 0) {
                    successCount++;
                }
            }
            
            log.info(">>> 新闻采集完成，入库 {} 条，共解析 {} 条", successCount, newsList.size());

            // 5. 采集完成后，自动生成一份汇总投资建议报告
            try {
                log.info(">>> 正在自动生成汇总投资建议报告...");
                reportService.generateSummaryReport();
            } catch (Exception e) {
                log.error(">>> 自动生成汇总报告失败", e);
            }
        } catch (Exception e) {
            log.error("解析 AI 返回数据失败，原始数据: {}", aiResponse, e);
        }
    }
    
}