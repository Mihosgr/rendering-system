package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.BigBagRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import com.mikedvl.rendering.service.BigBagService;
import com.mikedvl.rendering.service.ProductionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Controller
public class WebController {

    private final BatchRepository batchRepository;
    private final DailyProductionRepository dailyProductionRepository;
    private final BigBagRepository bigBagRepository;
    private final ProductionService productionService;
    private final BigBagService bigBagService;

    public WebController(BatchRepository batchRepository,
                         DailyProductionRepository dailyProductionRepository,
                         BigBagRepository bigBagRepository,
                         ProductionService productionService,
                         BigBagService bigBagService) {
        this.batchRepository = batchRepository;
        this.dailyProductionRepository = dailyProductionRepository;
        this.bigBagRepository = bigBagRepository;
        this.productionService = productionService;
        this.bigBagService = bigBagService;
    }

    // --- DASHBOARD ---
    @GetMapping("/")
    public String showDashboard(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate selectedDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("title", "Σύστημα Διαχείρισης Rendering");
        model.addAttribute("selectedDate", selectedDate);

        dailyProductionRepository.findByProductionDate(selectedDate).ifPresentOrElse(
                daily -> {
                    List<Batch> batches = batchRepository.findByDailyProductionId(daily.getId());
                    model.addAttribute("batches", batches);

                    // --- Ημερήσια Παραγωγή: Υπολογισμός των 3 Προϊόντων ---
                    int dailyOffalMeal = batches.stream()
                            .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                            .mapToInt(b -> b.getFinalMealWeight() != null ? b.getFinalMealWeight() : 0).sum();

                    int dailyFeatherMeal = batches.stream()
                            .filter(b -> b.getProductType() == Batch.ProductType.FEATHER_MEAL)
                            .mapToInt(b -> b.getFinalMealWeight() != null ? b.getFinalMealWeight() : 0).sum();

                    int dailyOilProduced = batches.stream()
                            .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                            .mapToInt(b -> (b.getFinalOilWeight() != null ? b.getFinalOilWeight() : 0) - (b.getAddedOilWeight() != null ? b.getAddedOilWeight() : 0))
                            .sum();

                    model.addAttribute("dailyOffalMeal", dailyOffalMeal);
                    model.addAttribute("dailyFeatherMeal", dailyFeatherMeal);
                    model.addAttribute("dailyOilProduced", dailyOilProduced);
                    // --------------------------------------------------
                },
                () -> {
                    model.addAttribute("batches", List.of());
                    model.addAttribute("dailyOffalMeal", 0.0);
                    model.addAttribute("dailyFeatherMeal", 0.0);
                    model.addAttribute("dailyOilProduced", 0.0);
                }
        );
        return "production/index";
    }

    // --- ΦΟΡΜΕΣ (ΝΕΟ & ΕΠΕΞΕΡΓΑΣΙΑ) ---
    @GetMapping("/batch/new")
    public String showNewBatchForm(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, Model model) {
        LocalDate batchDate = (date != null) ? date : LocalDate.now();
        model.addAttribute("batch", new Batch());
        model.addAttribute("productionDate", batchDate);
        return "production/new-batch";
    }

    @GetMapping("/batch/edit/{id}")
    public String editBatch(@PathVariable Long id, Model model) {
        Batch batch = batchRepository.findById(id).orElseThrow();
        model.addAttribute("batch", batch);
        model.addAttribute("productionDate", batch.getDailyProduction().getProductionDate());
        return "production/new-batch";
    }

    // --- ΑΠΟΘΗΚΕΥΣΗ ---
    @PostMapping("/batch/save")
    public String saveBatch(@ModelAttribute("batch") Batch batch,
                            @RequestParam("productionDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate productionDate,
                            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) { // Προσθήκη για μηνύματα λάθους

        // --- ΝΕΟ: Υπολογισμός και Έλεγχος Χρόνου ---
        if (batch.getLoadTime() != null && batch.getCookEndTime() != null) {
            // Υπολογισμός διαφοράς σε λεπτά
            long minutes = java.time.Duration.between(batch.getLoadTime(), batch.getCookEndTime()).toMinutes();

            // Αν τα λεπτά είναι αρνητικά (π.χ. 23:15 έως 01:30), σημαίνει ότι αλλάξαμε μέρα. Προσθέτουμε 24 ώρες (1440 λεπτά).
            if (minutes < 0) {
                minutes += 1440;
            }

            // Έλεγχος Λάθους: Αν η διάρκεια βγαίνει πάνω από 12 ώρες (720 λεπτά), είναι αδύνατον (π.χ. 17:15 με 16:00 βγάζει ~23 ώρες)
            if (minutes > 720) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "ΑΔΥΝΑΤΟΣ ΧΡΟΝΟΣ: Η ώρα φόρτωσης (" + batch.getLoadTime() + ") και η ώρα τέλους (" + batch.getCookEndTime() +
                                ") δίνουν διάρκεια " + (minutes / 60) + " ωρών! Ελέγξτε για τυπογραφικό λάθος.");

                // Επιστροφή στη φόρμα χωρίς να αποθηκευτεί
                if (batch.getId() != null) {
                    return "redirect:/batch/edit/" + batch.getId();
                } else {
                    return "redirect:/batch/new?date=" + productionDate.toString();
                }
            }

            batch.setDurationMinutes((int) minutes);
        } else {
            batch.setDurationMinutes(null); // Αν λείπει κάποια ώρα (π.χ. είναι ακόμα στο μαγείρεμα)
        }
        // ---------------------------------------------

        DailyProduction currentDaily = dailyProductionRepository.findByProductionDate(productionDate)
                .orElseGet(() -> {
                    DailyProduction newDaily = new DailyProduction();
                    newDaily.setProductionDate(productionDate);
                    return dailyProductionRepository.save(newDaily);
                });

        batch.setDailyProduction(currentDaily);

        if (batch.getId() == null) {
            batch.setBatchNumber(batchRepository.findMaxBatchNumber() + 1);
            batch.setCookerBatchNumber(batchRepository.findMaxCookerBatchNumber(currentDaily, batch.getCookerId()) + 1);
        }

        productionService.calculateAndSetYield(batch);
        batchRepository.save(batch);
        return "redirect:/?date=" + productionDate.toString();
    }

    // --- BIG BAGS ---
    @GetMapping("/batch/{id}/bigbags")
    public String showBigBags(@PathVariable Long id, Model model) {
        Batch batch = batchRepository.findById(id).orElseThrow();
        model.addAttribute("batch", batch);
        model.addAttribute("bigbags", bigBagRepository.findByBatch(batch));
        return "production/bigbags";
    }

    @PostMapping("/batch/{id}/bigbags/add")
    public String addBigBag(@PathVariable Long id, @RequestParam Double weight) {
        Batch batch = batchRepository.findById(id).orElseThrow();
        bigBagService.createBigBag(batch, weight);
        return "redirect:/batch/" + id + "/bigbags";
    }

    // ΝΕΟ ENDPOINT: ΑΚΥΡΩΣΗ BIG BAG
    @PostMapping("/batch/{batchId}/bigbags/{bagId}/void")
    public String voidBigBag(@PathVariable Long batchId, @PathVariable Long bagId) {
        bigBagService.voidBigBag(bagId);
        return "redirect:/batch/" + batchId + "/bigbags";
    }
}