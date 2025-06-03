package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.MarketData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketDataRepository extends MongoRepository<MarketData, String> {
    Optional<MarketData> findFirstBySymbolOrderByTimestampDesc(String symbol);
    List<MarketData> findBySymbolOrderByTimestampDesc(String symbol);
    List<MarketData> findBySymbolAndTimestampBetweenOrderByTimestampDesc(String symbol, LocalDateTime startTime, LocalDateTime endTime);
    List<MarketData> findByMarketCapGreaterThanEqual(Double marketCap);
    List<MarketData> findByVolumeGreaterThanEqual(Long volume);
    List<MarketData> findByExchange(String exchange);
    void deleteBySymbol(String symbol);
} 