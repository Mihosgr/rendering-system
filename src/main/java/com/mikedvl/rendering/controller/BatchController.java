package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.repository.BatchRepository;
import com.mikedvl.rendering.service.ProductionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/batches")
public class BatchController {

    private final BatchRepository batchRepository;
    private final ProductionService productionService;

    public BatchController(BatchRepository batchRepository, ProductionService productionService) {
        this.batchRepository = batchRepository;
        this.productionService = productionService;
    }

    @PostMapping
    public ResponseEntity<Batch> createBatch(@RequestBody Batch batch) {
        // Υπολογισμός απόδοσης πριν την αποθήκευση
        productionService.calculateAndSetYield(batch);

        // Αποθήκευση στη βάση δεδομένων
        Batch savedBatch = batchRepository.save(batch);
        return ResponseEntity.ok(savedBatch);
    }

    @GetMapping
    public ResponseEntity<List<Batch>> getAllBatches() {
        return ResponseEntity.ok(batchRepository.findAll());
    }
}