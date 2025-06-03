package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.BalanceSheet;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceSheetRepository extends MongoRepository<BalanceSheet, String> {
    List<BalanceSheet> findAllBySymbol(String symbol);
    List<BalanceSheet> findAllBySymbolOrderByDateDesc(String symbol);
    List<BalanceSheet> findBySymbolAndDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
    Optional<BalanceSheet> findFirstBySymbolOrderByDateDesc(String symbol);
} 