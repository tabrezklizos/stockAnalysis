package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.KeyStatistics;
import com.tab.StockAnalysis.service.KeyStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class KeyStatisticsController {

    private final KeyStatisticsService keyStatisticsService;

    @GetMapping("/{symbol}")
    public ResponseEntity<KeyStatistics> getKeyStatistics(@PathVariable String symbol) {
        return ResponseEntity.ok(keyStatisticsService.getKeyStatistics(symbol));
    }

    @GetMapping("/{symbol}/historical")
    public ResponseEntity<List<KeyStatistics>> getHistoricalKeyStatistics(
            @PathVariable String symbol,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(keyStatisticsService.getHistoricalKeyStatistics(symbol, startDate, endDate));
    }

    @GetMapping("/{symbol}/ratios")
    public ResponseEntity<KeyStatistics> getFinancialRatios(@PathVariable String symbol) {
        return ResponseEntity.ok(keyStatisticsService.getFinancialRatios(symbol));
    }
} 