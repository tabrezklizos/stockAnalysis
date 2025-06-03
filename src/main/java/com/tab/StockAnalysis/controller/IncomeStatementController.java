package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.IncomeStatement;
import com.tab.StockAnalysis.service.IncomeStatementService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/income-statements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class IncomeStatementController {

    private final IncomeStatementService incomeStatementService;

    @GetMapping
    public ResponseEntity<List<IncomeStatement>> getAllIncomeStatements() {
        return ResponseEntity.ok(incomeStatementService.getAllIncomeStatements());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<IncomeStatement>> getIncomeStatementsBySymbol(@PathVariable String symbol) {
        List<IncomeStatement> statements = incomeStatementService.getIncomeStatementsBySymbol(symbol);
        return !statements.isEmpty() ? ResponseEntity.ok(statements) : ResponseEntity.notFound().build();
    }

    @GetMapping("/symbol/{symbol}/period/{period}")
    public ResponseEntity<List<IncomeStatement>> getIncomeStatementsBySymbolAndPeriod(
            @PathVariable String symbol,
            @PathVariable String period) {
        List<IncomeStatement> statements = incomeStatementService.getIncomeStatementsBySymbolAndPeriod(symbol, period);
        return !statements.isEmpty() ? ResponseEntity.ok(statements) : ResponseEntity.notFound().build();
    }

    @GetMapping("/symbol/{symbol}/date-range")
    public ResponseEntity<List<IncomeStatement>> getIncomeStatementsBySymbolAndDateRange(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<IncomeStatement> statements = incomeStatementService.getIncomeStatementsBySymbolAndDateRange(symbol, startDate, endDate);
        return !statements.isEmpty() ? ResponseEntity.ok(statements) : ResponseEntity.notFound().build();
    }

    @GetMapping("/update/status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        return ResponseEntity.ok(incomeStatementService.getUpdateStatus());
    }

    @PostMapping("/update/trigger")
    public ResponseEntity<Map<String, Object>> triggerUpdate() {
        return ResponseEntity.ok(incomeStatementService.triggerManualUpdate());
    }
} 