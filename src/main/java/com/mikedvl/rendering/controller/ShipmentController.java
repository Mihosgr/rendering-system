package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.model.Shipment;
import com.mikedvl.rendering.repository.ShipmentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@Controller
@RequestMapping("/shipments")
public class ShipmentController {

    private final ShipmentRepository shipmentRepository;
    private final com.mikedvl.rendering.repository.BigBagRepository bigBagRepository;
    private final com.mikedvl.rendering.repository.TemperatureLogRepository temperatureLogRepository;

    public ShipmentController(ShipmentRepository shipmentRepository,
                              com.mikedvl.rendering.repository.BigBagRepository bigBagRepository,
                              com.mikedvl.rendering.repository.TemperatureLogRepository temperatureLogRepository) {
        this.shipmentRepository = shipmentRepository;
        this.bigBagRepository = bigBagRepository;
        this.temperatureLogRepository = temperatureLogRepository;
    }

    // 1. Προβολή Λίστας Ανοιχτών Δρομολογίων
    @GetMapping
    public String listShipments(Model model) {
        List<Shipment> activeShipments = shipmentRepository.findByStatusOrderByCreatedAtDesc(Shipment.ShipmentStatus.PREPARATION);
        model.addAttribute("activeShipments", activeShipments);
        return "shipment/shipments";
    }

    // 2. Δημιουργία Νέου Δρομολογίου
    @PostMapping("/create")
    public String createShipment() {
        Shipment shipment = new Shipment();
        shipment.setStatus(Shipment.ShipmentStatus.PREPARATION);

        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long totalCount = shipmentRepository.count() + 1;
        shipment.setShipmentCode("SH-" + datePart + "-" + String.format("%02d", totalCount));

        shipment = shipmentRepository.save(shipment);

        return "redirect:/shipments/" + shipment.getId();
    }

    // 3. Προβολή Συγκεκριμένου Φακέλου
    @GetMapping("/{id}")
    public String viewShipment(@PathVariable Long id, Model model) {
        Shipment shipment = shipmentRepository.findById(id).orElseThrow();
        model.addAttribute("shipment", shipment);
        return "shipment/shipment-view";
    }

    // 4. Αφαίρεση (Αποδέσμευση) Σάκου από Δρομολόγιο
    @PostMapping("/{shipmentId}/remove-bag/{bagId}")
    public String removeBagFromShipment(@PathVariable Long shipmentId, @PathVariable Long bagId) {
        bigBagRepository.findById(bagId).ifPresent(bag -> {
            bag.setShipment(null);
            bag.setTemperature(null);
            bag.setStatus(BigBag.BagStatus.ACTIVE);
            bigBagRepository.save(bag);
        });
        return "redirect:/shipments/" + shipmentId;
    }

    // 5. Χειροκίνητη Ενημέρωση Θερμοκρασίας
    @PostMapping("/{shipmentId}/update-temp/{bagId}")
    public String updateBagTemperatureManually(@PathVariable Long shipmentId,
                                               @PathVariable Long bagId,
                                               @RequestParam Double temperature) {
        bigBagRepository.findById(bagId).ifPresent(bag -> {
            bag.setTemperature(temperature);

            com.mikedvl.rendering.model.TemperatureLog tempLog = new com.mikedvl.rendering.model.TemperatureLog();
            tempLog.setBigBag(bag);
            tempLog.setTemperature(temperature);
            temperatureLogRepository.save(tempLog);

            bigBagRepository.save(bag);
        });
        return "redirect:/shipments/" + shipmentId;
    }

    // 6A. Εξαγωγή Πρόχειρου (Draft) Excel
    @GetMapping("/{id}/export-draft")
    public ResponseEntity<byte[]> exportDraftShipment(@PathVariable Long id) {
        Shipment shipment = shipmentRepository.findById(id).orElseThrow();

        try {
            // Παραγωγή του Excel χωρίς πεδία ημερομηνιών
            byte[] excelContent = generateExcel(shipment, true, null, null, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "DRAFT_" + shipment.getShipmentCode() + ".xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 6B. Οριστικοποίηση & Εξαγωγή Τελικού Excel
    @PostMapping("/{id}/finalize")
    public ResponseEntity<byte[]> finalizeAndExportShipment(
            @PathVariable Long id,
            @RequestParam String loadingDate,
            @RequestParam String entryDate,
            @RequestParam String supervisor) {

        Shipment shipment = shipmentRepository.findById(id).orElseThrow();

        // Αποθήκευση των στοιχείων του Modal στη Βάση Δεδομένων!
        shipment.setLoadingDate(loadingDate);
        shipment.setEntryDate(entryDate);
        shipment.setSupervisor(supervisor);

        shipment.setStatus(Shipment.ShipmentStatus.COMPLETED);
        for (com.mikedvl.rendering.model.BigBag bag : shipment.getBags()) {
            bag.setStatus(com.mikedvl.rendering.model.BigBag.BagStatus.EXPORTED);
        }
        shipmentRepository.save(shipment);

        try {
            byte[] excelContent = generateExcel(shipment, false, loadingDate, entryDate, supervisor);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Shipment_" + shipment.getShipmentCode() + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // 7. Προβολή Ιστορικού (Κλειστά Δρομολόγια)
    @GetMapping("/history")
    public String listHistory(Model model) {
        List<Shipment> closedShipments = shipmentRepository.findByStatusOrderByCreatedAtDesc(Shipment.ShipmentStatus.COMPLETED);
        model.addAttribute("closedShipments", closedShipments);
        return "shipment/history";
    }

    // 8. Επαναληπτική Εξαγωγή Τελικού Excel (Από Ιστορικό)
    @GetMapping("/{id}/export-final")
    public ResponseEntity<byte[]> exportFinalShipment(@PathVariable Long id) {
        Shipment shipment = shipmentRepository.findById(id).orElseThrow();
        try {
            // Χρησιμοποιεί τα αποθηκευμένα πεδία από τη βάση!
            byte[] excelContent = generateExcel(shipment, false, shipment.getLoadingDate(), shipment.getEntryDate(), shipment.getSupervisor());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "Shipment_" + shipment.getShipmentCode() + ".xlsx");
            return ResponseEntity.ok().headers(headers).body(excelContent);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    // --- Ιδιωτική Μέθοδος (Helper) για τη Δημιουργία του Excel ---
    private byte[] generateExcel(Shipment shipment, boolean isDraft, String loadingDate, String entryDate, String supervisor) throws Exception {
        ClassPathResource resource = new ClassPathResource("template.xlsx");
        try (InputStream is = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.getSheetAt(0);

            // Γέμισμα λίστας με Big Bags
            int rowIndex = 2; // Γραμμή 3 στο Excel
            int counter = 1;

            for (com.mikedvl.rendering.model.BigBag bag : shipment.getBags()) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) row = sheet.createRow(rowIndex);

                Cell cellA = row.getCell(0);
                if (cellA == null) cellA = row.createCell(0);
                cellA.setCellValue(counter++);

                Cell cellB = row.getCell(1);
                if (cellB == null) cellB = row.createCell(1);
                String productCode = bag.getBatch().getProductType().name().equals("OFFAL_MEAL") ? "800001" : "800003";
                cellB.setCellValue(productCode);

                Cell cellC = row.getCell(2);
                if (cellC == null) cellC = row.createCell(2);
                cellC.setCellValue(bag.getLotNumber());

                Cell cellD = row.getCell(3);
                if (cellD == null) cellD = row.createCell(3);
                cellD.setCellValue(bag.getWeight());

                Cell cellE = row.getCell(4);
                if (cellE == null) cellE = row.createCell(4);
                if (bag.getTemperature() != null) {
                    cellE.setCellValue(bag.getTemperature());
                } else {
                    cellE.setBlank();
                }

                rowIndex++;
            }

            // Εγγραφή των ειδικών πεδίων ανάλογα με την κατάσταση
            if (isDraft) {
                Row row1 = sheet.getRow(0);
                if (row1 == null) row1 = sheet.createRow(0);
                Cell cellA1 = row1.getCell(0);
                if (cellA1 == null) cellA1 = row1.createCell(0);

                cellA1.setCellValue("Αμβροσιάδης Rendering - Πρόχειρο");
            } else {
                // Κελί A30 (ΜΟΝΟ η Ημερομηνία)
                Row row30 = sheet.getRow(29);
                if (row30 == null) row30 = sheet.createRow(29);
                Cell cellA30 = row30.getCell(0);
                if (cellA30 == null) cellA30 = row30.createCell(0);
                cellA30.setCellValue(loadingDate);

                // Κελί C30 (ΜΟΝΟ ο Υπεύθυνος)
                Cell cellC30 = row30.getCell(2);
                if (cellC30 == null) cellC30 = row30.createCell(2);
                cellC30.setCellValue(supervisor);

                // Κελί A32 (ΜΟΝΟ η Ημερομηνία Καταχώρησης)
                Row row32 = sheet.getRow(31);
                if (row32 == null) row32 = sheet.createRow(31);
                Cell cellA32 = row32.getCell(0);
                if (cellA32 == null) cellA32 = row32.createCell(0);
                cellA32.setCellValue(entryDate);
            }

            // --- Η ΜΑΓΙΚΗ ΕΝΤΟΛΗ ΓΙΑ ΤΟ SUM (Tweak 3) ---
            // Λέει στο Excel: "Υπολόγισε ξανά όλες τις φόρμουλες (π.χ. SUM) με το που ανοίξεις το αρχείο"
            workbook.setForceFormulaRecalculation(true);

            workbook.write(bos);
            return bos.toByteArray();
        }
    }
}