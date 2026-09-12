package com.mikedvl.rendering.service;

import com.mikedvl.rendering.model.User;
import com.mikedvl.rendering.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() { return userRepository.findAll(); }

    public User getUserById(Integer id) { return userRepository.findById(id).orElse(null); }

    public User getUserByUsername(String username) { return userRepository.findByUsername(username).orElse(null); }

    public User saveUser(User user) {
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else if (user.getId() != null) {
            User existingUser = getUserById(user.getId());
            if (existingUser != null) {
                user.setPassword(existingUser.getPassword());
            }
        }
        return userRepository.save(user);
    }

    // ΝΕΑ ΜΕΘΟΔΟΣ: Διαγραφή Χρήστη
    public void deleteUser(Integer id) {
        userRepository.deleteById(id);
    }
}