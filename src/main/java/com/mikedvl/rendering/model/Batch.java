package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
@Entity
@Table(name = "batches")
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "daily_production_id", nullable = false)
    private DailyProduction dailyProduction;

    // --- Νέα πεδία για διασύνδεση Zebra & Πίνακα ---
    private Integer batchNumber; // Γενικός Α/Α (Υπολογίζεται αυτόματα)
    private Integer cookerId; // "Cooker No" (π.χ. 1, 2, 3)
    private Integer cookerBatchNumber; // Α/Α του συγκεκριμένου Cooker για τη μέρα

    @Enumerated(EnumType.STRING)
    private ProductType productType; // ΠΤΗΝΑΛΕΥΡΟ ή ΠΤΕΡΑΛΕΥΡΟ

    // --- Χρόνοι ---
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime loadTime; // "Ώρα Φόρτωσης"

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime cookEndTime; // "Ώρα Τέλους"

    // Διάρκεια κάθε batchσε λεπτά
    private Integer durationMinutes;

    // --- Ποσότητες ---
    private Integer rawMaterialWeight; // "Ποσότητα Φόρτ. (kg)"
    private Integer processedQuantity; // "Ποσότητα Επεξ. (kg)"
    private Integer addedOilWeight; // "Προσθήκη Λαδιού (kg)"
    private Integer finalMealWeight; // "Άλευρο (kg)"
    private Integer finalOilWeight; // "Τελικό Λάδι (kg)"

    private Double yieldPercentage; // "Απόδοση (%)"

    public enum ProductType {
        OFFAL_MEAL, FEATHER_MEAL
    }
}