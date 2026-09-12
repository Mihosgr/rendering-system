package com.mikedvl.rendering.bootstrap;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.model.User;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import com.mikedvl.rendering.repository.UserRepository;
import com.mikedvl.rendering.service.ProductionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DailyProductionRepository dailyRepo;
    private final BatchRepository batchRepo;
    private final ProductionService productionService;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(DailyProductionRepository dailyRepo,
                           BatchRepository batchRepo,
                           ProductionService productionService,
                           UserRepository userRepo,
                           PasswordEncoder passwordEncoder) {
        this.dailyRepo = dailyRepo;
        this.batchRepo = batchRepo;
        this.productionService = productionService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. Δημιουργία ή Ενημέρωση του Admin με κωδικό "1"
        User admin = userRepo.findByUsername("admin").orElse(new User());
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("1"));
        admin.setFullName("Διαχειριστής Συστήματος");
        admin.setRole("ADMIN");
        userRepo.save(admin);
        System.out.println("✅ Ο λογαριασμός admin ενημερώθηκε επιτυχώς (admin / 1)!");

        // 2. Αρχικοποίηση Δοκιμαστικών Δεδομένων Παραγωγής
        if (dailyRepo.count() == 0) {
            DailyProduction daily = new DailyProduction();
            daily.setProductionDate(LocalDate.now());
            daily.setSlaughterhouseStartTime(LocalTime.of(6, 0));
            daily.setBirdsCount(120000);
            DailyProduction savedDaily = dailyRepo.save(daily);

            Batch batch1 = new Batch();
            batch1.setDailyProduction(savedDaily);
            batch1.setBatchNumber(1);
            batch1.setProductType(Batch.ProductType.OFFAL_MEAL);
            batch1.setLoadTime(LocalTime.of(8, 30));
            batch1.setCookEndTime(LocalTime.of(12, 45));
            batch1.setRawMaterialWeight(10000);
            batch1.setAddedOilWeight(400);
            batch1.setFinalMealWeight(1800);
            batch1.setFinalOilWeight(1200);

            productionService.calculateAndSetYield(batch1);
            batchRepo.save(batch1);
        }
    }
}