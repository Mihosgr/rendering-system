package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByStatusOrderByCreatedAtDesc(Shipment.ShipmentStatus status);

    //Αναζήτηση δρομολογίου με βάση το String SH-...
    Optional<Shipment> findByShipmentCode(String shipmentCode);
}