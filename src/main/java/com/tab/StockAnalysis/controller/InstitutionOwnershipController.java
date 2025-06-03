// src/main/java/com/tab/StockAnalysis/controller/InstitutionOwnershipController.java
package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.InstitutionOwnership;
import com.tab.StockAnalysis.service.InstitutionOwnershipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/institutional-ownership")
@RequiredArgsConstructor
public class InstitutionOwnershipController {

    private final InstitutionOwnershipService institutionOwnershipService;

    @GetMapping("/{symbol}")
    public ResponseEntity<InstitutionOwnership> getInstitutionalOwnershipBySymbol(@PathVariable String symbol) {
        Optional<InstitutionOwnership> ownership = institutionOwnershipService.getInstitutionalOwnershipBySymbol(symbol);
        return ownership.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Add other endpoints as needed, e.g., for saving or deleting (likely for admin use)
}