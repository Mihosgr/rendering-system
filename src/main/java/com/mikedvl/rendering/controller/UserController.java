package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.User;
import com.mikedvl.rendering.model.UserLoginHistory;
import com.mikedvl.rendering.repository.UserLoginHistoryRepository;
import com.mikedvl.rendering.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final UserLoginHistoryRepository historyRepository;

    // Οι ρόλοι του συστήματος για να πηγαίνουν στο HTML Dropdown
    private final Map<String, String> systemRoles;

    public UserController(UserService userService, UserLoginHistoryRepository historyRepository) {
        this.userService = userService;
        this.historyRepository = historyRepository;

        systemRoles = new LinkedHashMap<>();
        systemRoles.put("ADMIN", "Διαχειριστής");
        systemRoles.put("OPERATOR", "Χειριστής");
        systemRoles.put("TECHNICIAN", "Τεχνικός");
        systemRoles.put("ASSISTANT", "Βοηθός");
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        Map<Integer, List<UserLoginHistory>> loginHistories = new HashMap<>();

        for (User user : users) {
            loginHistories.put(user.getId(), historyRepository.findTop20ByUserIdOrderByLoginTimeDesc(user.getId()));
        }

        model.addAttribute("users", users);
        model.addAttribute("loginHistories", loginHistories);
        model.addAttribute("roles", systemRoles);
        return "users/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", systemRoles);
        return "users/form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Integer id, Model model) {
        User user = userService.getUserById(id);
        if (user == null) return "redirect:/users";

        user.setPassword(""); // Αδειάζουμε τον κωδικό
        model.addAttribute("user", user);
        model.addAttribute("roles", systemRoles);
        return "users/form";
    }

    @PostMapping("/save")
    public String saveUser(@ModelAttribute("user") User user) {
        userService.saveUser(user);
        return "redirect:/users";
    }

    // ΝΕΟ ΕNDPOINT: Διαγραφή
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable("id") Integer id) {
        User user = userService.getUserById(id);
        // Προστασία: Διαγράφουμε μόνο αν ο χρήστης υπάρχει ΚΑΙ δεν είναι ο admin
        if (user != null && !"admin".equalsIgnoreCase(user.getUsername())) {
            userService.deleteUser(id);
        }
        return "redirect:/users";
    }
}