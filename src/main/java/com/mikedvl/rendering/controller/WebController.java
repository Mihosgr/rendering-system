package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import com.mikedvl.rendering.service.ProductionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;

@Controller
public class WebController {

    private final BatchRepository batchRepository;
    private final DailyProductionRepository dailyProductionRepository;
    private final ProductionService productionService;

    public WebController(BatchRepository batchRepository,
                         DailyProductionRepository dailyProductionRepository,
                         ProductionService productionService) {
        this.batchRepository = batchRepository;
        this.dailyProductionRepository = dailyProductionRepository;
        this.productionService = productionService;
    }

    @GetMapping("/")
    public String showDashboard(Model model) {
        model.addAttribute("title", "Σύστημα Διαχείρισης Rendering");
        model.addAttribute("batches", batchRepository.findAll());
        return "index";
    }

    @GetMapping("/batch/new")
    public String showNewBatchForm(Model model) {
        model.addAttribute("batch", new Batch());
        return "new-batch";
    }

    @PostMapping("/batch/save")
    public String saveBatch(@ModelAttribute("batch") Batch batch) {
        // 1. Βρίσκουμε τη σημερινή παραγωγή ή δημιουργούμε νέα αν είναι το πρώτο batch της ημέρας
        LocalDate today = LocalDate.now();
        DailyProduction currentDaily = dailyProductionRepository.findByProductionDate(today)
                .orElseGet(() -> {
                    DailyProduction newDaily = new DailyProduction();
                    newDaily.setProductionDate(today);
                    return dailyProductionRepository.save(newDaily);
                });

        // 2. Συνδέουμε το batch με τη σημερινή παραγωγή
        batch.setDailyProduction(currentDaily);

        // 3. Υπολογίζουμε την απόδοση
        productionService.calculateAndSetYield(batch);

        // 4. Αποθηκεύουμε
        batchRepository.save(batch);

        return "redirect:/";
    }
}