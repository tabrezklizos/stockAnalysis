package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.KeyStatistics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface KeyStatisticsRepository extends MongoRepository<KeyStatistics, String> {
    Optional<KeyStatistics> findFirstBySymbolOrderByDateDesc(String symbol);
    List<KeyStatistics> findBySymbolOrderByDateDesc(String symbol);
    List<KeyStatistics> findBySymbolAndDateBetweenOrderByDateDesc(String symbol, LocalDate startDate, LocalDate endDate);
} 