package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.Earnings;
import com.tab.StockAnalysis.service.EarningsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/earnings")
@RequiredArgsConstructor
public class EarningsController {

    private final EarningsService earningsService;

    @GetMapping("/{symbol}")
    public ResponseEntity<Earnings> getEarnings(@PathVariable String symbol) {
        return ResponseEntity.ok(earningsService.getEarnings(symbol));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<Earnings>> getUpcomingEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(3);
        }
        return ResponseEntity.ok(earningsService.getUpcomingEarnings(startDate, endDate));
    }

    @GetMapping("/high-growth")
    public ResponseEntity<List<Earnings>> getHighGrowthEarnings(
            @RequestParam(defaultValue = "0.10") BigDecimal growthThreshold) {
        return ResponseEntity.ok(earningsService.getHighGrowthEarnings(growthThreshold));
    }

    @GetMapping("/positive-surprise")
    public ResponseEntity<List<Earnings>> getPositiveSurpriseEarnings(
            @RequestParam(defaultValue = "70.0") BigDecimal surpriseThreshold) {
        return ResponseEntity.ok(earningsService.getPositiveSurpriseEarnings(surpriseThreshold));
    }
} 