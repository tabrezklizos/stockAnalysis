package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.CashFlow;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CashFlowRepository extends MongoRepository<CashFlow, String> {
    List<CashFlow> findAllBySymbol(String symbol);
    List<CashFlow> findAllBySymbolOrderByDateDesc(String symbol);
    List<CashFlow> findBySymbolAndDateBetween(String symbol, LocalDate startDate, LocalDate endDate);
    Optional<CashFlow> findFirstBySymbolOrderByDateDesc(String symbol);
} 