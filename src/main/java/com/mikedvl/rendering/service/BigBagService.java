package com.mikedvl.rendering.service;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.repository.BigBagRepository;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Service
public class BigBagService {

    private final BigBagRepository bigBagRepository;

    public BigBagService(BigBagRepository bigBagRepository) {
        this.bigBagRepository = bigBagRepository;
    }

    public void createBigBag(Batch batch, Double weight) {
        BigBag bag = new BigBag();
        bag.setBatch(batch);
        bag.setWeight(weight);
        bag.setStatus(BigBag.BagStatus.ACTIVE);

        // Υπολογισμός του επόμενου διαθέσιμου Α/Α για αυτό το Batch
        Integer nextSerialNumber = bigBagRepository.findMaxBagSerialNumberByBatch(batch) + 1;
        bag.setBagSerialNumber(nextSerialNumber);

        // --- Δημιουργία των μεταβλητών για το 36-char QR Code ---
        String serialFixed = String.format("%02d", nextSerialNumber);
        String cookerNoFixed = String.format("%02d", batch.getCookerId());
        String cookerBatchFixed = (batch.getCookerBatchNumber() != null)
                ? String.format("%02d", batch.getCookerBatchNumber())
                : "01";

        String itemCode = batch.getProductType().name().equals("OFFAL_MEAL") ? "800001" : "800003";

        String dateForQr = batch.getDailyProduction().getProductionDate().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));

        String kilosFixed = String.format("%04d", weight.intValue());

        // Σύνθεση του τελικού QR String ακριβώς όπως στο Zebra
        String qrData = "R01" +
                serialFixed +
                cookerNoFixed +
                cookerBatchFixed +
                itemCode +
                dateForQr +
                timestamp +
                "01" +
                kilosFixed +
                "000";

        // Ανακατασκευή του καθαρού LOT: yyyyMMdd + Cooker(1 ψηφίο απευθείας) + Batch(2 ψηφία)
        String cleanLot = batch.getDailyProduction().getProductionDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + batch.getCookerId()
                + cookerBatchFixed;

        bag.setLotNumber(cleanLot); // Καταχωρεί το καθαρό 12-ψήφιο LOT
        bag.setQrCode(qrData);      // Καταχωρεί το πλήρες QR Code

        bigBagRepository.save(bag);
    }

    // ΝΕΑ ΜΕΘΟΔΟΣ: Ακύρωση (VOID) Big Bag
    public void voidBigBag(Long bagId) {
        bigBagRepository.findById(bagId).ifPresent(bag -> {
            bag.setStatus(BigBag.BagStatus.VOIDED);
            bigBagRepository.save(bag);
        });
    }
}