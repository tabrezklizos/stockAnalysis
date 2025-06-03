package com.tab.StockAnalysis.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Document(collection = "cash_flows")
public class CashFlow {
    @Id
    private String id;
    private String symbol;
    private LocalDate date;
    private String fiscalYear;
    private String fiscalQuarter;
    private String reportingCurrency;

    // Operating Activities
    private BigDecimal netIncome;
    private BigDecimal operatingCashFlow;
    private BigDecimal depreciationAndAmortization;
    private BigDecimal changeInWorkingCapital;
    private BigDecimal changeInAccountsReceivable;
    private BigDecimal changeInInventory;
    private BigDecimal changeInAccountsPayable;
    private BigDecimal changeInOtherOperatingActivities;

    // Investing Activities
    private BigDecimal capitalExpenditures;
    private BigDecimal investments;
    private BigDecimal otherInvestingActivities;
    private BigDecimal totalInvestingCashFlows;

    // Financing Activities
    private BigDecimal dividendsPaid;
    private BigDecimal stockRepurchase;
    private BigDecimal debtRepayment;
    private BigDecimal commonStockIssued;
    private BigDecimal totalFinancingCashFlows;

    // Summary
    private BigDecimal freeCashFlow;
    private BigDecimal netChangeInCash;
    private BigDecimal beginningCashPosition;
    private BigDecimal endCashPosition;
} 