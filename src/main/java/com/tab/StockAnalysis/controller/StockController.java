package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.StockData;
import com.tab.StockAnalysis.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Enable CORS for all origins
public class StockController {

    private final StockService stockService;

    @GetMapping("/{symbol}")
    public ResponseEntity<?> getStockData(@PathVariable String symbol) {
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Stock symbol cannot be empty");
            }

            StockData stockData = stockService.getStockData(symbol.toUpperCase());
            if (stockData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stockData);
        } catch (Exception e) {
            log.error("Error fetching stock data for symbol: " + symbol, e);
            return ResponseEntity.internalServerError()
                    .body("Error fetching stock data: " + e.getMessage());
        }
    }

    @GetMapping("/{symbol}/refresh")
    public ResponseEntity<?> refreshStockData(@PathVariable String symbol) {
        try {
            if (symbol == null || symbol.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Stock symbol cannot be empty");
            }

            StockData stockData = stockService.getStockData(symbol.toUpperCase());
            if (stockData == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(stockData);
        } catch (Exception e) {
            log.error("Error refreshing stock data for symbol: " + symbol, e);
            return ResponseEntity.internalServerError()
                    .body("Error refreshing stock data: " + e.getMessage());
        }
    }
}
