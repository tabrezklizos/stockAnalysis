package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.CashFlow;
import com.tab.StockAnalysis.service.CashFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/cashflows")
@RequiredArgsConstructor
public class CashFlowController {

    private final CashFlowService cashFlowService;

    @GetMapping
    public ResponseEntity<List<CashFlow>> getAllCashFlows() {
        return ResponseEntity.ok(cashFlowService.getAllCashFlows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CashFlow> getCashFlowById(@PathVariable String id) {
        return cashFlowService.getCashFlowById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<CashFlow>> getCashFlowsBySymbol(@PathVariable String symbol) {
        List<CashFlow> cashFlows = cashFlowService.getCashFlowsBySymbol(symbol);
        return ResponseEntity.ok(cashFlows);
    }

    @GetMapping("/symbol/{symbol}/latest")
    public ResponseEntity<CashFlow> getLatestCashFlow(@PathVariable String symbol) {
        CashFlow latestCashFlow = cashFlowService.getLatestCashFlow(symbol);
        return latestCashFlow != null ? ResponseEntity.ok(latestCashFlow) : ResponseEntity.notFound().build();
    }

    @GetMapping("/symbol/{symbol}/range")
    public ResponseEntity<List<CashFlow>> getCashFlowsBySymbolAndDateRange(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CashFlow> cashFlows = cashFlowService.getCashFlowsBySymbolAndDateRange(symbol, startDate, endDate);
        return ResponseEntity.ok(cashFlows);
    }

    @PostMapping
    public ResponseEntity<CashFlow> createCashFlow(@RequestBody CashFlow cashFlow) {
        return ResponseEntity.ok(cashFlowService.saveCashFlow(cashFlow));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCashFlow(@PathVariable String id) {
        cashFlowService.deleteCashFlow(id);
        return ResponseEntity.ok().build();
    }
} 