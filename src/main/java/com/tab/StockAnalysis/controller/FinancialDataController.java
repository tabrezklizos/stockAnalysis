/*
package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.FinancialData;
import com.tab.StockAnalysis.service.FinancialDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/financial-data")
@RequiredArgsConstructor
public class FinancialDataController {

    private final FinancialDataService financialDataService;

    @GetMapping("/{symbol}")
    public ResponseEntity<FinancialData> getFinancialData(@PathVariable String symbol) {
        return financialDataService.getFinancialData(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/market-cap/{minMarketCap}")
    public ResponseEntity<List<FinancialData>> getByMarketCap(@PathVariable BigDecimal minMarketCap) {
        return ResponseEntity.ok(financialDataService.getByMarketCap(minMarketCap));
    }

    @PostMapping("/refresh/{symbol}")
    public ResponseEntity<FinancialData> refreshFinancialData(@PathVariable String symbol) {
        financialDataService.evictCache(symbol);
        return financialDataService.getFinancialData(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> deleteFinancialData(@PathVariable String symbol) {
        financialDataService.deleteBySymbol(symbol);
        financialDataService.evictCache(symbol);
        return ResponseEntity.ok().build();
    }
} */
