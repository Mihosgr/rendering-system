package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.dto.DailyReportDto;
import com.mikedvl.rendering.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports")
    public String showReports(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        List<DailyReportDto> reportData = reportService.generateReportData(startDate, endDate);

        double totalSoft = reportData.stream().mapToDouble(DailyReportDto::getSoftTissueWeight).sum();
        double totalOffal = reportData.stream().mapToDouble(DailyReportDto::getOffalMealWeight).sum();
        double totalOil = reportData.stream().mapToDouble(DailyReportDto::getOilWeight).sum();
        double totalFeathers = reportData.stream().mapToDouble(DailyReportDto::getFeathersWeight).sum();
        double totalFeatherMeal = reportData.stream().mapToDouble(DailyReportDto::getFeatherMealWeight).sum();

        model.addAttribute("reportData", reportData);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("totalSoft", totalSoft);
        model.addAttribute("totalOffal", totalOffal);
        model.addAttribute("totalOil", totalOil);
        model.addAttribute("totalFeathers", totalFeathers);
        model.addAttribute("totalFeatherMeal", totalFeatherMeal);

        return "reports/reports";
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) throws IOException {

        List<DailyReportDto> reportData = reportService.generateReportData(startDate, endDate);
        byte[] excelBytes = reportService.generateExcelReport(reportData, startDate, endDate);

        String filename = "Rendering_Report_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(excelBytes);
    }
}