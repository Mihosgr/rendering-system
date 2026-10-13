package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.AuditSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuditSessionRepository extends JpaRepository<AuditSession, Long> {

    // Επιστρέφει την πιο πρόσφατη ΑΝΟΙΧΤΗ (OPEN) συνεδρία απογραφής
    Optional<AuditSession> findFirstByStatusOrderByIdDesc(AuditSession.SessionStatus status);
}