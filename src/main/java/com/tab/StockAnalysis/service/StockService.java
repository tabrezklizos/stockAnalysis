package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.StockData;
import com.tab.StockAnalysis.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    @org.springframework.beans.factory.annotation.Qualifier("stockDataRedisTemplate")
    private final RedisTemplate<String, StockData> redisTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String API_KEY = "25HLtnCgiRCFX9fcryDyWJzOxPEeXfKx";
    private static final String BASE_URL = "https://financialmodelingprep.com/api/v3";
    private static final String REDIS_KEY_PREFIX = "stock_data:";
    private static final long CACHE_DURATION_MINUTES = 15;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    public StockData getStockData(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;

        StockData cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("Retrieved stock data for {} from Redis cache", symbol);
            return cachedData;
        }

        Optional<StockData> stockDataOptional = stockRepository.findFirstBySymbolOrderByLastUpdatedDesc(symbol);
        if (stockDataOptional.isPresent()) {
            StockData data = stockDataOptional.get();
            if (data.getLastUpdated().plusMinutes(CACHE_DURATION_MINUTES).isAfter(LocalDateTime.now())) {
                log.info("Retrieved fresh stock data for {} from MongoDB", symbol);
                redisTemplate.opsForValue().set(redisKey, data, CACHE_DURATION_MINUTES, TimeUnit.MINUTES);
                return data;
            }
        }

        log.info("Fetching stock data for {} from FMP API", symbol);
        return fetchAndSaveStockData(symbol);
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void updateStockData() {
        log.info("Starting scheduled stock data update");
        stockRepository.findAll().forEach(stock -> {
            try {
                if (stock.getLastUpdated().plusMinutes(CACHE_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
                    fetchAndSaveStockData(stock.getSymbol());
                }
            } catch (Exception e) {
                log.error("Error updating stock data for symbol: " + stock.getSymbol(), e);
            }
        });
    }

    private StockData fetchAndSaveStockData(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                String url = UriComponentsBuilder.fromUriString(BASE_URL + "/quote/" + symbol)
                    .queryParam("apikey", API_KEY)
                    .build()
                    .toUriString();

                log.info("Fetching data from FMP for symbol: {} URL: {} (attempt {})", symbol, url, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "application/json");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    // FMP returns an array, even for a single symbol quote
                    if (root.isArray() && root.size() > 0) {
                        JsonNode quote = root.get(0);
                        StockData stockData = parseStockData(quote, symbol);

                        log.info("Saving stock data to MongoDB for symbol: {}", symbol);
                        stockData = stockRepository.save(stockData);

                        log.info("Caching stock data in Redis for symbol: {}", symbol);
                        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + symbol, stockData, CACHE_DURATION_MINUTES, TimeUnit.MINUTES);

                        return stockData;
                    } else {
                        log.error("Empty or invalid array response from FMP for symbol: {}", symbol);
                        throw new RuntimeException("Empty or invalid array response from FMP API for symbol: " + symbol);
                    }
                } else {
                    log.error("Failed to fetch data from FMP. Status code: {}", response.getStatusCode());
                    throw new RuntimeException("Failed to fetch data from FMP API. Status: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Error fetching stock data for symbol: " + symbol + ". Attempt: " + (retryCount + 1), e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting to retry fetching stock data for symbol: {}", symbol, ie);
                        throw new RuntimeException("Interrupted while waiting to retry for symbol: " + symbol, ie);
                    }
                } else {
                    log.error("Max retries reached for symbol: {}. Error: {}", symbol, e.getMessage());
                    throw new RuntimeException("Failed to fetch stock data for " + symbol + " after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        // Should not be reached if MAX_RETRIES > 0
        throw new RuntimeException("Failed to fetch stock data for " + symbol + " after " + MAX_RETRIES + " attempts (loop exit)"); 
    }

    private StockData parseStockData(JsonNode quote, String symbol) {
        StockData data = new StockData();
        data.setSymbol(symbol);
        data.setCompanyName(getString(quote, "name"));
        data.setExchange(getString(quote, "exchange"));
        data.setCurrentPrice(getBigDecimal(quote, "price"));
        data.setPreviousClose(getBigDecimal(quote, "previousClose"));
        data.setDayHigh(getBigDecimal(quote, "dayHigh"));
        data.setDayLow(getBigDecimal(quote, "dayLow"));
        data.setVolume(getLong(quote, "volume"));
        data.setMarketCap(getBigDecimal(quote, "marketCap"));
        data.setPriceChange(getBigDecimal(quote, "change"));
        data.setPriceChangePercent(getBigDecimal(quote, "changesPercentage"));
        data.setFiftyDayAverage(getBigDecimal(quote, "priceAvg50"));
        data.setTwoHundredDayAverage(getBigDecimal(quote, "priceAvg200"));
        data.setYearHigh(getBigDecimal(quote, "yearHigh"));
        data.setYearLow(getBigDecimal(quote, "yearLow"));
        data.setSharesOutstanding(getLong(quote, "sharesOutstanding"));
        data.setEps(getBigDecimal(quote, "eps"));
        data.setPe(getBigDecimal(quote, "pe"));
        data.setLastUpdated(LocalDateTime.now());
        return data;
    }

    private BigDecimal getBigDecimal(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            try {
                return new BigDecimal(node.get(fieldName).asText());
            } catch (NumberFormatException e) {
                log.warn("Could not parse BigDecimal for field '{}' with value '{}'", fieldName, node.get(fieldName).asText());
                return null;
            }
        }
        return null;
    }

    private Long getLong(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            try {
                return node.get(fieldName).asLong();
            } catch (Exception e) {
                log.warn("Could not parse Long for field '{}' with value '{}'", fieldName, node.get(fieldName).asText());
                return null;
            }
        }
        return null;
    }

    private String getString(JsonNode node, String fieldName) {
        if (node.hasNonNull(fieldName)) {
            return node.get(fieldName).asText();
        }
        return null;
    }
}
