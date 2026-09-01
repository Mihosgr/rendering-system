package com.mikedvl.rendering.controller;

import com.mikedvl.rendering.model.BigBag;
import com.mikedvl.rendering.repository.BigBagRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/bigbags")
public class BigBagController {

    private final BigBagRepository bigBagRepository;

    public BigBagController(BigBagRepository bigBagRepository) {
        this.bigBagRepository = bigBagRepository;
    }

    @GetMapping("/scan/{lotNumber}")
    public ResponseEntity<BigBag> scanBigBag(@PathVariable String lotNumber) {
        Optional<BigBag> bag = bigBagRepository.findByLotNumber(lotNumber);
        return bag.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}