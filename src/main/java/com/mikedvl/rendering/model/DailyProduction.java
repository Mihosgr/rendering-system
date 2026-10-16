package com.mikedvl.rendering.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "daily_productions")
public class DailyProduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate productionDate;

    private LocalTime slaughterhouseStartTime;

    private Integer birdsCount;

    // Μια ημερήσια παραγωγή έχει πολλά batches
    @OneToMany(mappedBy = "dailyProduction", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("dailyProduction")
    private List<Batch> batches;

    // TODO: Generate Getters and Setters (Alt + Insert -> Getter and Setter)
}