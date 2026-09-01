package com.mikedvl.rendering.bootstrap;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.DailyProduction;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.repository.DailyProductionRepository;
import com.mikedvl.rendering.service.ProductionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class DataInitializer implements CommandLineRunner {

    private final DailyProductionRepository dailyRepo;
    private final BatchRepository batchRepo;
    private final ProductionService productionService;

    public DataInitializer(DailyProductionRepository dailyRepo, BatchRepository batchRepo, ProductionService productionService) {
        this.dailyRepo = dailyRepo;
        this.batchRepo = batchRepo;
        this.productionService = productionService;
    }

    @Override
    public void run(String... args) throws Exception {
        // Έλεγχος αν υπάρχουν ήδη δεδομένα για να μην τα διπλοπεράσουμε
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
            batch1.setRawMaterialWeight(10000.0);
            batch1.setAddedOilWeight(400.0);
            batch1.setFinalMealWeight(1800.0);
            batch1.setFinalOilWeight(1200.0);

            // Υπολογισμός απόδοσης
            productionService.calculateAndSetYield(batch1);
            batchRepo.save(batch1);
        }
    }
}