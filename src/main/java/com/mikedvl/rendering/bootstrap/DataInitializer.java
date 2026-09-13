package com.mikedvl.rendering.bootstrap;

import com.mikedvl.rendering.model.User;
import com.mikedvl.rendering.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Δημιουργία του αρχικού Admin μόνο αν δεν υπάρχει κανένας χρήστης στη βάση
        if (userRepo.count() == 0) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("1"));
            admin.setFullName("Διαχειριστής Συστήματος");
            admin.setRole("ADMIN");
            userRepo.save(admin);
            System.out.println("✅ Δημιουργήθηκε ο αρχικός λογαριασμός admin!");
        }
    }
}