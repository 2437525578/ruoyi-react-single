package org.example.controller;

import java.util.List;
import javax.servlet.http.HttpServletResponse;

import org.example.common.core.utils.poi.ExcelUtil;
import org.example.common.core.web.domain.AjaxResult;
import org.example.common.core.web.page.TableDataInfo;
import org.example.system.domain.BizCryptoMetrics;
import org.example.system.service.IBizCryptoMetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.example.common.core.web.controller.BaseController;
import org.example.system.security.annotation.RequiresPermissions;
import org.example.system.log.annotation.Log;
import org.example.system.log.enums.BusinessType;

@RestController
@RequestMapping("/crypto/metrics")
public class BizCryptoMetricsController extends BaseController
{
    @Autowired
    private IBizCryptoMetricsService bizCryptoMetricsService;

    // 临时注释掉权限校验，便于测试
//    @RequiresPermissions("crypto:metrics:list")
    @GetMapping("/list")
    public TableDataInfo list(BizCryptoMetrics bizCryptoMetrics)
    {
        startPage();
        // 如果没有查询条件，默认只返回每个币种最新的行情
        List<BizCryptoMetrics> list;
        if (bizCryptoMetrics.getSymbol() == null && bizCryptoMetrics.getName() == null) {
            list = bizCryptoMetricsService.selectLatestMetrics();
        } else {
            list = bizCryptoMetricsService.selectBizCryptoMetricsList(bizCryptoMetrics);
        }
        return getDataTable(list);
    }

    // 临时注释掉权限校验，便于测试
//    @RequiresPermissions("crypto:metrics:export")
    @Log(title = "虚拟货币行情", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizCryptoMetrics bizCryptoMetrics)
    {
        List<BizCryptoMetrics> list = bizCryptoMetricsService.selectBizCryptoMetricsList(bizCryptoMetrics);
        ExcelUtil<BizCryptoMetrics> util = new ExcelUtil<>(BizCryptoMetrics.class);
        util.exportExcel(response, list, "虚拟货币行情数据");
    }

    // 临时注释掉权限校验，便于测试
//    @RequiresPermissions("crypto:metrics:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizCryptoMetricsService.selectBizCryptoMetricsById(id));
    }

    // 临时注释掉权限校验，便于测试
//    @RequiresPermissions("crypto:metrics:collect")
    @Log(title = "虚拟货币行情", businessType = BusinessType.OTHER)
    @PostMapping("/collect")
    public AjaxResult collect()
    {
        return toAjax(bizCryptoMetricsService.collectCryptoMetrics());
    }
}