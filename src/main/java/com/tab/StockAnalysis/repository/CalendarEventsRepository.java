package com.tab.StockAnalysis.repository;

import com.tab.StockAnalysis.entity.CalendarEvents;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventsRepository extends MongoRepository<CalendarEvents, String> {
    Optional<CalendarEvents> findFirstBySymbolOrderByLastUpdatedDesc(String symbol);
    List<CalendarEvents> findBySymbolOrderByLastUpdatedDesc(String symbol);
    List<CalendarEvents> findByNextEarningsDateBetween(LocalDate startDate, LocalDate endDate);
    List<CalendarEvents> findByNextDividendDateBetween(LocalDate startDate, LocalDate endDate);
    List<CalendarEvents> findByNextSplitDateBetween(LocalDate startDate, LocalDate endDate);
} 