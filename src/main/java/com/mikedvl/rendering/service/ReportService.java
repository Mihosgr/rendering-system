package com.mikedvl.rendering.service;

import com.mikedvl.rendering.dto.DailyReportDto;
import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.repository.BatchRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final BatchRepository batchRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public ReportService(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    public List<DailyReportDto> generateReportData(LocalDate startDate, LocalDate endDate) {
        List<Batch> batches = batchRepository.findAll(); // Μπορεί να αντικατασταθεί με query ημερομηνιών
        Map<LocalDate, DailyReportDto> map = new TreeMap<>();

        for (Batch batch : batches) {
            if (batch.getDailyProduction() == null || batch.getDailyProduction().getProductionDate() == null) continue;
            LocalDate pDate = batch.getDailyProduction().getProductionDate();

            if ((startDate != null && pDate.isBefore(startDate)) || (endDate != null && pDate.isAfter(endDate))) {
                continue;
            }

            DailyReportDto dto = map.computeIfAbsent(pDate, d -> new DailyReportDto(d, 0, 0, 0, 0, 0));

            if (batch.getProductType() == Batch.ProductType.OFFAL_MEAL) {
                dto.setSoftTissueWeight(dto.getSoftTissueWeight() + (batch.getRawMaterialWeight() != null ? batch.getRawMaterialWeight() : 0));
                dto.setOffalMealWeight(dto.getOffalMealWeight() + (batch.getFinalMealWeight() != null ? batch.getFinalMealWeight() : 0));
                dto.setOilWeight(dto.getOilWeight() + (batch.getFinalOilWeight() != null ? batch.getFinalOilWeight() : 0));
            } else if (batch.getProductType() == Batch.ProductType.FEATHER_MEAL) {
                dto.setFeathersWeight(dto.getFeathersWeight() + (batch.getRawMaterialWeight() != null ? batch.getRawMaterialWeight() : 0));
                dto.setFeatherMealWeight(dto.getFeatherMealWeight() + (batch.getFinalMealWeight() != null ? batch.getFinalMealWeight() : 0));
            }
        }

        return new ArrayList<>(map.values());
    }

    public byte[] generateExcelReport(List<DailyReportDto> reportData, LocalDate startDate, LocalDate endDate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Αναφορά Παραγωγής");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle codeStyle = workbook.createCellStyle();
            Font codeFont = workbook.createFont();
            codeFont.setItalic(true);
            codeStyle.setFont(codeFont);
            codeStyle.setAlignment(HorizontalAlignment.CENTER);
            codeStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Γραμμή 1: Περίοδος Αναφοράς (Merged)
            Row row0 = sheet.createRow(0);
            Cell cellPeriod = row0.createCell(0);
            String periodStr = "ΠΕΡΙΟΔΟΣ ΑΝΑΦΟΡΑΣ: " +
                    (startDate != null ? startDate.format(DATE_FORMATTER) : "ΑΡΧΗ") + " ΕΩΣ " +
                    (endDate != null ? endDate.format(DATE_FORMATTER) : "ΣΗΜΕΡΑ");
            cellPeriod.setCellValue(periodStr);
            cellPeriod.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            // Γραμμή 2: Κεφαλίδες
            Row row1 = sheet.createRow(1);
            String[] headers = {"Ημερομηνία", "Μαλακοί ιστοί", "Πτηνάλευρο", "Πτηνέλαιο", "Πούπουλα", "Πτεράλευρο"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row1.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Γραμμή 3: Κωδικοί Προϊόντων
            Row row2 = sheet.createRow(2);
            String[] codes = {"-", "07-99-12", "80-00-01", "80-00-02", "07-99-11", "80-00-03"};
            for (int i = 0; i < codes.length; i++) {
                Cell cell = row2.createCell(i);
                cell.setCellValue(codes[i]);
                cell.setCellStyle(codeStyle);
            }

            // Γραμμές Δεδομένων
            int rowIdx = 3;
            double totalSoft = 0, totalOffal = 0, totalOil = 0, totalFeathers = 0, totalFeatherMeal = 0;

            for (DailyReportDto item : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(item.getDate().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(item.getSoftTissueWeight());
                row.createCell(2).setCellValue(item.getOffalMealWeight());
                row.createCell(3).setCellValue(item.getOilWeight());
                row.createCell(4).setCellValue(item.getFeathersWeight());
                row.createCell(5).setCellValue(item.getFeatherMealWeight());

                for (int i = 0; i < 6; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }

                totalSoft += item.getSoftTissueWeight();
                totalOffal += item.getOffalMealWeight();
                totalOil += item.getOilWeight();
                totalFeathers += item.getFeathersWeight();
                totalFeatherMeal += item.getFeatherMealWeight();
            }

            // Γραμμή Συναθροίσεων (Σύνολα)
            Row totalRow = sheet.createRow(rowIdx);
            totalRow.createCell(0).setCellValue("ΣΥΝΟΛΑ");
            totalRow.createCell(1).setCellValue(totalSoft);
            totalRow.createCell(2).setCellValue(totalOffal);
            totalRow.createCell(3).setCellValue(totalOil);
            totalRow.createCell(4).setCellValue(totalFeathers);
            totalRow.createCell(5).setCellValue(totalFeatherMeal);

            for (int i = 0; i < 6; i++) {
                totalRow.getCell(i).setCellStyle(headerStyle);
            }

            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}