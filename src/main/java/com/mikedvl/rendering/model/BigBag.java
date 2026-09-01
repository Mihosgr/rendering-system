package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Column(unique = true, nullable = false)
    private String lotNumber; // Το QR Code text (π.χ. R01...)

    private Double weight;

    @Enumerated(EnumType.STRING)
    private BagStatus status;

    public enum BagStatus {
        ACTIVE,          // Διαθέσιμο στην αποθήκη
        PENDING_NIR,     // Περιμένει ανάλυση
        UNFIT,           // Ακατάλληλο (Blocked)
        REWORKED,        // Ξαναμπήκε σε παραγωγή
        EXPORTED,         // Πουλήθηκε
        MISSING           // Για περιπτώσεις απώλειας κατά την απογραφή
    }

    // TODO: Generate Getters and Setters
}