package org.example.controller;

import java.util.List;
import java.util.Map;
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
import org.example.system.domain.BizInvestmentReport;
import org.example.system.service.IBizInvestmentReportService;
import org.example.common.core.web.controller.BaseController;
import org.example.common.core.web.domain.AjaxResult;
import org.example.common.core.utils.poi.ExcelUtil;
import org.example.common.core.web.page.TableDataInfo;

@RestController
@RequestMapping("/crypto/report")
public class BizInvestmentReportController extends BaseController
{
    @Autowired
    private IBizInvestmentReportService bizInvestmentReportService;
//    @RequiresPermissions("crypto:report:list")
    @GetMapping("/list")
    public TableDataInfo list(BizInvestmentReport bizInvestmentReport)
    {
        startPage();
        List<BizInvestmentReport> list = bizInvestmentReportService.selectBizInvestmentReportList(bizInvestmentReport);
        return getDataTable(list);
    }

//    @RequiresPermissions("crypto:report:export")
    @Log(title = "AI投资建议报告", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BizInvestmentReport bizInvestmentReport)
    {
        List<BizInvestmentReport> list = bizInvestmentReportService.selectBizInvestmentReportList(bizInvestmentReport);
        ExcelUtil<BizInvestmentReport> util = new ExcelUtil<BizInvestmentReport>(BizInvestmentReport.class);
        util.exportExcel(response, list, "AI投资建议报告数据");
    }

//    @RequiresPermissions("crypto:report:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(bizInvestmentReportService.selectBizInvestmentReportById(id));
    }

//    @RequiresPermissions("crypto:report:add")
    @Log(title = "AI投资建议报告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BizInvestmentReport bizInvestmentReport)
    {
        return toAjax(bizInvestmentReportService.insertBizInvestmentReport(bizInvestmentReport));
    }

//    @RequiresPermissions("crypto:report:edit")
    @Log(title = "AI投资建议报告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BizInvestmentReport bizInvestmentReport)
    {
        return toAjax(bizInvestmentReportService.updateBizInvestmentReport(bizInvestmentReport));
    }

//    @RequiresPermissions("crypto:report:remove")
    @Log(title = "AI投资建议报告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bizInvestmentReportService.deleteBizInvestmentReportByIds(ids));
    }
    
    /**
     * 手动生成投资报告
     */
//    @RequiresPermissions("crypto:report:add")
    @Log(title = "AI投资建议报告", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody Map<String, Long> requestData)
    {
        Long messageId = requestData.get("messageId");
        bizInvestmentReportService.generateReport(messageId);
        return success("投资建议报告生成成功");
    }
}