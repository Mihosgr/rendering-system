package com.mikedvl.rendering.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "audit_sessions")
public class AuditSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionName; // π.χ. "Απογραφή 13/10/2026 03:30"

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    private SessionStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "audit_session_qrs", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "qr_code", length = 100)
    private List<String> scannedQrCodes = new ArrayList<>();

    public enum SessionStatus {
        OPEN, COMPLETED, CANCELLED
    }
}