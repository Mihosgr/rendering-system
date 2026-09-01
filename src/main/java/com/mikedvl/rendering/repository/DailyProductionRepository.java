package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.DailyProduction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyProductionRepository extends JpaRepository<DailyProduction, Long> {
    // Αυτόματη εύρεση της ημερήσιας παραγωγής βάσει ημερομηνίας
    Optional<DailyProduction> findByProductionDate(LocalDate date);
}