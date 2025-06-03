package com.tab.StockAnalysis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document(collection = "balance_sheets")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheet {
    @Id
    private String id;

    @Indexed
    private String symbol;
    
    @Indexed
    private LocalDate date;
    
    // Assets
    private BigDecimal totalAssets;
    private BigDecimal currentAssets;
    private BigDecimal cashAndCashEquivalents;
    private BigDecimal shortTermInvestments;
    private BigDecimal accountsReceivable;
    private BigDecimal inventory;
    
    // Liabilities
    private BigDecimal totalLiabilities;
    private BigDecimal currentLiabilities;
    private BigDecimal accountsPayable;
    private BigDecimal shortTermDebt;
    private BigDecimal longTermDebt;
    
    // Equity
    private BigDecimal totalShareholderEquity;
    private BigDecimal retainedEarnings;
    private BigDecimal commonStock;
    
    // Additional fields
    private BigDecimal workingCapital;
    private String reportingCurrency;
    private String fiscalYear;
    private String fiscalQuarter;
} 