package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "big_bags")
public class BigBag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    private String lotNumber;

    @Column(unique = true)
    private String qrCode; // Το πλήρες string (π.χ. R01010101800001...)

    private Double weight;

    // ΝΕΟ ΠΕΔΙΟ: Ο Α/Α του συγκεκριμένου σάκου μέσα στο Batch
    private Integer bagSerialNumber;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BagStatus status;

    public enum BagStatus {
        ACTIVE, VOIDED, PENDING_EXPORT, EXPORTED
    }

    @ManyToOne
    @JoinColumn(name = "shipment_id")
    private Shipment shipment; // Το δρομολόγιο στο οποίο έχει δεσμευτεί

    private Double temperature; // Η τελευταία θερμοκρασία που καταγράφηκε

    @OneToMany(mappedBy = "bigBag", cascade = CascadeType.ALL)
    @OrderBy("recordedAt DESC") // Ταξινομεί αυτόματα από την πιο πρόσφατη στην παλαιότερη
    private List<TemperatureLog> temperatureLogs = new ArrayList<>();

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public List<TemperatureLog> getTemperatureLogs() {
        return temperatureLogs;
    }

    public void setTemperatureLogs(List<TemperatureLog> temperatureLogs) {
        this.temperatureLogs = temperatureLogs;
    }
}