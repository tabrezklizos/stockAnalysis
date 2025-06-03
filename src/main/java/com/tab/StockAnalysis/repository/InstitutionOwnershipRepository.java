// src/main/java/com/tab/StockAnalysis/repository/InstitutionOwnershipRepository.java
package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.InstitutionOwnership;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository // [cite: 9]
public interface InstitutionOwnershipRepository extends MongoRepository<InstitutionOwnership, String> {
    List<InstitutionOwnership> findAllBySymbolOrderByReportDateDesc(String symbol); // [cite: 9] - Similar to BalanceSheet
    Optional<InstitutionOwnership> findFirstBySymbolOrderByReportDateDesc(String symbol); // [cite: 10] - Similar to BalanceSheet
}