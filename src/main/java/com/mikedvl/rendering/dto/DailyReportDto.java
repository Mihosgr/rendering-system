package com.mikedvl.rendering.dto;

import java.time.LocalDate;

public class DailyReportDto {
    private LocalDate date;
    private double softTissueWeight;  // 07-99-12 (Πρώτη Ύλη Πτηνάλευρου)
    private double offalMealWeight;   // 80-00-01 (Πτηνάλευρο)
    private double oilWeight;         // 80-00-02 (Πτηνέλαιο)
    private double feathersWeight;    // 07-99-11 (Πούπουλα)
    private double featherMealWeight; // 80-00-03 (Πτεράλευρο)

    public DailyReportDto() {}

    public DailyReportDto(LocalDate date, double softTissueWeight, double offalMealWeight, double oilWeight, double feathersWeight, double featherMealWeight) {
        this.date = date;
        this.softTissueWeight = softTissueWeight;
        this.offalMealWeight = offalMealWeight;
        this.oilWeight = oilWeight;
        this.feathersWeight = feathersWeight;
        this.featherMealWeight = featherMealWeight;
    }

    // Getters & Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public double getSoftTissueWeight() { return softTissueWeight; }
    public void setSoftTissueWeight(double softTissueWeight) { this.softTissueWeight = softTissueWeight; }

    public double getOffalMealWeight() { return offalMealWeight; }
    public void setOffalMealWeight(double offalMealWeight) { this.offalMealWeight = offalMealWeight; }

    public double getOilWeight() { return oilWeight; }
    public void setOilWeight(double oilWeight) { this.oilWeight = oilWeight; }

    public double getFeathersWeight() { return feathersWeight; }
    public void setFeathersWeight(double feathersWeight) { this.feathersWeight = feathersWeight; }

    public double getFeatherMealWeight() { return featherMealWeight; }
    public void setFeatherMealWeight(double featherMealWeight) { this.featherMealWeight = featherMealWeight; }
}