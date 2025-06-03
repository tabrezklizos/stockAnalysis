package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.IncomeStatement;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface IncomeStatementRepository extends MongoRepository<IncomeStatement, String> {
    List<IncomeStatement> findBySymbol(String symbol);
    List<IncomeStatement> findBySymbolAndPeriod(String symbol, String period);
    List<IncomeStatement> findBySymbolAndDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
    List<IncomeStatement> findBySymbolAndPeriodAndDateBetween(String symbol, String period, LocalDate startDate, LocalDate endDate);
    void deleteBySymbol(String symbol);
} 