package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.IncomeStatement;
import com.tab.StockAnalysis.repository.IncomeStatementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IncomeStatementService {

    private final IncomeStatementRepository incomeStatementRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, IncomeStatement> incomeStatementRedisTemplate;

    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String REDIS_KEY_PREFIX = "income_statement:";
    private static final long CACHE_TTL_HOURS = 24;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private LocalDateTime lastUpdateTime;

    @Cacheable(value = "incomeStatements", key = "'all'")
    public List<IncomeStatement> getAllIncomeStatements() {
        return incomeStatementRepository.findAll();
    }

    @Cacheable(value = "incomeStatements", key = "#symbol")
    public List<IncomeStatement> getIncomeStatementsBySymbol(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        
        // Try Redis cache first
        IncomeStatement cachedStatement = incomeStatementRedisTemplate.opsForValue().get(redisKey);
        if (cachedStatement != null) {
            log.info("Retrieved income statement for {} from Redis cache", symbol);
            return Collections.singletonList(cachedStatement);
        }

        // Then try MongoDB
        List<IncomeStatement> statements = incomeStatementRepository.findBySymbol(symbol);
        if (!statements.isEmpty()) {
            log.info("Retrieved income statements for {} from MongoDB", symbol);
            cacheIncomeStatement(symbol, statements);
            return statements;
        }

        // If not in database, fetch from Yahoo Finance API
        log.info("Fetching income statements for {} from Yahoo Finance", symbol);
        statements = fetchAndSaveIncomeStatements(symbol);
        if (!statements.isEmpty()) {
            cacheIncomeStatement(symbol, statements);
        }
        return statements;
    }

    public List<IncomeStatement> getIncomeStatementsBySymbolAndPeriod(String symbol, String period) {
        return incomeStatementRepository.findBySymbolAndPeriod(symbol, period);
    }

    public List<IncomeStatement> getIncomeStatementsBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        return incomeStatementRepository.findBySymbolAndDateBetween(symbol, startDate, endDate);
    }

    private List<IncomeStatement> fetchAndSaveIncomeStatements(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching income statement data from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = YAHOO_FINANCE_URL + symbol + "?interval=3mo&range=5y";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode timestamps = result.path("timestamp");
                    JsonNode indicators = result.path("indicators");
                    JsonNode quote = indicators.path("quote").get(0);

                    List<IncomeStatement> statements = parseIncomeStatements(timestamps, quote, symbol);
                    return incomeStatementRepository.saveAll(statements);
                }
            } catch (Exception e) {
                log.error("Error fetching income statement data for symbol: " + symbol, e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Failed to fetch income statement data after " + MAX_RETRIES + " attempts");
    }

    private List<IncomeStatement> parseIncomeStatements(JsonNode timestamps, JsonNode quote, String symbol) {
        List<IncomeStatement> statements = new ArrayList<>();
        
        for (int i = 0; i < timestamps.size(); i++) {
            if (quote.path("close").path(i).isNull()) continue;

            IncomeStatement statement = new IncomeStatement();
            statement.setSymbol(symbol);
            statement.setDate(LocalDate.now()); // You should parse the actual date from timestamps
            statement.setPeriod("quarterly"); // Set based on the data

            // Set financial data from quote
            BigDecimal revenue = BigDecimal.valueOf(quote.path("close").path(i).asDouble() * 
                                                  quote.path("volume").path(i).asDouble());
            
            // Set the parsed values
            statement.setTotalRevenue(revenue);
            statement.setCostOfRevenue(revenue.multiply(BigDecimal.valueOf(0.7))); // Example calculation
            statement.setGrossProfit(revenue.multiply(BigDecimal.valueOf(0.3)));
            
            statement.setResearchDevelopment(revenue.multiply(BigDecimal.valueOf(0.15)));
            statement.setSellingGeneralAdministrative(revenue.multiply(BigDecimal.valueOf(0.1)));
            statement.setTotalOperatingExpenses(revenue.multiply(BigDecimal.valueOf(0.25)));
            statement.setOperatingIncome(revenue.multiply(BigDecimal.valueOf(0.05)));
            
            statement.setInterestExpense(revenue.multiply(BigDecimal.valueOf(0.01)));
            statement.setInterestIncome(revenue.multiply(BigDecimal.valueOf(0.005)));
            statement.setOtherIncomeExpense(revenue.multiply(BigDecimal.valueOf(-0.002)));
            statement.setIncomeBeforeTax(revenue.multiply(BigDecimal.valueOf(0.043)));
            
            statement.setIncomeTaxExpense(revenue.multiply(BigDecimal.valueOf(0.01)));
            statement.setNetIncome(revenue.multiply(BigDecimal.valueOf(0.033)));
            statement.setNetIncomeApplicableToCommonShares(revenue.multiply(BigDecimal.valueOf(0.033)));
            
            // Calculate per share metrics
            BigDecimal sharesOutstanding = BigDecimal.valueOf(1_000_000_000); // Example value
            statement.setBasicEPS(statement.getNetIncome().divide(sharesOutstanding, 4, BigDecimal.ROUND_HALF_UP));
            statement.setDilutedEPS(statement.getBasicEPS().multiply(BigDecimal.valueOf(0.98)));
            statement.setBasicAverageShares(sharesOutstanding);
            statement.setDilutedAverageShares(sharesOutstanding.multiply(BigDecimal.valueOf(1.02)));
            
            // Set additional metrics
            statement.setEbitda(revenue.multiply(BigDecimal.valueOf(0.08)));
            statement.setOperatingMargin(BigDecimal.valueOf(0.05));
            statement.setProfitMargin(BigDecimal.valueOf(0.033));
            
            // Set metadata
            statement.setCurrency("USD");
            statement.setLastUpdated(LocalDate.now());
            
            statements.add(statement);
        }
        
        return statements;
    }

    private void cacheIncomeStatement(String symbol, List<IncomeStatement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        String redisKey = REDIS_KEY_PREFIX + symbol;
        // Store only the most recent statement
        IncomeStatement mostRecentStatement = statements.get(0);
        incomeStatementRedisTemplate.opsForValue().set(redisKey, mostRecentStatement, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.info("Cached most recent income statement for symbol {} in Redis with TTL {} hours", symbol, CACHE_TTL_HOURS);
    }

    @CacheEvict(value = "incomeStatements", key = "#symbol")
    public void evictCache(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        incomeStatementRedisTemplate.delete(redisKey);
        log.info("Evicted cache for symbol: {}", symbol);
    }

    @Scheduled(cron = "0 0 2 * * ?") // Run at 2 AM every day
    @CacheEvict(value = "incomeStatements", allEntries = true)
    public void scheduledUpdateIncomeStatements() {
        log.info("Starting scheduled income statement update at {}", LocalDateTime.now());
        updateAllIncomeStatements();
    }

    public void updateAllIncomeStatements() {
        int successCount = 0;
        int failureCount = 0;
        List<String> failedSymbols = new ArrayList<>();

        // Get unique symbols to update
        Set<String> uniqueSymbols = new HashSet<>();
        incomeStatementRepository.findAll().forEach(statement -> uniqueSymbols.add(statement.getSymbol()));
        
        int totalSymbols = uniqueSymbols.size();
        log.info("Found {} unique symbols to update", totalSymbols);

        for (String symbol : uniqueSymbols) {
            try {
                log.info("Updating income statements for symbol: {}", symbol);
                evictCache(symbol);
                
                List<IncomeStatement> updatedStatements = fetchAndSaveIncomeStatements(symbol);
                if (!updatedStatements.isEmpty()) {
                    successCount++;
                    cacheIncomeStatement(symbol, updatedStatements);
                    log.info("Successfully updated {} income statements for symbol: {}", updatedStatements.size(), symbol);
                } else {
                    failureCount++;
                    failedSymbols.add(symbol);
                    log.error("No income statements fetched for symbol: {}", symbol);
                }
                Thread.sleep(500); // Rate limiting
            } catch (Exception e) {
                failureCount++;
                failedSymbols.add(symbol);
                log.error("Error updating income statements for symbol: " + symbol, e);
            }
        }

        lastUpdateTime = LocalDateTime.now();
        log.info("Income statement update completed at {}", lastUpdateTime);
        log.info("Update summary - Success: {}, Failures: {}, Total: {}", successCount, failureCount, totalSymbols);
        if (!failedSymbols.isEmpty()) {
            log.info("Failed symbols: {}", String.join(", ", failedSymbols));
        }
    }

    public Map<String, Object> getUpdateStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastUpdateTime", lastUpdateTime);
        status.put("totalStatements", incomeStatementRepository.count());
        status.put("uniqueSymbols", incomeStatementRepository.findAll()
            .stream()
            .map(IncomeStatement::getSymbol)
            .distinct()
            .count());
        return status;
    }

    public Map<String, Object> triggerManualUpdate() {
        log.info("Manual update triggered at {}", LocalDateTime.now());
        updateAllIncomeStatements();
        return getUpdateStatus();
    }
} 