package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    // Εύρεση όλων των Batches που ανήκουν σε μια συγκεκριμένη μέρα παραγωγής
    List<Batch> findByDailyProductionId(Long dailyProductionId);
}