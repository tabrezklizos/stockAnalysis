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
import java.time.LocalDate;

@Document(collection = "income_statements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomeStatement implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDate date;
    private String period; // "annual" or "quarterly"

    // Revenue Section
    private BigDecimal totalRevenue;
    private BigDecimal costOfRevenue;
    private BigDecimal grossProfit;

    // Operating Expenses
    private BigDecimal researchDevelopment;
    private BigDecimal sellingGeneralAdministrative;
    private BigDecimal totalOperatingExpenses;
    private BigDecimal operatingIncome;

    // Non-Operating Income/Expenses
    private BigDecimal interestExpense;
    private BigDecimal interestIncome;
    private BigDecimal otherIncomeExpense;
    private BigDecimal incomeBeforeTax;

    // Tax and Net Income
    private BigDecimal incomeTaxExpense;
    private BigDecimal netIncome;
    private BigDecimal netIncomeApplicableToCommonShares;

    // Per Share Data
    private BigDecimal basicEPS;
    private BigDecimal dilutedEPS;
    private BigDecimal basicAverageShares;
    private BigDecimal dilutedAverageShares;

    // Additional Metrics
    private BigDecimal ebitda;
    private BigDecimal operatingMargin;
    private BigDecimal profitMargin;

    // Metadata
    private String currency;
    private LocalDate lastUpdated;
} 