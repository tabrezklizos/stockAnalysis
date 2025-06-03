package com.tab.StockAnalysis.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "stock_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockData {
    @Id
    private String id;

    @Indexed(unique = true)
    private String symbol;
    private String companyName;
    private String exchange;

    private BigDecimal currentPrice;
    private BigDecimal previousClose;
    private BigDecimal dayHigh;
    private BigDecimal dayLow;
    private Long volume;
    private BigDecimal marketCap;

    private BigDecimal priceChange;
    private BigDecimal priceChangePercent;

    private BigDecimal fiftyDayAverage;
    private BigDecimal twoHundredDayAverage;
    private BigDecimal yearHigh;
    private BigDecimal yearLow;

    private Long sharesOutstanding;
    private BigDecimal eps;
    private BigDecimal pe;

    private LocalDateTime lastUpdated;
}
