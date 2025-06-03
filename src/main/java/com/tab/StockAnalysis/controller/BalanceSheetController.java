package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.BalanceSheet;
import com.tab.StockAnalysis.service.BalanceSheetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/balance-sheets")
@CrossOrigin(origins = "*")
public class BalanceSheetController {

    private final BalanceSheetService balanceSheetService;

    @Autowired
    public BalanceSheetController(BalanceSheetService balanceSheetService) {
        this.balanceSheetService = balanceSheetService;
    }

    @GetMapping
    public ResponseEntity<List<BalanceSheet>> getAllBalanceSheets() {
        return ResponseEntity.ok(balanceSheetService.getAllBalanceSheets());
    }

    @GetMapping("/id/{id}")
    public ResponseEntity<BalanceSheet> getBalanceSheetById(@PathVariable String id) {
        return balanceSheetService.getBalanceSheetById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<BalanceSheet>> getBalanceSheetsBySymbol(@PathVariable String symbol) {
        List<BalanceSheet> balanceSheets = balanceSheetService.getBalanceSheetsBySymbol(symbol);
        return ResponseEntity.ok(balanceSheets);
    }

    @GetMapping("/symbol/{symbol}/latest")
    public ResponseEntity<BalanceSheet> getLatestBalanceSheet(@PathVariable String symbol) {
        BalanceSheet latestBalanceSheet = balanceSheetService.getLatestBalanceSheet(symbol);
        return latestBalanceSheet != null ? 
               ResponseEntity.ok(latestBalanceSheet) : 
               ResponseEntity.notFound().build();
    }

    @GetMapping("/symbol/{symbol}/range")
    public ResponseEntity<List<BalanceSheet>> getBalanceSheetsByDateRange(
            @PathVariable String symbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<BalanceSheet> balanceSheets = balanceSheetService.getBalanceSheetsBySymbolAndDateRange(symbol, startDate, endDate);
        return ResponseEntity.ok(balanceSheets);
    }

    @PostMapping
    public ResponseEntity<BalanceSheet> createBalanceSheet(@RequestBody BalanceSheet balanceSheet) {
        BalanceSheet savedBalanceSheet = balanceSheetService.saveBalanceSheet(balanceSheet);
        return ResponseEntity.ok(savedBalanceSheet);
    }

    @DeleteMapping("/id/{id}")
    public ResponseEntity<Void> deleteBalanceSheet(@PathVariable String id) {
        balanceSheetService.deleteBalanceSheet(id);
        return ResponseEntity.ok().build();
    }
} 