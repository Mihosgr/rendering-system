package com.mikedvl.rendering.repository;

import com.mikedvl.rendering.model.TemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TemperatureLogRepository extends JpaRepository<TemperatureLog, Long> {
    List<TemperatureLog> findByBigBagIdOrderByRecordedAtDesc(Long bigBagId);
}