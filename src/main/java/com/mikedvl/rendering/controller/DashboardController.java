package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.repository.BatchRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DashboardController {

    private final BatchRepository batchRepository;

    public DashboardController(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @GetMapping("/dashboard")
    public String showGeneralDashboard(@RequestParam(defaultValue = "7") String filter, Model model) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;
        List<Batch> batches;

        if (filter.equals("all")) {
            batches = batchRepository.findAll();
        } else {
            int days = Integer.parseInt(filter);
            startDate = endDate.minusDays(days);
            batches = batchRepository.findByDailyProductionProductionDateBetween(startDate, endDate);
        }

        // Υπολογισμοί από το Batch.java
        double totalAddedOil = batches.stream().mapToDouble(b -> b.getAddedOilWeight() != null ? b.getAddedOilWeight() : 0).sum();


        // --- 1. Υπολογισμοί Μαλακών Ιστών (Πτηνάλευρο) ---
        int totalOffalRaw = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                .mapToInt(b -> b.getRawMaterialWeight() != null ? b.getRawMaterialWeight() : 0).sum();

        int totalOffalProcessed = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                .mapToInt(b -> b.getProcessedQuantity() != null ? b.getProcessedQuantity() : 0).sum();

        int totalOffalMeal = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                .mapToInt(b -> b.getFinalMealWeight() != null ? b.getFinalMealWeight() : 0).sum();

        int totalOilProduced = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL)
                .mapToInt(b -> (b.getFinalOilWeight() != null ? b.getFinalOilWeight() : 0) - (b.getAddedOilWeight() != null ? b.getAddedOilWeight() : 0))
                .sum();

        // --- 2. Υπολογισμοί Πούπουλων (Πτεράλευρο) ---
        int totalFeatherRaw = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.FEATHER_MEAL)
                .mapToInt(b -> b.getRawMaterialWeight() != null ? b.getRawMaterialWeight() : 0).sum();

        int totalFeatherProcessed = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.FEATHER_MEAL)
                .mapToInt(b -> b.getProcessedQuantity() != null ? b.getProcessedQuantity() : 0).sum();

        int totalFeatherMeal = batches.stream()
                .filter(b -> b.getProductType() == Batch.ProductType.FEATHER_MEAL)
                .mapToInt(b -> b.getFinalMealWeight() != null ? b.getFinalMealWeight() : 0).sum();

        long countOffalBatches = batches.stream().filter(b -> b.getProductType() == Batch.ProductType.OFFAL_MEAL).count();
        long countFeatherBatches = batches.stream().filter(b -> b.getProductType() == Batch.ProductType.FEATHER_MEAL).count();

        // Απόδοση Πτηνάλευρου (Προσθήκη (double) για σωστή δεκαδική διαίρεση)
        double offalYield = totalOffalRaw > 0 ? ((double) (totalOffalMeal + totalOilProduced) / totalOffalRaw) * 100 : 0.0;

        model.addAttribute("filter", filter);
        model.addAttribute("totalAddedOil", totalAddedOil);
        model.addAttribute("totalOffalMeal", totalOffalMeal);
        model.addAttribute("totalFeatherMeal", totalFeatherMeal);
        model.addAttribute("totalOilProduced", totalOilProduced);
        model.addAttribute("countOffalBatches", countOffalBatches);
        model.addAttribute("countFeatherBatches", countFeatherBatches);
        model.addAttribute("offalYield", String.format("%.2f", offalYield));
        model.addAttribute("totalOffalRaw", totalOffalRaw);
        model.addAttribute("totalOffalProcessed", totalOffalProcessed);
        model.addAttribute("totalFeatherRaw", totalFeatherRaw);
        model.addAttribute("totalFeatherProcessed", totalFeatherProcessed);

        return "dashboard/dashboard";
    }
}