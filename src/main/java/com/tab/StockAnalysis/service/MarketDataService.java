/*
package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.MarketData;
import com.tab.StockAnalysis.repository.MarketDataRepository;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, MarketData> marketDataRedisTemplate;

    private static final String API_KEY = "25HLtnCgiRCFX9fcryDyWJzOxPEeXfKx....";
    private static final String BASE_URL = "https://financialmodelingprep.com/api/v3....";
    private static final String REDIS_KEY_PREFIX = "market_data:";
    private static final long CACHE_TTL_MINUTES = 15;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    private LocalDateTime lastUpdateTime;

    @Cacheable(value = "marketData", key = "'latest:' + #symbol")
    public Optional<MarketData> getLatestMarketData(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        
        MarketData cachedData = marketDataRedisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("Retrieved market data for {} from Redis cache", symbol);
            return Optional.of(cachedData);
        }

        Optional<MarketData> latestData = marketDataRepository.findFirstBySymbolOrderByTimestampDesc(symbol);
        if (latestData.isPresent()) {
            log.info("Retrieved market data for {} from MongoDB", symbol);
            cacheMarketData(symbol, latestData.get());
            return latestData;
        }

        log.info("Fetching market data for {} from FMP API", symbol);
        return fetchAndSaveMarketData(symbol);
    }

    public List<MarketData> getMarketDataHistory(String symbol) {
        return marketDataRepository.findBySymbolOrderByTimestampDesc(symbol);
    }

    public List<MarketData> getMarketDataByTimeRange(String symbol, LocalDateTime startTime, LocalDateTime endTime) {
        return marketDataRepository.findBySymbolAndTimestampBetweenOrderByTimestampDesc(symbol, startTime, endTime);
    }

    public List<MarketData> getMarketDataByMarketCap(Double minMarketCap) {
        return marketDataRepository.findByMarketCapGreaterThanEqual(minMarketCap);
    }

    public List<MarketData> getMarketDataByVolume(Long minVolume) {
        return marketDataRepository.findByVolumeGreaterThanEqual(minVolume);
    }

    public List<MarketData> getMarketDataByExchange(String exchange) {
        return marketDataRepository.findByExchange(exchange);
    }

    private Optional<MarketData> fetchAndSaveMarketData(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                String url = UriComponentsBuilder.fromUriString(BASE_URL + "/quote/" + symbol)
                    .queryParam("apikey", API_KEY)
                    .build()
                    .toUriString();

                log.info("Fetching market data from FMP for symbol: {} URL: {} (attempt {})", symbol, url, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "application/json");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    if (root.isArray() && root.size() > 0) {
                        JsonNode quote = root.get(0);
                        MarketData marketData = parseMarketData(quote, symbol);
                        MarketData savedData = marketDataRepository.save(marketData);
                        cacheMarketData(symbol, savedData);
                        return Optional.of(savedData);
                    } else {
                        log.error("Empty or invalid array response from FMP for symbol: {}", symbol);
                        throw new RuntimeException("Empty or invalid array response from FMP API for symbol: " + symbol);
                    }
                } else {
                    log.error("Failed to fetch data from FMP. Status code: {}", response.getStatusCode());
                    throw new RuntimeException("Failed to fetch data from FMP API. Status: " + response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Error fetching market data for symbol: " + symbol + ". Attempt: " + (retryCount + 1), e);
                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Interrupted while waiting to retry fetching market data for symbol: {}", symbol, ie);
                        throw new RuntimeException("Interrupted while waiting to retry for symbol: " + symbol, ie);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private MarketData parseMarketData(JsonNode quote, String symbol) {
        MarketData marketData = new MarketData();
        marketData.setSymbol(symbol);
        marketData.setTimestamp(LocalDateTime.now());
        
        // Set price information
        marketData.setCurrentPrice(getBigDecimal(quote, "price"));
        marketData.setPreviousClose(getBigDecimal(quote, "previousClose"));
        marketData.setOpen(getBigDecimal(quote, "open"));
        marketData.setDayHigh(getBigDecimal(quote, "dayHigh"));
        marketData.setDayLow(getBigDecimal(quote, "dayLow"));

        // Set volume information
        marketData.setVolume(getLong(quote, "volume"));
        
        // Set market information
        marketData.setExchange(getString(quote, "exchange"));
        marketData.setCurrency("USD"); // FMP typically provides USD
        marketData.setMarketState(quote.path("marketOpen").asBoolean() ? "REGULAR" : "CLOSED");
        marketData.setIsTrading(quote.path("marketOpen").asBoolean());

        // Set market cap and shares outstanding
        marketData.setMarketCap(getBigDecimal(quote, "marketCap"));
        marketData.setSharesOutstanding(getLong(quote, "sharesOutstanding"));

        // Calculate day change and percentage
        marketData.setDayChange(getBigDecimal(quote, "change"));
        marketData.setDayChangePercent(getBigDecimal(quote, "changesPercentage"));

        marketData.setLastUpdated(LocalDateTime.now());
        return marketData;
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

    private void cacheMarketData(String symbol, MarketData marketData) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        marketDataRedisTemplate.opsForValue().set(redisKey, marketData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        log.info("Cached market data for symbol {} in Redis with TTL {} minutes", symbol, CACHE_TTL_MINUTES);
    }

    @CacheEvict(value = "marketData", key = "'latest:' + #symbol")
    public void evictCache(String symbol) {
        String redisKey = REDIS_KEY_PREFIX + symbol;
        marketDataRedisTemplate.delete(redisKey);
        log.info("Evicted cache for symbol: {}", symbol);
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes during market hours
    @CacheEvict(value = "marketData", allEntries = true)
    public void scheduledMarketDataUpdate() {
        if (isMarketHours()) {
            log.info("Starting scheduled market data update");
            updateAllMarketData();
        }
    }

    private boolean isMarketHours() {
        // For now, always return true as FMP handles market hours internally
        return true;
    }

    public void updateAllMarketData() {
        marketDataRepository.findAll().forEach(data -> {
            try {
                fetchAndSaveMarketData(data.getSymbol());
            } catch (Exception e) {
                log.error("Error updating market data for symbol: " + data.getSymbol(), e);
            }
        });
        lastUpdateTime = LocalDateTime.now();
    }

    public Map<String, Object> getUpdateStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastUpdate", lastUpdateTime);
        status.put("isMarketHours", isMarketHours());
        return status;
    }

    public Map<String, Object> triggerManualUpdate() {
        updateAllMarketData();
        return getUpdateStatus();
    }
} */
