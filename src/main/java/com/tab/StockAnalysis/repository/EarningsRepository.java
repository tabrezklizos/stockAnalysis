package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.Earnings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EarningsRepository extends MongoRepository<Earnings, String> {
    Optional<Earnings> findFirstBySymbolOrderByLastUpdatedDesc(String symbol);
    List<Earnings> findBySymbolOrderByLastUpdatedDesc(String symbol);
    List<Earnings> findByCurrentQuarterDateBetween(LocalDate startDate, LocalDate endDate);
    List<Earnings> findByEstimateGrowthGreaterThan(BigDecimal growth);
    List<Earnings> findByEarningsSurpriseScoreGreaterThan(BigDecimal score);
} 