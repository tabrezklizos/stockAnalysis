package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.KeyStatistics;
import com.tab.StockAnalysis.repository.KeyStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyStatisticsService {

    private final KeyStatisticsRepository keyStatisticsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "key_statistics:";
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final long CACHE_DURATION_HOURS = 24;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    public KeyStatistics getKeyStatistics(String symbol) {
        // Try to get from Redis cache
        String redisKey = REDIS_KEY_PREFIX + symbol;
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("Retrieved key statistics for {} from Redis cache", symbol);
            return (KeyStatistics) cachedData;
        }

        // Try to get from MongoDB
        Optional<KeyStatistics> statistics = keyStatisticsRepository.findFirstBySymbolOrderByDateDesc(symbol);
        if (statistics.isPresent()) {
            // Cache in Redis
            redisTemplate.opsForValue().set(redisKey, statistics.get(), CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return statistics.get();
        }

        // Fetch from Yahoo Finance
        return fetchAndSaveKeyStatistics(symbol);
    }

    public List<KeyStatistics> getHistoricalKeyStatistics(String symbol, String startDate, String endDate) {
        LocalDate start = startDate != null ? LocalDate.parse(startDate) : LocalDate.now().minusYears(1);
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : LocalDate.now();

        return keyStatisticsRepository.findBySymbolAndDateBetweenOrderByDateDesc(symbol, start, end);
    }

    public KeyStatistics getFinancialRatios(String symbol) {
        return getKeyStatistics(symbol); // This returns the same data but you might want to filter specific ratios
    }

    private KeyStatistics fetchAndSaveKeyStatistics(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching key statistics from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = YAHOO_FINANCE_URL + symbol + "?interval=1d&range=1d";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode quote = result.path("indicators").path("quote").get(0);
                    JsonNode meta = result.path("meta");

                    KeyStatistics statistics = parseKeyStatistics(quote, meta, symbol);
                    
                    // Save to MongoDB
                    statistics = keyStatisticsRepository.save(statistics);

                    // Cache in Redis
                    String redisKey = REDIS_KEY_PREFIX + symbol;
                    redisTemplate.opsForValue().set(redisKey, statistics, CACHE_DURATION_HOURS, TimeUnit.HOURS);

                    return statistics;
                }

                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                log.error("Error fetching key statistics for symbol: " + symbol, e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Thread interrupted while waiting to retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Failed to fetch key statistics after " + MAX_RETRIES + " attempts");
    }

    private KeyStatistics parseKeyStatistics(JsonNode quote, JsonNode meta, String symbol) {
        KeyStatistics statistics = new KeyStatistics();
        statistics.setSymbol(symbol);
        statistics.setDate(LocalDate.now());

        // Parse data from meta
        if (meta.has("marketCap")) {
            statistics.setMarketCap(BigDecimal.valueOf(meta.path("marketCap").asDouble()));
        }
        if (meta.has("regularMarketPrice")) {
            BigDecimal price = BigDecimal.valueOf(meta.path("regularMarketPrice").asDouble());
            statistics.setFiftyTwoWeekHigh(price.multiply(BigDecimal.valueOf(1.1))); // Example calculation
            statistics.setFiftyTwoWeekLow(price.multiply(BigDecimal.valueOf(0.9))); // Example calculation
        }

        // Parse data from quote
        if (!quote.path("volume").isEmpty()) {
            statistics.setAverageVolume(BigDecimal.valueOf(quote.path("volume").get(0).asDouble()));
        }

        // Set some calculated ratios (you would want to get these from actual API data)
        statistics.setPriceToBook(BigDecimal.valueOf(2.5)); // Example value
        statistics.setTrailingPE(BigDecimal.valueOf(15.0)); // Example value
        statistics.setForwardPE(BigDecimal.valueOf(14.0)); // Example value
        statistics.setPriceToSales(BigDecimal.valueOf(3.0)); // Example value
        
        return statistics;
    }

    @Scheduled(cron = "0 0 */4 * * *") // Run every 4 hours
    public void updateAllKeyStatistics() {
        log.info("Starting scheduled key statistics update");
        keyStatisticsRepository.findAll().forEach(stats -> {
            try {
                fetchAndSaveKeyStatistics(stats.getSymbol());
            } catch (Exception e) {
                log.error("Error updating key statistics for symbol: " + stats.getSymbol(), e);
            }
        });
    }
} 