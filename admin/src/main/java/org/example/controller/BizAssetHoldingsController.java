package org.example.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.example.system.log.annotation.Log;
import org.example.system.log.enums.BusinessType;
import org.example.system.security.annotation.RequiresPermissions;
import org.example.system.domain.BizAssetHoldings;
import org.example.system.service.IBizAssetHoldingsService;
import org.example.common.core.web.controller.BaseController;
import org.example.common.core.web.domain.AjaxResult;
import org.example.common.core.utils.poi.ExcelUtil;
import org.example.common.core.web.page.TableDataInfo;

@RestController
@RequestMapping("/crypto/holdings")
public class BizAssetHoldingsController extends BaseController
{
    @Autowired
    private IBizAssetHoldingsService bizAssetHoldingsService;

//    @RequiresPermissions("crypto:holdings:list")
    @GetMapping("/list")
    public TableDataInfo list(BizAssetHoldings bizAssetHoldings)
    {
        startPage();
        List<BizAssetHoldings> list = bizAssetHoldingsService.selectBizAssetHoldingsList(bizAssetHoldings);
        return getDataTable(list);
    }

//    @RequiresPermissions("crypto:holdings:export")
    @Log(title = "企业持仓资产", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizAssetHoldings bizAssetHoldings)
    {
        List<BizAssetHoldings> list = bizAssetHoldingsService.selectBizAssetHoldingsList(bizAssetHoldings);
        ExcelUtil<BizAssetHoldings> util = new ExcelUtil<BizAssetHoldings>(BizAssetHoldings.class);
        util.exportExcel(response, list, "企业持仓资产数据");
    }

//    @RequiresPermissions("crypto:holdings:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizAssetHoldingsService.selectBizAssetHoldingsById(id));
    }

//    @RequiresPermissions("crypto:holdings:add")
    @Log(title = "企业持仓资产", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizAssetHoldings bizAssetHoldings)
    {
        return toAjax(bizAssetHoldingsService.insertBizAssetHoldings(bizAssetHoldings));
    }

//    @RequiresPermissions("crypto:holdings:edit")
    @Log(title = "企业持仓资产", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizAssetHoldings bizAssetHoldings)
    {
        return toAjax(bizAssetHoldingsService.updateBizAssetHoldings(bizAssetHoldings));
    }

//    @RequiresPermissions("crypto:holdings:remove")
    @Log(title = "企业持仓资产", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizAssetHoldingsService.deleteBizAssetHoldingsByIds(ids));
    }
}