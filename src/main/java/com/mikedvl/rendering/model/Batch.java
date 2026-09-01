package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

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

    private Integer batchNumber; // Το Α/Α

    @Enumerated(EnumType.STRING)
    private ProductType productType; // ΠΤΗΝΑΛΕΥΡΟ ή ΠΤΕΡΑΛΕΥΡΟ

    private LocalTime loadTime;
    private LocalTime cookEndTime;

    // --- Inputs (Φορτώσεις) ---
    private Double rawMaterialWeight; // Μαλακά ή Φτερά ανάλογα το type
    private Double addedOilWeight; // Λάδι (βοηθητικό)
    private Double skinFatWeight; // Λίπος
    private Double pastaWeight; // Πάστα
    private Double bonesWeight; // Κόκαλα
    private Double reworkWeight; // Βάρος από Big Bags που επιστρέφουν

    // --- Outputs (Ζυγίσεις) ---
    private Double cooker1EndWeight;
    private Double cooker2EndWeight;
    private Double cooker3EndWeight;

    private Double finalMealWeight; // Ζύγιση Μαλακά / Ζύγιση Φτερά
    private Double finalOilWeight; // Τελικό Λάδι

    // Υπολογισμένη απόδοση (Yield) %
    private Double yieldPercentage;

    public enum ProductType {
        OFFAL_MEAL, FEATHER_MEAL
    }

    // TODO: Generate Getters and Setters
}