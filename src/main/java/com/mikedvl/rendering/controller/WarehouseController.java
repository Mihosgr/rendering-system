package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.repository.BigBagRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
public class WarehouseController {

    private final BigBagRepository bigBagRepository;

    public WarehouseController(BigBagRepository bigBagRepository) {
        this.bigBagRepository = bigBagRepository;
    }

    @GetMapping("/warehouse")
    public String showWarehouse(
            @RequestParam(required = false) BigBag.BagStatus status,
            @RequestParam(required = false) Batch.ProductType productType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String searchTerm,
            Model model) {

        if (searchTerm != null && searchTerm.trim().isEmpty()) {
            searchTerm = null;
        }

        List<BigBag> bags = bigBagRepository.findWithFilters(status, productType, startDate, endDate, searchTerm);

        double totalWeight = bags.stream().mapToDouble(b -> b.getWeight() != null ? b.getWeight() : 0).sum();

        model.addAttribute("bags", bags);
        model.addAttribute("totalWeight", totalWeight);
        model.addAttribute("bagCount", bags.size());

        // Κρατάμε τις επιλογές των φίλτρων στο model
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedProductType", productType);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("searchTerm", searchTerm); // ΔΙΟΡΘΩΣΗ: searchTerm αντί για lotNumber

        return "warehouse/warehouse";
    }
}