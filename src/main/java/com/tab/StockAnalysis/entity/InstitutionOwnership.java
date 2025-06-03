// src/main/java/com/tab/StockAnalysis/entity/InstitutionOwnership.java
package com.tab.StockAnalysis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.math.BigDecimal; // For percentages or share counts if needed

@Document(collection = "institutional_ownership") // [cite: 2] - Similar to BalanceSheet
@Data // [cite: 2]
@NoArgsConstructor // [cite: 2]
@AllArgsConstructor // [cite: 2]
public class InstitutionOwnership {

    @Id // [cite: 2]
    private String id;

    @Indexed // [cite: 3]
    private String symbol; // Ticker symbol (e.g., AAPL) [cite: 3]

    @Indexed // [cite: 3]
    private LocalDate reportDate; // The date of the 13F report, usually quarterly [cite: 3]

    private Long totalSharesHeld; // Total shares held by institutions
    private BigDecimal totalValueHeld; // Total value of shares held by institutions
    private BigDecimal percentageOfOutstandingShares; // % of outstanding shares held by institutions

    // Fields for individual top institutional holders (can be a list of sub-objects or separate entity)
    // For simplicity, here's an example for the top holder. You might want a List<HolderDetail>
    private String topHolderName;
    private Long topHolderShares;
    private BigDecimal topHolderPercentage; // % of company's shares held by this institution

    // You can add more fields if available from the API:
    // private Long sharesBought;
    // private Long sharesSold;
    // private Integer numberOfFunds;
    // private LocalDate latestFilingDate;
}