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
import java.util.List;

@Document(collection = "earnings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Earnings {
    @Id
    private String id;

    @Indexed
    private String symbol;
    private LocalDateTime lastUpdated;

    // Current Quarter Estimates
    private String currentQuarter;
    private LocalDate currentQuarterDate;
    private BigDecimal currentQuarterEstimateEps;
    private BigDecimal currentQuarterEstimateRevenue;
    private Integer numberOfAnalysts;
    private BigDecimal estimateGrowth;

    // Next Quarter Estimates
    private String nextQuarter;
    private LocalDate nextQuarterDate;
    private BigDecimal nextQuarterEstimateEps;
    private BigDecimal nextQuarterEstimateRevenue;
    private Integer nextQuarterNumberOfAnalysts;

    // Current Year Estimates
    private String currentYear;
    private BigDecimal currentYearEstimateEps;
    private BigDecimal currentYearEstimateRevenue;
    private Integer currentYearNumberOfAnalysts;

    // Next Year Estimates
    private String nextYear;
    private BigDecimal nextYearEstimateEps;
    private BigDecimal nextYearEstimateRevenue;
    private Integer nextYearNumberOfAnalysts;

    // Historical Quarterly Results
    private List<QuarterlyEarnings> quarterlyEarnings;

    // Historical Annual Results
    private List<AnnualEarnings> annualEarnings;

    // Earnings Growth
    private BigDecimal quarterlyGrowth;
    private BigDecimal yearlyGrowth;
    private BigDecimal fiveYearGrowthRate;

    // Earnings Quality Metrics
    private BigDecimal earningsQualityScore;
    private BigDecimal earningsConsistencyScore;
    private BigDecimal earningsSurpriseScore;

    // Revision Metrics
    private Integer upwardRevisions30Days;
    private Integer downwardRevisions30Days;
    private Integer upwardRevisions90Days;
    private Integer downwardRevisions90Days;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuarterlyEarnings {
        private String quarter;
        private LocalDate date;
        private BigDecimal reportedEps;
        private BigDecimal estimatedEps;
        private BigDecimal surprise;
        private BigDecimal surprisePercentage;
        private BigDecimal reportedRevenue;
        private BigDecimal estimatedRevenue;
        private BigDecimal revenueSurprise;
        private BigDecimal revenueSurprisePercentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnnualEarnings {
        private String fiscalYear;
        private LocalDate date;
        private BigDecimal reportedEps;
        private BigDecimal estimatedEps;
        private BigDecimal surprise;
        private BigDecimal surprisePercentage;
        private BigDecimal reportedRevenue;
        private BigDecimal estimatedRevenue;
        private BigDecimal revenueSurprise;
        private BigDecimal revenueSurprisePercentage;
    }
} 