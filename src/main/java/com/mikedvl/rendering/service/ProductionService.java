package com.mikedvl.rendering.service;

import com.mikedvl.rendering.model.Batch;
import org.springframework.stereotype.Service;

@Service
public class ProductionService {

    /**
     * Υπολογίζει την καθαρή απόδοση (Yield %) ενός Batch.
     * Τύπος: ((Τελικό Άλευρο + (Τελικό Λάδι - Προστιθέμενο Λάδι)) / Καθαρή Πρώτη Ύλη) * 100
     */
    public void calculateAndSetYield(Batch batch) {
        // 1. Προστασία από διαίρεση με το μηδέν αν δεν έχει δηλωθεί πρώτη ύλη
        if (batch.getRawMaterialWeight() == null || batch.getRawMaterialWeight() <= 0) {
            batch.setYieldPercentage(0.0);
            return;
        }

        // 2. Μετατροπή πιθανών null τιμών σε 0.0 για ασφαλείς πράξεις
        double mealWeight = batch.getFinalMealWeight() != null ? batch.getFinalMealWeight() : 0.0;
        double finalOil = batch.getFinalOilWeight() != null ? batch.getFinalOilWeight() : 0.0;
        double addedOil = batch.getAddedOilWeight() != null ? batch.getAddedOilWeight() : 0.0;

        // 3. Υπολογισμός καθαρού λαδιού (Math.max αποτρέπει αρνητικό νούμερο σε περίπτωση λάθους ζύγισης)
        double netOilProduced = Math.max(0, finalOil - addedOil);

        // 4. Καθαρή παραγωγή (Άλευρο + Καθαρό Λάδι)
        double totalNetProduction = mealWeight + netOilProduced;

        // 5. Υπολογισμός ποσοστού (%)
        double yield = (totalNetProduction / batch.getRawMaterialWeight()) * 100;

        // 6. Στρογγυλοποίηση σε 2 δεκαδικά (π.χ. 26.15)
        yield = Math.round(yield * 100.0) / 100.0;

        // 7. Αποθήκευση της τιμής πίσω στο αντικείμενο Batch
        batch.setYieldPercentage(yield);
    }
}