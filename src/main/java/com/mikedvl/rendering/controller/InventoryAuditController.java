package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.dto.AuditResultDto;
import com.mikedvl.rendering.model.AuditSession;
import com.mikedvl.rendering.service.InventoryAuditService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class InventoryAuditController {

    private final InventoryAuditService auditService;

    public InventoryAuditController(InventoryAuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 1. Προβολή Ενεργής Απογραφής (Web UI)
     */
    @GetMapping("/inventory/audit")
    public String showAuditPage(Model model) {
        AuditSession activeSession = auditService.getOrCreateActiveSession();
        AuditResultDto auditResult = auditService.performAuditForActiveSession();

        String rawScannedText = String.join("\n", activeSession.getScannedQrCodes());

        model.addAttribute("activeSession", activeSession);
        model.addAttribute("auditResult", auditResult);
        model.addAttribute("rawScannedText", rawScannedText);

        return "inventory/audit";
    }

    /**
     * 2. Προσθήκη / Επεξεργασία Σαρώσεων από τη Φόρμα του Web UI
     */
    @PostMapping("/inventory/audit/process")
    public String processWebAudit(@RequestParam("scannedQrs") String scannedQrsText,
                                  RedirectAttributes redirectAttributes) {
        List<String> scannedList = Arrays.stream(scannedQrsText.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        auditService.addScannedQrsToActiveSession(scannedList);
        redirectAttributes.addFlashAttribute("successMessage", "Τα δεδομένα απογραφής ενημερώθηκαν επιτυχώς!");

        return "redirect:/inventory/audit";
    }

    /**
     * 3. REST API Endpoint για την Android Εφαρμογή (Bulk Upload)
     */
    @PostMapping("/api/inventory/audit/bulk-scan")
    @ResponseBody
    public ResponseEntity<AuditResultDto> processAndroidBulkScan(@RequestBody Map<String, List<String>> payload) {
        List<String> scannedQrs = payload.get("scannedQrCodes");
        auditService.addScannedQrsToActiveSession(scannedQrs);
        AuditResultDto result = auditService.performAuditForActiveSession();
        return ResponseEntity.ok(result);
    }

    /**
     * 4. Οριστικοποίηση & Κλείσιμο Συνεδρίας
     */
    @PostMapping("/inventory/audit/finalize")
    public String finalizeAudit(RedirectAttributes redirectAttributes) {
        auditService.finalizeActiveSession();
        redirectAttributes.addFlashAttribute("successMessage", "Η απογραφή οριστικοποιήθηκε και ολοκληρώθηκε επιτυχώς!");
        return "redirect:/inventory/audit";
    }

    /**
     * 5. Καθαρισμός / Νέα Απογραφή
     */
    @PostMapping("/inventory/audit/clear")
    public String clearAudit(RedirectAttributes redirectAttributes) {
        auditService.clearActiveSession();
        redirectAttributes.addFlashAttribute("successMessage", "Η ενεργή απογραφή ακυρώθηκε. Ξεκίνησε νέα συνεδρία.");
        return "redirect:/inventory/audit";
    }

    /**
     * 6. Διορθωτικές Ενέργειες
     */
    @PostMapping("/inventory/audit/resolve/surplus")
    public String resolveSurplus(@RequestParam(value = "bagIds", required = false) List<Long> bagIds,
                                 RedirectAttributes redirectAttributes) {
        if (bagIds != null && !bagIds.isEmpty()) {
            auditService.resolveSurplusToActive(bagIds);
            redirectAttributes.addFlashAttribute("successMessage", "Επαναφέρθηκαν επιτυχώς " + bagIds.size() + " σάκοι στην αποθήκη ως ΕΝΕΡΓΟΙ (ACTIVE).");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Δεν επιλέχθηκε κανένας σάκος για επαναφορά.");
        }
        return "redirect:/inventory/audit";
    }

    @PostMapping("/inventory/audit/resolve/missing")
    public String resolveMissing(@RequestParam(value = "bagIds", required = false) List<Long> bagIds,
                                 RedirectAttributes redirectAttributes) {
        if (bagIds != null && !bagIds.isEmpty()) {
            auditService.resolveMissingToExported(bagIds);
            redirectAttributes.addFlashAttribute("successMessage", "Ενημερώθηκαν επιτυχώς " + bagIds.size() + " σάκοι ως ΕΞΑΧΘΕΝΤΕΣ (EXPORTED).");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Δεν επιλέχθηκε κανένας σάκος για ενημέρωση.");
        }
        return "redirect:/inventory/audit";
    }

    @PostMapping("/inventory/audit/resolve/import-unknown")
    public String importUnknownQr(@RequestParam("qrData") String qrData,
                                  RedirectAttributes redirectAttributes) {
        try {
            auditService.importUnknownAsActive(qrData);
            redirectAttributes.addFlashAttribute("successMessage", "Ο σάκος με QR " + qrData + " εισήχθη επιτυχώς ως ΕΝΕΡΓΟΣ (ACTIVE).");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Σφάλμα κατά την εισαγωγή: " + e.getMessage());
        }
        return "redirect:/inventory/audit";
    }
}