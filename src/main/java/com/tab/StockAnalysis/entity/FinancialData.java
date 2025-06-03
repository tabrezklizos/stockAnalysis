package com.tab.StockAnalysis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "financial_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinancialData {
    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDateTime lastUpdated;

    // Market Data
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    private BigDecimal marketCap;

    // Performance Metrics
    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;
    private BigDecimal fiftyDayAverage;
    private BigDecimal twoHundredDayAverage;

    // Trading Information
    private Long sharesOutstanding;
    private BigDecimal beta;
} 