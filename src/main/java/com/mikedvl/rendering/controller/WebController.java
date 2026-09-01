package com.mikedvl.rendering.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String showDashboard(Model model) {
        // Στέλνουμε μια μεταβλητή (title) στο HTML
        model.addAttribute("title", "Σύστημα Διαχείρισης Rendering");
        // Επιστρέφει το αρχείο index.html (χωρίς την κατάληξη)
        return "index";
    }
}