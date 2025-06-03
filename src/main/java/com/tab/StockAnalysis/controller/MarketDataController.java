/*
package com.tab.StockAnalysis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.tab.StockAnalysis.entity.MarketData;
import com.tab.StockAnalysis.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/market-data")
@RequiredArgsConstructor
public class MarketDataController {

    private final MarketDataService marketDataService;

    @GetMapping("/{symbol}")
    public ResponseEntity<MarketData> getLatestMarketData(@PathVariable String symbol) {
        return marketDataService.getLatestMarketData(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/history")
    public ResponseEntity<List<MarketData>> getMarketDataHistory(@PathVariable String symbol) {
        List<MarketData> history = marketDataService.getMarketDataHistory(symbol);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{symbol}/range")
    public ResponseEntity<List<MarketData>> getMarketDataByTimeRange(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<MarketData> rangeData = marketDataService.getMarketDataByTimeRange(symbol, startTime, endTime);
        return ResponseEntity.ok(rangeData);
    }

    @GetMapping("/by-market-cap")
    public ResponseEntity<List<MarketData>> getMarketDataByMarketCap(
            @RequestParam Double minMarketCap) {
        List<MarketData> data = marketDataService.getMarketDataByMarketCap(minMarketCap);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/by-volume")
    public ResponseEntity<List<MarketData>> getMarketDataByVolume(
            @RequestParam Long minVolume) {
        List<MarketData> data = marketDataService.getMarketDataByVolume(minVolume);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/by-exchange/{exchange}")
    public ResponseEntity<List<MarketData>> getMarketDataByExchange(
            @PathVariable String exchange) {
        List<MarketData> data = marketDataService.getMarketDataByExchange(exchange);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/update/{symbol}")
    public ResponseEntity<MarketData> updateMarketData(@PathVariable String symbol) {
        marketDataService.evictCache(symbol);
        return marketDataService.getLatestMarketData(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/update/all")
    public ResponseEntity<Map<String, Object>> triggerManualUpdate() {
        Map<String, Object> status = marketDataService.triggerManualUpdate();
        return ResponseEntity.ok(status);
    }

    @GetMapping("/update/status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        Map<String, Object> status = marketDataService.getUpdateStatus();
        return ResponseEntity.ok(status);
    }
} */
