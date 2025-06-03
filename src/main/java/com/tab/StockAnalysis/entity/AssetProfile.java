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

@Document(collection = "asset_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    @Indexed
    private String symbol;

    // Company Information
    private String companyName;
    private String industry;
    private String sector;
    private String website;
    private String description;
    private String country;
    private String address;
    private String phone;

    // Business Details
    private String businessSummary;
    private Integer fullTimeEmployees;

    // Financial Information
    private BigDecimal marketCap;
    private String financialCurrency;
    private BigDecimal revenueGrowth;
    private BigDecimal grossMargins;
    private BigDecimal operatingMargins;
    private BigDecimal profitMargins;

    // Market Information
    private String exchange;
    private String quoteType;
    private String market;
} 