package org.example.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
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
public class BizCryptoMessageController extends BaseController
{
    @Autowired
    private IBizCryptoMessageService bizCryptoMessageService;

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
        return toAjax(bizCryptoMessageService.insertBizCryptoMessage(bizCryptoMessage));
    }

    @RequiresPermissions("crypto:message:edit")
    @Log(title = "数字货币市场消息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizCryptoMessage bizCryptoMessage)
    {
        return toAjax(bizCryptoMessageService.updateBizCryptoMessage(bizCryptoMessage));
    }

    @RequiresPermissions("crypto:message:remove")
    @Log(title = "数字货币市场消息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizCryptoMessageService.deleteBizCryptoMessageByIds(ids));
    }
}