package org.example.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;


import lombok.extern.slf4j.Slf4j;
import org.example.system.domain.DifyAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.system.log.annotation.Log;
import org.example.system.log.enums.BusinessType;
import org.example.system.security.annotation.RequiresPermissions;
import org.example.system.domain.BizCryptoMessage;
import org.example.system.service.IBizCryptoMessageService;
import org.example.common.core.web.controller.BaseController;
import org.example.common.core.web.domain.AjaxResult;
import org.example.common.core.utils.poi.ExcelUtil;
import org.example.common.core.web.page.TableDataInfo;

/**
 * 数字货币市场消息Controller
 * @author louis
 * @date 2025-12-02
 */
@RestController
@RequestMapping("/crypto/message")
@Slf4j
public class BizCryptoMessageController extends BaseController
{
    @Autowired
    private IBizCryptoMessageService bizCryptoMessageService;

    @Autowired
    private DifyAIService difyAIService;

    @RequiresPermissions("crypto:message:list")
    @GetMapping("/list")
    public TableDataInfo list(BizCryptoMessage bizCryptoMessage)
    {
        startPage();
        List<BizCryptoMessage> list = bizCryptoMessageService.selectBizCryptoMessageList(bizCryptoMessage);
        return getDataTable(list);
    }

    @RequiresPermissions("crypto:message:export")
    @Log(title = "数字货币市场消息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizCryptoMessage bizCryptoMessage)
    {
        List<BizCryptoMessage> list = bizCryptoMessageService.selectBizCryptoMessageList(bizCryptoMessage);
        ExcelUtil<BizCryptoMessage> util = new ExcelUtil<BizCryptoMessage>(BizCryptoMessage.class);
        util.exportExcel(response, list, "数字货币市场消息数据");
    }

    @RequiresPermissions("crypto:message:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizCryptoMessageService.selectBizCryptoMessageById(id));
    }

    @RequiresPermissions("crypto:message:add")
    @Log(title = "数字货币市场消息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizCryptoMessage bizCryptoMessage)
    {
        if (bizCryptoMessage.getUseAiAnalysis() == null || bizCryptoMessage.getUseAiAnalysis()) {
            String aiResult = difyAIService.analyzeMessage(bizCryptoMessage.getContent());
            parseAndSetAiResult(bizCryptoMessage, aiResult);
        }
        // 否则保留用户传的值
        return toAjax(bizCryptoMessageService.insertBizCryptoMessage(bizCryptoMessage));
    }

    @RequiresPermissions("crypto:message:edit")
    @Log(title = "数字货币市场消息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizCryptoMessage bizCryptoMessage)
    {
        String aiResult = difyAIService.analyzeMessage(bizCryptoMessage.getContent());
        parseAndSetAiResult(bizCryptoMessage, aiResult);
        return toAjax(bizCryptoMessageService.updateBizCryptoMessage(bizCryptoMessage));
    }

    private void parseAndSetAiResult(BizCryptoMessage msg, String aiResult) {
        if (aiResult == null || aiResult.trim().isEmpty()) {
            msg.setSentiment("未知");
            msg.setImpactScore("0");
            return;
        }

        // 示例：假设 Dify 返回格式为：
        // "情绪：利空；影响分数：85"
        String text = aiResult.trim();

        // 提取 sentiment（支持“情绪：”或“情感倾向：”等）
        int sentStart = text.indexOf("情绪：");
        if (sentStart == -1) sentStart = text.indexOf("情感：");
        if (sentStart == -1) sentStart = text.indexOf("sentiment:");

        if (sentStart != -1) {
            int sentEnd = text.indexOf("；", sentStart);
            if (sentEnd == -1) sentEnd = text.indexOf(";", sentStart);
            if (sentEnd == -1) sentEnd = text.length();

            String sentiment = text.substring(
                    sentStart + (text.charAt(sentStart + 2) == '：' ? 3 : (sentStart + 11 <= text.length() ? 11 : 3)),
                    sentEnd
            ).trim();

            // 标准化输出（可选）
            if (sentiment.contains("负面") || sentiment.contains("下跌") || sentiment.contains("利空")) {
                msg.setSentiment("利空");
            } else if (sentiment.contains("正面") || sentiment.contains("上涨") || sentiment.contains("利好")) {
                msg.setSentiment("利好");
            } else if (sentiment.contains("中性") || sentiment.contains("无明显")) {
                msg.setSentiment("中性");
            } else {
                msg.setSentiment(sentiment);
            }
        } else {
            msg.setSentiment("未知");
        }

        // 提取 impact_score（支持“影响分数：”或“影响程度：”）
        int scoreStart = text.indexOf("影响分数：");
        if (scoreStart == -1) scoreStart = text.indexOf("影响程度：");
        if (scoreStart == -1) scoreStart = text.indexOf("impact_score:");

        if (scoreStart != -1) {
            int scoreEnd = text.indexOf("；", scoreStart);
            if (scoreEnd == -1) scoreEnd = text.indexOf(";", scoreStart);
            if (scoreEnd == -1) scoreEnd = text.length();

            String scoreStr = text.substring(
                    scoreStart + (text.startsWith("影响分数：", scoreStart) ? 5 :
                            text.startsWith("影响程度：", scoreStart) ? 5 : 13),
                    scoreEnd
            ).trim();

            // 只保留数字部分（防止带单位）
            scoreStr = scoreStr.replaceAll("[^\\d]", "");
            msg.setImpactScore(scoreStr.isEmpty() ? "0" : scoreStr);
        } else {
            msg.setImpactScore("0");
        }
    }


    @RequiresPermissions("crypto:message:remove")
    @Log(title = "数字货币市场消息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizCryptoMessageService.deleteBizCryptoMessageByIds(ids));
    }

    @PostMapping("/collect")
    @Log(title = "手动采集新闻", businessType = BusinessType.INSERT)
    public AjaxResult collect() {
        try {
            bizCryptoMessageService.autoCollectNews(); // 复用定时任务逻辑
            return success("新闻采集成功");
        } catch (Exception e) {
            log.error("手动采集失败", e);
            return error("采集失败：" + e.getMessage());
        }
    }


}