package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.FinancialData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialDataRepository extends MongoRepository<FinancialData, String> {
    Optional<FinancialData> findFirstBySymbolOrderByLastUpdatedDesc(String symbol);
    List<FinancialData> findBySymbolOrderByLastUpdatedDesc(String symbol);
    List<FinancialData> findByMarketCapGreaterThanEqual(BigDecimal marketCap);
    void deleteBySymbol(String symbol);
} 