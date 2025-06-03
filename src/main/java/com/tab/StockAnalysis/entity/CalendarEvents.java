package com.tab.StockAnalysis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Document(collection = "calendar_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarEvents {
    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDateTime lastUpdated;

    // Earnings Information
    private LocalDate nextEarningsDate;
    private String nextEarningsQuarter;
    private BigDecimal earningsEstimate;
    private BigDecimal revenueEstimate;
    private Integer numberOfAnalysts;
    private LocalDate lastEarningsDate;
    private BigDecimal lastEarningsEps;
    private BigDecimal lastEarningsEpsEstimate;
    private BigDecimal lastEarningsEpsSurprise;
    private BigDecimal lastEarningsRevenue;
    private BigDecimal lastEarningsRevenueEstimate;
    private BigDecimal lastEarningsRevenueSurprise;

    // Dividend Information
    private LocalDate nextDividendDate;
    private LocalDate exDividendDate;
    private BigDecimal dividendAmount;
    private BigDecimal dividendYield;
    private String dividendFrequency;
    private LocalDate lastDividendDate;
    private BigDecimal lastDividendAmount;

    // Stock Split Information
    private LocalDate nextSplitDate;
    private String nextSplitRatio;
    private LocalDate lastSplitDate;
    private String lastSplitRatio;

    // Shareholder Meeting
    private LocalDate nextShareholderMeetingDate;
    private String nextShareholderMeetingType; // Annual or Special

    // Fiscal Period
    private String fiscalYearEnd;
    private String mostRecentQuarter;
} 