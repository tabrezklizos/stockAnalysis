package com.tab.StockAnalysis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "key_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyStatistics {
    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDate date;

    // Valuation Measures
    private BigDecimal marketCap;
    private BigDecimal enterpriseValue;
    private BigDecimal trailingPE;
    private BigDecimal forwardPE;
    private BigDecimal priceToSales;
    private BigDecimal priceToBook;
    private BigDecimal enterpriseToRevenue;
    private BigDecimal enterpriseToEbitda;

    // Financial Metrics
    private BigDecimal profitMargin;
    private BigDecimal operatingMargin;
    private BigDecimal returnOnAssets;
    private BigDecimal returnOnEquity;
    private BigDecimal quarterlyRevenueGrowth;
    private BigDecimal quarterlyEarningsGrowth;

    // Balance Sheet Metrics
    private BigDecimal totalCash;
    private BigDecimal totalDebt;
    private BigDecimal debtToEquity;
    private BigDecimal currentRatio;
    private BigDecimal quickRatio;

    // Trading Information
    private Long sharesOutstanding;
    private Long floatShares;
    private BigDecimal beta;
    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;
    private BigDecimal fiftyDayMovingAverage;
    private BigDecimal twoHundredDayMovingAverage;

    // Share Statistics
    private BigDecimal averageVolume;
    private BigDecimal averageVolume10Day;
    private BigDecimal shortRatio;
    private BigDecimal shortPercentOfFloat;

    // Dividends & Splits
    private BigDecimal dividendRate;
    private BigDecimal dividendYield;
    private LocalDate exDividendDate;
    private BigDecimal lastSplitFactor;
    private LocalDate lastSplitDate;
} 