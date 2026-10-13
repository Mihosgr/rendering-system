package com.mikedvl.rendering.dto;

import com.mikedvl.rendering.model.BigBag;
import java.util.ArrayList;
import java.util.List;

public class AuditResultDto {
    // 1. Πράσινο: Σαρώθηκαν & ήταν ACTIVE στη βάση
    private List<BigBag> matches = new ArrayList<>();

    // 2. Κόκκινο: Ήταν ACTIVE στη βάση αλλά ΔΕΝ σαρώθηκαν στην αποθήκη
    private List<BigBag> missing = new ArrayList<>();

    // 3. Πορτοκαλί: Σαρώθηκαν στην αποθήκη αλλά στη βάση ήταν EXPORTED/VOIDED
    private List<BigBag> surplus = new ArrayList<>();

    // 4. Μπλε: Σαρώθηκαν αλλά ΔΕΝ υπάρχουν καθόλου στη βάση (Άγνωστα QR)
    private List<String> unknown = new ArrayList<>();

    public AuditResultDto() {}

    // Getters & Setters
    public List<BigBag> getMatches() { return matches; }
    public void setMatches(List<BigBag> matches) { this.matches = matches; }

    public List<BigBag> getMissing() { return missing; }
    public void setMissing(List<BigBag> missing) { this.missing = missing; }

    public List<BigBag> getSurplus() { return surplus; }
    public void setSurplus(List<BigBag> surplus) { this.surplus = surplus; }

    public List<String> getUnknown() { return unknown; }
    public void setUnknown(List<String> unknown) { this.unknown = unknown; }
}