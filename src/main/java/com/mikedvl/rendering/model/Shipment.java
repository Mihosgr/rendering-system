package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shipments")
public class Shipment {

    public enum ShipmentStatus {
        PREPARATION, // Σε διαδικασία προετοιμασίας / ελέγχου θερμοκρασιών
        COMPLETED,   // Οριστικοποιήθηκε και έφυγε
        CANCELED     // Ακυρώθηκε
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String shipmentCode; // Μοναδικός κωδικός π.χ. SH-20260904-01

    private LocalDateTime createdAt;

    private String vehiclePlate; // Πινακίδα οχήματος (προαιρετικό κατά την έναρξη)

    @Enumerated(EnumType.STRING)
    private ShipmentStatus status;

    @OneToMany(mappedBy = "shipment", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<BigBag> bags = new ArrayList<>();

    private String loadingDate;
    private String entryDate;
    private String supervisor;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- Getters & Setters ---

    public String getLoadingDate() { return loadingDate; }
    public void setLoadingDate(String loadingDate) { this.loadingDate = loadingDate; }

    public String getEntryDate() { return entryDate; }
    public void setEntryDate(String entryDate) { this.entryDate = entryDate; }

    public String getSupervisor() { return supervisor; }
    public void setSupervisor(String supervisor) { this.supervisor = supervisor; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getShipmentCode() { return shipmentCode; }
    public void setShipmentCode(String shipmentCode) { this.shipmentCode = shipmentCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getVehiclePlate() { return vehiclePlate; }
    public void setVehiclePlate(String vehiclePlate) { this.vehiclePlate = vehiclePlate; }

    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }

    public List<BigBag> getBags() { return bags; }
    public void setBags(List<BigBag> bags) { this.bags = bags; }
}