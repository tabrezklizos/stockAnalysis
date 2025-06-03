package com.tab.StockAnalysis.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "market_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MarketData implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDateTime timestamp;

    // Price Information
    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal open;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private BigDecimal fiftyTwoWeekHigh;
    private BigDecimal fiftyTwoWeekLow;

    // Volume and Trading Information
    private Long volume;
    private Long averageVolume;
    private Long averageVolume10Day;
    private BigDecimal marketCap;

    // Key Statistics
    private BigDecimal beta;
    private BigDecimal peRatio;
    private BigDecimal forwardPE;
    private BigDecimal eps;
    private BigDecimal priceToBook;
    private Long sharesOutstanding;
    private Long floatShares;

    // Moving Averages
    private BigDecimal fiftyDayAverage;
    private BigDecimal twoHundredDayAverage;
    private BigDecimal fiftyDayAverageChange;
    private BigDecimal twoHundredDayAverageChange;

    // Additional Metrics
    private BigDecimal dividendYield;
    private BigDecimal dividendRate;
    private LocalDateTime exDividendDate;
    private BigDecimal trailingAnnualDividendRate;
    private BigDecimal trailingAnnualDividendYield;

    // Volatility Metrics
    private BigDecimal dayChange;
    private BigDecimal dayChangePercent;
    private BigDecimal weekChange;
    private BigDecimal weekChangePercent;
    private BigDecimal monthChange;
    private BigDecimal monthChangePercent;

    // Trading Status
    private String marketState; // PRE, REGULAR, POST, CLOSED
    private Boolean isTrading;
    private String exchange;
    private String currency;

    // Last Update Information
    private LocalDateTime lastUpdated;
} 