package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.StockData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends MongoRepository<StockData, String> {
    List<StockData> findAllBySymbol(String symbol);
    Optional<StockData> findFirstBySymbolOrderByLastUpdatedDesc(String symbol);
    void deleteBySymbol(String symbol);
}
