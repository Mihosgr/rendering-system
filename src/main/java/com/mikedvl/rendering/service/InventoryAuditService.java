package com.mikedvl.rendering.service;

import com.mikedvl.rendering.dto.AuditResultDto;
import com.mikedvl.rendering.model.AuditSession;
import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.repository.AuditSessionRepository;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.BigBagRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class InventoryAuditService {

    private final BigBagRepository bigBagRepository;
    private final BatchRepository batchRepository;
    private final DailyProductionRepository dailyProductionRepository;
    private final AuditSessionRepository auditSessionRepository;

    public InventoryAuditService(BigBagRepository bigBagRepository,
                                 BatchRepository batchRepository,
                                 DailyProductionRepository dailyProductionRepository,
                                 AuditSessionRepository auditSessionRepository) {
        this.bigBagRepository = bigBagRepository;
        this.batchRepository = batchRepository;
        this.dailyProductionRepository = dailyProductionRepository;
        this.auditSessionRepository = auditSessionRepository;
    }

    /**
     * Επιστρέφει την ενεργή συνεδρία απογραφής ή δημιουργεί νέα αν δεν υπάρχει ανοιχτή.
     */
    @Transactional
    public AuditSession getOrCreateActiveSession() {
        return auditSessionRepository.findFirstByStatusOrderByIdDesc(AuditSession.SessionStatus.OPEN)
                .orElseGet(() -> {
                    AuditSession session = new AuditSession();
                    session.setSessionName("Απογραφή " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                    session.setCreatedAt(LocalDateTime.now());
                    session.setUpdatedAt(LocalDateTime.now());
                    session.setStatus(AuditSession.SessionStatus.OPEN);
                    return auditSessionRepository.save(session);
                });
    }

    /**
     * Προσθέτει νέα σαρωμένα QR codes στην ενεργή συνεδρία.
     */
    @Transactional
    public AuditSession addScannedQrsToActiveSession(List<String> newQrs) {
        AuditSession session = getOrCreateActiveSession();
        if (newQrs != null && !newQrs.isEmpty()) {
            Set<String> currentSet = new LinkedHashSet<>(session.getScannedQrCodes());
            for (String qr : newQrs) {
                if (qr != null && !qr.trim().isEmpty()) {
                    currentSet.add(qr.trim());
                }
            }
            session.getScannedQrCodes().clear();
            session.getScannedQrCodes().addAll(currentSet);
            session.setUpdatedAt(LocalDateTime.now());
            auditSessionRepository.save(session);
        }
        return session;
    }

    /**
     * Εκτελεί τη διασταύρωση για την ενεργή συνεδρία απογραφής.
     */
    @Transactional(readOnly = true)
    public AuditResultDto performAuditForActiveSession() {
        AuditSession session = getOrCreateActiveSession();
        return performAudit(session.getScannedQrCodes());
    }

    /**
     * Μηχανή Διασταύρωσης
     */
    public AuditResultDto performAudit(List<String> scannedQrCodes) {
        AuditResultDto result = new AuditResultDto();

        if (scannedQrCodes == null) {
            scannedQrCodes = Collections.emptyList();
        }

        Set<String> scannedSet = new HashSet<>();
        for (String qr : scannedQrCodes) {
            if (qr != null && !qr.trim().isEmpty()) {
                scannedSet.add(qr.trim());
            }
        }

        List<BigBag> activeBagsInDb = bigBagRepository.findByStatus(BigBag.BagStatus.ACTIVE);
        Map<String, BigBag> activeMapByQr = new HashMap<>();
        for (BigBag bag : activeBagsInDb) {
            if (bag.getQrCode() != null) {
                activeMapByQr.put(bag.getQrCode(), bag);
            }
        }

        Set<String> processedScannedQrs = new HashSet<>();

        for (String qr : scannedSet) {
            processedScannedQrs.add(qr);

            if (activeMapByQr.containsKey(qr)) {
                result.getMatches().add(activeMapByQr.get(qr));
            } else {
                Optional<BigBag> bagOpt = bigBagRepository.findByQrCode(qr);
                if (bagOpt.isPresent()) {
                    result.getSurplus().add(bagOpt.get());
                } else {
                    result.getUnknown().add(qr);
                }
            }
        }

        for (BigBag activeBag : activeBagsInDb) {
            if (!processedScannedQrs.contains(activeBag.getQrCode())) {
                result.getMissing().add(activeBag);
            }
        }

        return result;
    }

    /**
     * Οριστικοποίηση & Κλείσιμο Συνεδρίας
     */
    @Transactional
    public void finalizeActiveSession() {
        auditSessionRepository.findFirstByStatusOrderByIdDesc(AuditSession.SessionStatus.OPEN).ifPresent(session -> {
            session.setStatus(AuditSession.SessionStatus.COMPLETED);
            session.setCompletedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            auditSessionRepository.save(session);
        });
    }

    /**
     * Ακύρωση / Καθαρισμός Ενεργής Συνεδρίας
     */
    @Transactional
    public void clearActiveSession() {
        auditSessionRepository.findFirstByStatusOrderByIdDesc(AuditSession.SessionStatus.OPEN).ifPresent(session -> {
            session.setStatus(AuditSession.SessionStatus.CANCELLED);
            session.setUpdatedAt(LocalDateTime.now());
            auditSessionRepository.save(session);
        });
    }

    /**
     * Διορθωτικές Ενέργειες
     */
    @Transactional
    public void resolveSurplusToActive(List<Long> bagIds) {
        if (bagIds == null) return;
        for (Long bagId : bagIds) {
            bigBagRepository.findById(bagId).ifPresent(bag -> {
                bag.setStatus(BigBag.BagStatus.ACTIVE);
                bag.setShipment(null);
                bigBagRepository.save(bag);
            });
        }
    }

    @Transactional
    public void resolveMissingToExported(List<Long> bagIds) {
        if (bagIds == null) return;
        for (Long bagId : bagIds) {
            bigBagRepository.findById(bagId).ifPresent(bag -> {
                bag.setStatus(BigBag.BagStatus.EXPORTED);
                bigBagRepository.save(bag);
            });
        }
    }

    @Transactional
    public BigBag importUnknownAsActive(String qrData) {
        if (qrData == null || qrData.length() != 36 || !qrData.startsWith("R01")) {
            throw new IllegalArgumentException("Μη έγκυρο QR code: " + qrData);
        }

        Optional<BigBag> existing = bigBagRepository.findByQrCode(qrData);
        if (existing.isPresent()) {
            BigBag bag = existing.get();
            bag.setStatus(BigBag.BagStatus.ACTIVE);
            bag.setShipment(null);
            return bigBagRepository.save(bag);
        }

        int bagSerialNumber = Integer.parseInt(qrData.substring(3, 5));
        int cookerNo = Integer.parseInt(qrData.substring(5, 7));
        int cookerBatch = Integer.parseInt(qrData.substring(7, 9));
        String itemCode = qrData.substring(9, 15);
        String dateStr = qrData.substring(15, 21);
        double kilos = Double.parseDouble(qrData.substring(29, 33));

        LocalDate productionDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyMMdd"));
        Batch.ProductType productType = itemCode.equals("800001") ? Batch.ProductType.OFFAL_MEAL : Batch.ProductType.FEATHER_MEAL;

        DailyProduction daily = dailyProductionRepository.findByProductionDate(productionDate)
                .orElseGet(() -> {
                    DailyProduction dp = new DailyProduction();
                    dp.setProductionDate(productionDate);
                    return dailyProductionRepository.save(dp);
                });

        Batch batch = batchRepository.findByDailyProductionAndCookerIdAndCookerBatchNumber(daily, cookerNo, cookerBatch)
                .orElseGet(() -> {
                    Batch b = new Batch();
                    b.setDailyProduction(daily);
                    b.setCookerId(cookerNo);
                    b.setCookerBatchNumber(cookerBatch);
                    b.setProductType(productType);
                    return batchRepository.save(b);
                });

        String cleanLot = productionDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + cookerNo
                + String.format("%02d", cookerBatch);

        BigBag newBag = new BigBag();
        newBag.setLotNumber(cleanLot);
        newBag.setQrCode(qrData);
        newBag.setWeight(kilos);
        newBag.setBatch(batch);
        newBag.setBagSerialNumber(bagSerialNumber);
        newBag.setStatus(BigBag.BagStatus.ACTIVE);

        return bigBagRepository.save(newBag);
    }
}