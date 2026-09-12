package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.DailyProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByDailyProductionId(Long dailyProductionId);

    // Αναζήτηση βάσει Daily + CookerId + CookerBatchNumber (Θα χρησιμοποιηθεί από το Zebra)
    Optional<Batch> findByDailyProductionAndCookerIdAndCookerBatchNumber(DailyProduction daily, Integer cookerId, Integer cookerBatchNumber);

    // Εύρεση του τελευταίου γενικού Α/Α
    @Query("SELECT COALESCE(MAX(b.batchNumber), 0) FROM Batch b")
    Integer findMaxBatchNumber();

    // Εύρεση του τελευταίου Α/Α για συγκεκριμένο Cooker τη σημερινή μέρα
    @Query("SELECT COALESCE(MAX(b.cookerBatchNumber), 0) FROM Batch b WHERE b.dailyProduction = :daily AND b.cookerId = :cookerId")
    Integer findMaxCookerBatchNumber(@Param("daily") DailyProduction daily, @Param("cookerId") Integer cookerId);

    //Φέρνει τα Batches με βάση ένα εύρος ημερομηνιών
    List<Batch> findByDailyProductionProductionDateBetween(LocalDate startDate, LocalDate endDate);
}