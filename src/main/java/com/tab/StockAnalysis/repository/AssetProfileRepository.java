package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.AssetProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetProfileRepository extends MongoRepository<AssetProfile, String> {
    List<AssetProfile> findAllBySymbol(String symbol);
    List<AssetProfile> findAllBySymbolOrderByIdDesc(String symbol);
    List<AssetProfile> findAllByIndustry(String industry);
    List<AssetProfile> findAllBySector(String sector);
    List<AssetProfile> findByMarketCapGreaterThanEqual(double marketCap);
    List<AssetProfile> findByCountry(String country);
    Optional<AssetProfile> findFirstBySymbolOrderByIdDesc(String symbol);
} 