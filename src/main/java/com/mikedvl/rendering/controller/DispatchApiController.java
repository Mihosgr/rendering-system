package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.model.Shipment;
import com.mikedvl.rendering.model.TemperatureLog;
import com.mikedvl.rendering.repository.BigBagRepository;
import com.mikedvl.rendering.repository.ShipmentRepository;
import com.mikedvl.rendering.repository.TemperatureLogRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/dispatch")
public class DispatchApiController {

    private final BigBagRepository bigBagRepository;
    private final ShipmentRepository shipmentRepository;
    private final TemperatureLogRepository temperatureLogRepository;

    public DispatchApiController(BigBagRepository bigBagRepository,
                                 ShipmentRepository shipmentRepository,
                                 TemperatureLogRepository temperatureLogRepository) {
        this.bigBagRepository = bigBagRepository;
        this.shipmentRepository = shipmentRepository;
        this.temperatureLogRepository = temperatureLogRepository;
    }

    // --- ΝΕΟ: Επιστρέφει Λίστα με τα Ανοιχτά Δρομολόγια για το Android Spinner ---
    @GetMapping("/active-shipments")
    public ResponseEntity<java.util.List<java.util.Map<String, String>>> getActiveShipments() {
        java.util.List<Shipment> activeShipments = shipmentRepository.findByStatusOrderByCreatedAtDesc(Shipment.ShipmentStatus.PREPARATION);

        java.util.List<java.util.Map<String, String>> responseList = new java.util.ArrayList<>();

        for (Shipment s : activeShipments) {
            java.util.Map<String, String> map = new java.util.HashMap<>();
            map.put("shipmentCode", s.getShipmentCode());
            // Μορφοποίηση ημερομηνίας π.χ. "09/09/2026"
            String dateStr = s.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            map.put("displayLabel", s.getShipmentCode() + " (" + dateStr + ")");
            responseList.add(map);
        }

        return ResponseEntity.ok(responseList);
    }

    // --- ΝΕΟ: Επιστρέφει τα ήδη σαρωμένα σακιά ενός Δρομολογίου ---
    @GetMapping("/shipment-bags")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getShipmentBags(@RequestParam String shipmentCode) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentCode(shipmentCode);
        if (shipmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Shipment shipment = shipmentOpt.get();
        java.util.List<java.util.Map<String, Object>> bagsList = new java.util.ArrayList<>();

        for (com.mikedvl.rendering.model.BigBag bag : shipment.getBags()) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("qrCode", bag.getQrCode());
            map.put("lotNumber", bag.getLotNumber());
            map.put("weight", bag.getWeight());
            map.put("temperature", bag.getTemperature());

            // Υπολογισμός Κωδικού Προϊόντος για το Android
            String productCode = bag.getBatch().getProductType().name().equals("OFFAL_MEAL") ? "800001" : "800003";
            map.put("productCode", Integer.parseInt(productCode));

            bagsList.add(map);
        }

        return ResponseEntity.ok(bagsList);
    }

    @PostMapping("/scan")
    public ResponseEntity<String> scanBagForShipment(
            @RequestParam String shipmentCode,
            @RequestParam String qrCode,
            @RequestParam(required = false) Double temperature) {

        Optional<Shipment> shipmentOpt = shipmentRepository.findByShipmentCode(shipmentCode);
        if (shipmentOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ΣΦΑΛΜΑ: Ο φάκελος δρομολογίου " + shipmentCode + " δεν βρέθηκε.");
        }
        Shipment shipment = shipmentOpt.get();

        if (shipment.getStatus() != Shipment.ShipmentStatus.PREPARATION) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ΣΦΑΛΜΑ: Το δρομολόγιο έχει οριστικοποιηθεί.");
        }

        Optional<BigBag> bagOpt = bigBagRepository.findByQrCode(qrCode);
        if (bagOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("ΣΦΑΛΜΑ: Ο κωδικός QR δεν βρέθηκε στο σύστημα.");
        }
        BigBag bag = bagOpt.get();

        if (bag.getStatus() == BigBag.BagStatus.VOIDED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ΣΦΑΛΜΑ: Ο σάκος είναι ΑΚΥΡΩΜΕΝΟΣ (VOID).");
        }

        boolean isAlreadyInShipment = bag.getShipment() != null && bag.getShipment().getId().equals(shipment.getId());

        if (bag.getShipment() != null && !isAlreadyInShipment) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("ΣΦΑΛΜΑ: Ο σάκος έχει ήδη δεσμευτεί στο δρομολόγιο " + bag.getShipment().getShipmentCode());
        }

        // --- ΕΝΗΜΕΡΩΣΗ ΣΑΚΟΥ & ΙΣΤΟΡΙΚΟΥ ΘΕΡΜΟΚΡΑΣΙΑΣ ---
        if (!isAlreadyInShipment) {
            bag.setShipment(shipment);
            bag.setStatus(BigBag.BagStatus.PENDING_EXPORT);
        }

        if (temperature != null) {
            bag.setTemperature(temperature); // Ενημέρωση τελευταίας θερμοκρασίας στον σάκο

            // Δημιουργία εγγραφής στο Ιστορικό
            TemperatureLog tempLog = new TemperatureLog();
            tempLog.setBigBag(bag);
            tempLog.setTemperature(temperature);
            temperatureLogRepository.save(tempLog);
        }

        bigBagRepository.save(bag);

        if (isAlreadyInShipment && temperature != null) {
            return ResponseEntity.ok("ΕΝΗΜΕΡΩΣΗ: Η νέα θερμοκρασία (" + temperature + " °C) καταγράφηκε επιτυχώς!");
        } else if (isAlreadyInShipment) {
            return ResponseEntity.ok("ΠΡΟΣΟΧΗ: Ο σάκος υπάρχει ήδη στο δρομολόγιο.");
        }

        return ResponseEntity.ok("ΕΠΙΤΥΧΙΑ: Ο σάκος προστέθηκε! (" + bag.getWeight() + " kg)");
    }

    // 7. Αναίρεση Σάρωσης από το Zebra
    @PostMapping("/undo")
    public ResponseEntity<String> undoScan(@RequestParam String qrCode) {
        Optional<com.mikedvl.rendering.model.BigBag> bagOpt = bigBagRepository.findByQrCode(qrCode);
        if (bagOpt.isPresent() && bagOpt.get().getShipment() != null) {
            com.mikedvl.rendering.model.BigBag bag = bagOpt.get();
            bag.setShipment(null);
            bag.setTemperature(null);
            bag.setStatus(com.mikedvl.rendering.model.BigBag.BagStatus.ACTIVE);
            bigBagRepository.save(bag);
            return ResponseEntity.ok("Η σάρωση ακυρώθηκε επιτυχώς.");
        }
        return ResponseEntity.badRequest().body("Ο σάκος δεν βρέθηκε σε δρομολόγιο.");
    }
}