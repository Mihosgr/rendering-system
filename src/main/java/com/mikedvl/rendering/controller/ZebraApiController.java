package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.BigBagRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/zebra")
public class ZebraApiController {

    private final DailyProductionRepository dailyRepo;
    private final BatchRepository batchRepo;
    private final BigBagRepository bigBagRepo;

    public ZebraApiController(DailyProductionRepository dailyRepo, BatchRepository batchRepo, BigBagRepository bigBagRepo) {
        this.dailyRepo = dailyRepo;
        this.batchRepo = batchRepo;
        this.bigBagRepo = bigBagRepo;
    }

    @PostMapping("/register-bag")
    public ResponseEntity<String> registerBag(@RequestBody Map<String, String> payload) {
        String qrData = payload.get("qrData");

        if (qrData == null || qrData.length() != 36 || !qrData.startsWith("R01")) {
            return ResponseEntity.badRequest().body("Μη έγκυρα δεδομένα QR.");
        }

        try {
            // ΝΕΑ ΑΠΟΔΟΜΗΣΗ (Βάσει της διορθωμένης δομής)
            int bagSerialNumber = Integer.parseInt(qrData.substring(3, 5)); // Θέσεις 3-4: Α/Α Big Bag
            int cookerNo = Integer.parseInt(qrData.substring(5, 7));        // Θέσεις 5-6: Batch Cooker No
            int cookerBatch = Integer.parseInt(qrData.substring(7, 9));     // Θέσεις 7-8: Cooker Batch
            String itemCode = qrData.substring(9, 15);                      // Θέσεις 9-14: Κωδικός Είδους
            String dateStr = qrData.substring(15, 21);                      // Θέσεις 15-20: Ημερομηνία
            double kilos = Double.parseDouble(qrData.substring(29, 33));    // Θέσεις 29-32: Κιλά

            LocalDate productionDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyMMdd"));

            // 1. Εύρεση Ημερήσιας Παραγωγής
            Optional<DailyProduction> dailyOpt = dailyRepo.findByProductionDate(productionDate);
            if (dailyOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("ΣΦΑΛΜΑ: Η Ημερήσια Παραγωγή δεν έχει ανοίξει.");
            }

            // 2. Εύρεση Batch
            Optional<Batch> batchOpt = batchRepo.findByDailyProductionAndCookerIdAndCookerBatchNumber(dailyOpt.get(), cookerNo, cookerBatch);
            if (batchOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("ΣΦΑΛΜΑ: Το Batch (Cooker " + cookerNo + ", Παρτίδα " + cookerBatch + ") δεν βρέθηκε.");
            }

            Batch currentBatch = batchOpt.get();

            // 3. ΕΛΕΓΧΟΣ ΛΑΘΟΣ ΠΡΟΪΟΝΤΟΣ (Πτηνάλευρο vs Πτεράλευρο)
            String qrProductType = itemCode.equals("800001") ? "OFFAL_MEAL" : "FEATHER_MEAL";
            if (!currentBatch.getProductType().name().equals(qrProductType)) {
                String expected = currentBatch.getProductType().name().equals("OFFAL_MEAL") ? "Πτηνάλευρο" : "Πτεράλευρο";
                String selected = qrProductType.equals("OFFAL_MEAL") ? "Πτηνάλευρο" : "Πτεράλευρο";
                return ResponseEntity.badRequest().body("ΛΑΘΟΣ ΠΡΟΪΟΝ: Στο Zebra επιλέξατε " + selected + ", αλλά το Cooker " + cookerNo + " έχει καταχωρηθεί για " + expected + "!");
            }

            // 3.5 ΕΛΕΓΧΟΣ ΑΛΛΗΛΟΥΧΙΑΣ ΣΑΚΩΝ (Αποτροπή πήδησης αριθμού)
            Integer maxBagSoFar = bigBagRepo.findMaxBagSerialNumberByBatch(currentBatch);
            if (bagSerialNumber > maxBagSoFar + 1) {
                return ResponseEntity.badRequest().body("ΣΕΙΡΙΑΚΟ ΛΑΘΟΣ: Αναμένεται ο σάκος #" + (maxBagSoFar + 1) + ", αλλά σαρώθηκε ο #" + bagSerialNumber + ". Ελέγξτε το τερματικό!");
            }

            // 4. ΕΛΕΓΧΟΣ ΔΕΙΓΜΑΤΟΣ (Έλεγχος του bagSerialNumber)
            Optional<BigBag> existingBag = bigBagRepo.findByBatchAndBagSerialNumber(currentBatch, bagSerialNumber);
            if (existingBag.isPresent()) {
                // Η εφαρμογή επιστρέφει OK στο Zebra για να μην χτυπήσει σφάλμα στο UI, αλλά ενημερώνει ότι είναι δείγμα.
                return ResponseEntity.ok("ΕΚΤΥΠΩΣΗ ΔΕΙΓΜΑΤΟΣ: Η ετικέτα εκτυπώθηκε, δεν διπλοκαταχωρήθηκε.");
            }

            // 5. ΑΠΟΘΗΚΕΥΣΗ ΚΑΝΟΝΙΚΟΥ ΣΑΚΟΥ
            BigBag newBag = new BigBag();

            // 11-ψήφιο LOT: yyyyMMdd + Cooker(1 ψηφίο) + Batch(2 ψηφία)
            String cleanLot = productionDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + cookerNo
                    + String.format("%02d", cookerBatch);

            newBag.setLotNumber(cleanLot); // Καταχωρεί το καθαρό 12-ψήφιο LOT
            newBag.setQrCode(qrData);      // Καταχωρεί το πλήρες QR Code

            newBag.setWeight(kilos);
            newBag.setBatch(currentBatch);
            newBag.setBagSerialNumber(bagSerialNumber);
            newBag.setStatus(BigBag.BagStatus.ACTIVE);

            bigBagRepo.save(newBag);

            return ResponseEntity.ok("Επιτυχής προσθήκη νέου σάκου (" + kilos + "kg) στην αποθήκη!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Σφάλμα API κατά την αποδόμηση.");
        }
    }
}