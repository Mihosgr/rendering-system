package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.Batch;
import com.mikedvl.rendering.model.BigBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BigBagRepository extends JpaRepository<BigBag, Long> {
    Optional<BigBag> findByLotNumber(String lotNumber);
    List<BigBag> findByStatus(BigBag.BagStatus status);
    List<BigBag> findByBatch(Batch batch);

    Optional<BigBag> findByBatchAndBagSerialNumber(Batch batch, Integer bagSerialNumber);

    // ΝΕΑ ΜΕΘΟΔΟΣ: Εύρεση του μεγαλύτερου Α/Α σάκου μέσα σε ένα συγκεκριμένο Batch
    @Query("SELECT COALESCE(MAX(b.bagSerialNumber), 0) FROM BigBag b WHERE b.batch = :batch")
    Integer findMaxBagSerialNumberByBatch(@Param("batch") Batch batch);

    @Query("SELECT b FROM BigBag b WHERE " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:productType IS NULL OR b.batch.productType = :productType) AND " +
            "(:startDate IS NULL OR b.batch.dailyProduction.productionDate >= :startDate) AND " +
            "(:endDate IS NULL OR b.batch.dailyProduction.productionDate <= :endDate) AND " +
            "(:searchTerm IS NULL OR b.lotNumber LIKE %:searchTerm% OR b.qrCode LIKE %:searchTerm%) " +
            "ORDER BY b.id DESC")
    List<BigBag> findWithFilters(@Param("status") BigBag.BagStatus status,
                                 @Param("productType") Batch.ProductType productType,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("searchTerm") String searchTerm);

    // Μας δίνει τη δυνατότητα να βρίσκει έναν σάκο με βάση το ακριβές QR Code.
    Optional<BigBag> findByQrCode(String qrCode);
}