package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.BalanceSheet;
import com.tab.StockAnalysis.repository.BalanceSheetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import yahoofinance.YahooFinance;
import yahoofinance.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceSheetService {

    private final BalanceSheetRepository balanceSheetRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "balance_sheet:";
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final long CACHE_DURATION_HOURS = 24; // Cache for 24 hours

    // Rate limiting constants
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds



    public List<BalanceSheet> getAllBalanceSheets() {
        return balanceSheetRepository.findAll();
    }

    public Optional<BalanceSheet> getBalanceSheetById(String id) {
        return balanceSheetRepository.findById(id);
    }

    public List<BalanceSheet> getBalanceSheetsBySymbol(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        
        // Try to get from Redis first
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null && cachedData instanceof List<?>) {
            log.info("Retrieved balance sheets for {} from Redis cache", symbol);
            return (List<BalanceSheet>) cachedData;
        }

        // If not in Redis, get from MongoDB
        List<BalanceSheet> balanceSheets = balanceSheetRepository.findAllBySymbolOrderByDateDesc(symbol);
        if (!balanceSheets.isEmpty()) {
            // Cache in Redis
            log.info("Retrieved balance sheets for {} from MongoDB", symbol);
            redisTemplate.opsForValue().set(redisKey, balanceSheets, CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return balanceSheets;
        }

        // If not in cache, fetch from external API
        log.info("Fetching balance sheets for {} from Yahoo Finance", symbol);
        return fetchAndSaveBalanceSheets(symbol);
    }

    private List<BalanceSheet> fetchAndSaveBalanceSheets(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching data from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Yahoo Finance API endpoint with parameters for quarterly data
                String url = YAHOO_FINANCE_URL + symbol + "?interval=3mo&range=5y"; // 5 years of quarterly data
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode timestamps = result.path("timestamp");
                    JsonNode indicators = result.path("indicators");
                    JsonNode quote = indicators.path("quote").get(0);

                    List<BalanceSheet> sheets = parseBalanceSheets(timestamps, quote, symbol);

                    // Save to MongoDB
                    log.info("Saving balance sheets to MongoDB for symbol: {}", symbol);
                    sheets = balanceSheetRepository.saveAll(sheets);

                    // Cache in Redis
                    log.info("Caching balance sheets in Redis for symbol: {}", symbol);
                    redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + symbol, sheets, CACHE_DURATION_HOURS, TimeUnit.HOURS);

                    return sheets;
                }
            } catch (Exception e) {
                log.error("Error fetching balance sheet data for symbol: " + symbol, e);
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
        throw new RuntimeException("Failed to fetch balance sheet data after " + MAX_RETRIES + " attempts");
    }

    private List<BalanceSheet> parseBalanceSheets(JsonNode timestamps, JsonNode quote, String symbol) {
        List<BalanceSheet> balanceSheets = new ArrayList<>();
        
        for (int i = 0; i < timestamps.size(); i++) {
            if (quote.path("close").path(i).isNull()) continue;

            BalanceSheet balanceSheet = new BalanceSheet();
            balanceSheet.setSymbol(symbol);
            
            // Convert timestamp to LocalDate
            long timestamp = timestamps.get(i).asLong() * 1000; // Convert to milliseconds
            balanceSheet.setDate(LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp), 
                java.time.ZoneId.systemDefault()
            ));

            // Set financial data from quote
            BigDecimal close = BigDecimal.valueOf(quote.path("close").path(i).asDouble());
            BigDecimal volume = BigDecimal.valueOf(quote.path("volume").path(i).asDouble());
            BigDecimal high = BigDecimal.valueOf(quote.path("high").path(i).asDouble());
            BigDecimal low = BigDecimal.valueOf(quote.path("low").path(i).asDouble());

            // Use market data to estimate balance sheet items
            balanceSheet.setTotalAssets(close.multiply(volume));
            balanceSheet.setCurrentAssets(close.multiply(BigDecimal.valueOf(0.3))); // Estimate 30% as current assets
            balanceSheet.setCashAndCashEquivalents(close.multiply(BigDecimal.valueOf(0.1))); // Estimate 10% as cash
            balanceSheet.setShortTermInvestments(close.multiply(BigDecimal.valueOf(0.1)));
            balanceSheet.setAccountsReceivable(close.multiply(BigDecimal.valueOf(0.05)));
            balanceSheet.setInventory(close.multiply(BigDecimal.valueOf(0.05)));

            balanceSheet.setTotalLiabilities(close.multiply(BigDecimal.valueOf(0.6))); // Estimate 60% as liabilities
            balanceSheet.setCurrentLiabilities(close.multiply(BigDecimal.valueOf(0.2)));
            balanceSheet.setAccountsPayable(close.multiply(BigDecimal.valueOf(0.05)));
            balanceSheet.setShortTermDebt(close.multiply(BigDecimal.valueOf(0.1)));
            balanceSheet.setLongTermDebt(close.multiply(BigDecimal.valueOf(0.4)));

            balanceSheet.setTotalShareholderEquity(close.multiply(BigDecimal.valueOf(0.4))); // Estimate 40% as equity
            balanceSheet.setRetainedEarnings(close.multiply(BigDecimal.valueOf(0.3)));
            balanceSheet.setCommonStock(close.multiply(BigDecimal.valueOf(0.1)));

            balanceSheet.setWorkingCapital(balanceSheet.getCurrentAssets().subtract(balanceSheet.getCurrentLiabilities()));
            balanceSheet.setReportingCurrency("USD");
            balanceSheet.setFiscalYear(String.valueOf(balanceSheet.getDate().getYear()));
            int quarter = (balanceSheet.getDate().getMonthValue() - 1) / 3 + 1;
            balanceSheet.setFiscalQuarter("Q" + quarter);
            
            balanceSheets.add(balanceSheet);
        }
        
        return balanceSheets;
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
    public void updateBalanceSheets() {
        log.info("Starting scheduled balance sheet update");
        balanceSheetRepository.findAll().forEach(balanceSheet -> {
            try {
                fetchAndSaveBalanceSheets(balanceSheet.getSymbol());
            } catch (Exception e) {
                log.error("Error updating balance sheet data for symbol: " + balanceSheet.getSymbol(), e);
            }
        });
    }

    public BalanceSheet saveBalanceSheet(BalanceSheet balanceSheet) {
        BalanceSheet saved = balanceSheetRepository.save(balanceSheet);
        String redisKey = REDIS_KEY_PREFIX + saved.getSymbol();
        List<BalanceSheet> existingSheets = getBalanceSheetsBySymbol(saved.getSymbol());
        existingSheets.add(saved);
        redisTemplate.opsForValue().set(redisKey, existingSheets, CACHE_DURATION_HOURS, TimeUnit.HOURS);
        return saved;
    }

    public void deleteBalanceSheet(String id) {
        Optional<BalanceSheet> balanceSheet = balanceSheetRepository.findById(id);
        balanceSheet.ifPresent(sheet -> {
            balanceSheetRepository.deleteById(id);
            String redisKey = REDIS_KEY_PREFIX + sheet.getSymbol();
            redisTemplate.delete(redisKey);
        });
    }

    public List<BalanceSheet> getBalanceSheetsBySymbolAndDateRange(String symbol, LocalDate startDate, LocalDate endDate) {
        String redisKey = REDIS_KEY_PREFIX + symbol + ":range:" + startDate + ":" + endDate;
        
        // Try to get from Redis first
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null && cachedData instanceof List<?>) {
            log.info("Retrieved balance sheets for {} from Redis cache for date range", symbol);
            return (List<BalanceSheet>) cachedData;
        }

        // If not in Redis, get from MongoDB
        List<BalanceSheet> balanceSheets = balanceSheetRepository.findBySymbolAndDateBetween(symbol, startDate, endDate);
        if (!balanceSheets.isEmpty()) {
            // Cache in Redis
            log.info("Retrieved balance sheets for {} from MongoDB for date range", symbol);
            redisTemplate.opsForValue().set(redisKey, balanceSheets, CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return balanceSheets;
        }

        // If not found, fetch all balance sheets and filter by date range
        log.info("Fetching balance sheets for {} and filtering by date range", symbol);
        List<BalanceSheet> allSheets = fetchAndSaveBalanceSheets(symbol);
        balanceSheets = allSheets.stream()
                .filter(sheet -> !sheet.getDate().isBefore(startDate) && !sheet.getDate().isAfter(endDate))
                .toList();
        
        // Cache the filtered results
        if (!balanceSheets.isEmpty()) {
            redisTemplate.opsForValue().set(redisKey, balanceSheets, CACHE_DURATION_HOURS, TimeUnit.HOURS);
        }
        
        return balanceSheets;
    }

    public BalanceSheet getLatestBalanceSheet(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol + ":latest";
        
        // Try to get from Redis first
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null && cachedData instanceof BalanceSheet) {
            log.info("Retrieved latest balance sheet for {} from Redis cache", symbol);
            return (BalanceSheet) cachedData;
        }

        // If not in Redis, get from MongoDB
        Optional<BalanceSheet> latestSheet = balanceSheetRepository.findFirstBySymbolOrderByDateDesc(symbol);
        if (latestSheet.isPresent()) {
            // Cache in Redis
            log.info("Retrieved latest balance sheet for {} from MongoDB", symbol);
            redisTemplate.opsForValue().set(redisKey, latestSheet.get(), CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return latestSheet.get();
        }

        // If not found, fetch all and get the latest
        log.info("Fetching balance sheets for {} and getting latest", symbol);
        List<BalanceSheet> allSheets = fetchAndSaveBalanceSheets(symbol);
        if (!allSheets.isEmpty()) {
            BalanceSheet latest = allSheets.get(0); // First one since we're ordering by date desc
            redisTemplate.opsForValue().set(redisKey, latest, CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return latest;
        }
        
        return null;
    }
} 