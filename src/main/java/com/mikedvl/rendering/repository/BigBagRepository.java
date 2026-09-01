package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.BigBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BigBagRepository extends JpaRepository<BigBag, Long> {
    // Άμεση εύρεση του Big Bag από το QR Code (π.χ. R01...)
    Optional<BigBag> findByLotNumber(String lotNumber);

    // Εύρεση αποθέματος ανάλογα με την κατάσταση (π.χ. ACTIVE)
    List<BigBag> findByStatus(BigBag.BagStatus status);
}