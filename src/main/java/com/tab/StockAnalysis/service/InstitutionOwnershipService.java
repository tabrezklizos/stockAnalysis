// src/main/java/com/tab/StockAnalysis/service/InstitutionOwnershipService.java
package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.InstitutionOwnership; // [cite: 11]
import com.tab.StockAnalysis.repository.InstitutionOwnershipRepository; // [cite: 11]
import lombok.RequiredArgsConstructor; // [cite: 11]
import lombok.extern.slf4j.Slf4j; // [cite: 11]
import org.springframework.data.redis.core.RedisTemplate; // [cite: 11]
import org.springframework.scheduling.annotation.Scheduled; // [cite: 11]
import org.springframework.stereotype.Service; // [cite: 11]
import org.springframework.web.client.RestTemplate; // [cite: 11]
import org.springframework.http.HttpHeaders; // [cite: 11]
import org.springframework.http.HttpEntity; // [cite: 11]
import org.springframework.http.HttpMethod; // [cite: 12]
import org.springframework.http.ResponseEntity; // [cite: 12]

import java.math.BigDecimal; // [cite: 12]
import java.time.LocalDate; // [cite: 12]
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList; // [cite: 12]
import java.util.List; // [cite: 12]
import java.util.Optional; // [cite: 12]
import java.util.concurrent.TimeUnit; // [cite: 12]

@Slf4j // [cite: 13]
@Service // [cite: 13]
@RequiredArgsConstructor // [cite: 13]
public class InstitutionOwnershipService {

    private final InstitutionOwnershipRepository institutionOwnershipRepository; // Inject the new repository
    private final RedisTemplate<String, Object> redisTemplate; // [cite: 13]
    private final RestTemplate restTemplate; // [cite: 13]
    private final ObjectMapper objectMapper; // [cite: 14]

    private static final String REDIS_KEY_PREFIX = "institutional_ownership:"; // [cite: 14]
    private static final String YAHOO_FINANCE_API_BASE_URL = "https://query1.finance.yahoo.com/v10/finance/quoteSummary/AAPL?modules=price,financialData"; // Base URL for quoteSummary endpoint

    private static final long CACHE_DURATION_HOURS = 24; // Cache for 24 hours [cite: 15]

    // Rate limiting constants [cite: 15]
    private static final int MAX_RETRIES = 3; // [cite: 15]
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds [cite: 16]

    public Optional<InstitutionOwnership> getInstitutionalOwnershipBySymbol(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol + ":latest"; // [cite: 18] - Similar Redis key strategy
        Object cachedData = redisTemplate.opsForValue().get(redisKey); // [cite: 19]

        if (cachedData != null && cachedData instanceof InstitutionOwnership) { // [cite: 20]
            log.info("Retrieved latest institutional ownership for {} from Redis cache", symbol); // [cite: 20]
            return Optional.of((InstitutionOwnership) cachedData); // [cite: 21]
        }

        // Try to get from MongoDB
        Optional<InstitutionOwnership> latestOwnership = institutionOwnershipRepository.findFirstBySymbolOrderByReportDateDesc(symbol); // [cite: 68] - Adapting from BalanceSheet
        if (latestOwnership.isPresent()) { // [cite: 69]
            log.info("Retrieved latest institutional ownership for {} from MongoDB", symbol); // [cite: 70]
            redisTemplate.opsForValue().set(redisKey, latestOwnership.get(), CACHE_DURATION_HOURS, TimeUnit.HOURS); // [cite: 72]
            return latestOwnership;
        }

        // If not in cache or DB, fetch from external API
        log.info("Fetching institutional ownership for {} from Yahoo Finance", symbol); // [cite: 23]
        List<InstitutionOwnership> fetchedOwnerships = fetchAndSaveInstitutionalOwnership(symbol);
        if (!fetchedOwnerships.isEmpty()) {
            // Assuming the first one is the latest if ordered correctly by API
            InstitutionOwnership latest = fetchedOwnerships.get(0); // [cite: 71] - Adapting from BalanceSheet
            redisTemplate.opsForValue().set(redisKey, latest, CACHE_DURATION_HOURS, TimeUnit.HOURS); // [cite: 72]
            return Optional.of(latest);
        }
        return Optional.empty(); // If nothing found after all attempts
    }

    private List<InstitutionOwnership> fetchAndSaveInstitutionalOwnership(String symbol) {
        int retryCount = 0; // [cite: 24]
        while (retryCount < MAX_RETRIES) { // [cite: 25]
            try {
                log.info("Fetching institutional ownership data from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1); // [cite: 25]
                HttpHeaders headers = new HttpHeaders(); // [cite: 26]
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"); // [cite: 26]
                HttpEntity<String> entity = new HttpEntity<>(headers); // [cite: 27]

                // Yahoo Finance API endpoint for institutional ownership
                // The modules parameter is key: "institutionOwnership", "fundOwnership", "majorHoldersBreakdown"
                String url = YAHOO_FINANCE_API_BASE_URL + symbol + "?modules=institutionOwnership,majorHoldersBreakdown"; // Use quoteSummary endpoint for this data
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class); // [cite: 28]

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) { // [cite: 29]
                    JsonNode root = objectMapper.readTree(response.getBody()); // [cite: 29]
                    JsonNode result = root.path("quoteSummary").path("result").get(0);

                    // Parse Institutional Ownership data
                    List<InstitutionOwnership> ownershipData = parseInstitutionalOwnership(result, symbol);

                    // Save to MongoDB
                    log.info("Saving institutional ownership to MongoDB for symbol: {}", symbol); // [cite: 31]
                    List<InstitutionOwnership> savedOwnerships = institutionOwnershipRepository.saveAll(ownershipData); // [cite: 32]

                    // Cache in Redis (cache all if multiple, or just the latest if only one needed)
                    if (!savedOwnerships.isEmpty()) {
                        log.info("Caching institutional ownership in Redis for symbol: {}", symbol); // [cite: 33]
                        // You might want to cache the list for future range queries, or just the latest
                        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + symbol, savedOwnerships, CACHE_DURATION_HOURS, TimeUnit.HOURS); // [cite: 33]
                        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + symbol + ":latest", savedOwnerships.get(0), CACHE_DURATION_HOURS, TimeUnit.HOURS); // Cache latest
                    }

                    return savedOwnerships;
                }
            } catch (Exception e) { // [cite: 33]
                log.error("Error fetching institutional ownership data for symbol: " + symbol, e); // [cite: 34]
                retryCount++; // [cite: 34]
                if (retryCount < MAX_RETRIES) { // [cite: 34]
                    try {
                        Thread.sleep(RETRY_DELAY_MS); // [cite: 35]
                    } catch (InterruptedException ie) { // [cite: 35]
                        Thread.currentThread().interrupt(); // [cite: 35]
                        throw new RuntimeException("Interrupted while waiting to retry", ie); // [cite: 36]
                    }
                }
            }
        }
        throw new RuntimeException("Failed to fetch institutional ownership data after " + MAX_RETRIES + " attempts"); // [cite: 37]
    }

    private List<InstitutionOwnership> parseInstitutionalOwnership(JsonNode resultNode, String symbol) {
        List<InstitutionOwnership> ownershipList = new ArrayList<>(); // [cite: 37]

        // Extracting data from "institutionOwnership" module
        JsonNode institutionOwnershipNode = resultNode.path("institutionOwnership").path("holders").get(0); // Often an array, get the first one for summary

        if (institutionOwnershipNode != null && !institutionOwnershipNode.isMissingNode()) {
            InstitutionOwnership ownership = new InstitutionOwnership(); // [cite: 39]
            ownership.setSymbol(symbol); // [cite: 39]

            // Yahoo Finance's timestamp for institutionOwnership is usually a Unix timestamp for the filing date
            JsonNode reportDateNode = institutionOwnershipNode.path("reportDate");
            if (reportDateNode != null && reportDateNode.path("raw") != null) {
                long timestamp = reportDateNode.path("raw").asLong(); // Timestamp is usually in seconds
                ownership.setReportDate(LocalDate.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())); // Convert seconds to LocalDate
            } else {
                ownership.setReportDate(LocalDate.now()); // Fallback if reportDate is missing
            }


            // Extract total shares held by institutions
            JsonNode totalSharesNode = resultNode.path("majorHoldersBreakdown").path("totalInstitutionalHolding");
            if (totalSharesNode != null && totalSharesNode.path("raw") != null) {
                ownership.setTotalSharesHeld(totalSharesNode.path("raw").asLong());
            }

            // Extract percentage of outstanding shares held by institutions
            JsonNode percentageOutstandingNode = resultNode.path("majorHoldersBreakdown").path("institutionalPercentHeld");
            if (percentageOutstandingNode != null && percentageOutstandingNode.path("raw") != null) {
                ownership.setPercentageOfOutstandingShares(BigDecimal.valueOf(percentageOutstandingNode.path("raw").asDouble() * 100)); // Convert to percentage
            }


            // You might need to iterate through 'holders' array if you want individual top holders
            // For simplicity, let's just get the first one if available
            JsonNode topHolderDetailsNode = institutionOwnershipNode.path("holders").get(0);
            if (topHolderDetailsNode != null && !topHolderDetailsNode.isMissingNode()) {
                ownership.setTopHolderName(topHolderDetailsNode.path("holder").asText());
                if (topHolderDetailsNode.path("shares").path("raw") != null) {
                    ownership.setTopHolderShares(topHolderDetailsNode.path("shares").path("raw").asLong());
                }
                if (topHolderDetailsNode.path("pctHeld").path("raw") != null) {
                    ownership.setTopHolderPercentage(BigDecimal.valueOf(topHolderDetailsNode.path("pctHeld").path("raw").asDouble() * 100));
                }
            }

            ownershipList.add(ownership); // [cite: 48]
        }
        return ownershipList; // [cite: 49]
    }

    // You might want a scheduled update for institutional ownership,
    // though 13F filings are quarterly, so daily might be too frequent for this data.
    // Consider a weekly or monthly update if real-time tracking of institutional positions isn't critical.
    // @Scheduled(cron = "0 0 2 * * MON") // Example: Run at 2 AM every Monday
    // public void updateInstitutionalOwnership() {
    //     log.info("Starting scheduled institutional ownership update");
    //     institutionOwnershipRepository.findAll().forEach(ownership -> {
    //         try {
    //             fetchAndSaveInstitutionalOwnership(ownership.getSymbol());
    //         } catch (Exception e) {
    //             log.error("Error updating institutional ownership for symbol: " + ownership.getSymbol(), e);
    //         }
    //     });
    // }

    public InstitutionOwnership saveInstitutionalOwnership(InstitutionOwnership ownership) {
        InstitutionOwnership saved = institutionOwnershipRepository.save(ownership); // [cite: 52]
        String redisKey = REDIS_KEY_PREFIX + saved.getSymbol() + ":latest"; // [cite: 53]
        redisTemplate.opsForValue().set(redisKey, saved, CACHE_DURATION_HOURS, TimeUnit.HOURS); // [cite: 53]
        return saved; // [cite: 54]
    }

    public void deleteInstitutionalOwnership(String id) {
        Optional<InstitutionOwnership> ownership = institutionOwnershipRepository.findById(id); // [cite: 54]
        ownership.ifPresent(o -> { // [cite: 55]
            institutionOwnershipRepository.deleteById(id); // [cite: 55]
            String redisKey = REDIS_KEY_PREFIX + o.getSymbol() + ":latest"; // [cite: 55]
            redisTemplate.delete(redisKey); // [cite: 55]
        });
    }
}