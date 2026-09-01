package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.repository.BatchRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private final BatchRepository batchRepository;

    public WebController(BatchRepository batchRepository) {
        this.batchRepository = batchRepository;
    }

    @GetMapping("/")
    public String showDashboard(Model model) {
        model.addAttribute("title", "Σύστημα Διαχείρισης Rendering");
        // Στέλνουμε τη λίστα με όλα τα batches στο HTML
        model.addAttribute("batches", batchRepository.findAll());
        return "index";
    }
}