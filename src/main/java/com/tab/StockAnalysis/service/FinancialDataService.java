//package com.tab.StockAnalysis.service;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.tab.StockAnalysis.entity.FinancialData;
//import com.tab.StockAnalysis.repository.FinancialDataRepository;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.cache.annotation.CacheEvict;
//import org.springframework.cache.annotation.Cacheable;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.http.*;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.web.util.UriComponentsBuilder;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//import java.math.BigDecimal;
//import java.time.Duration;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class FinancialDataService {
//
//    private final FinancialDataRepository financialDataRepository;
//    private final ObjectMapper objectMapper;
//    @org.springframework.beans.factory.annotation.Qualifier("financialDataRedisTemplate")
//    private final RedisTemplate<String, FinancialData> redisTemplate;
//    private final RestTemplateBuilder restTemplateBuilder;
//
//    private static final String API_KEY = "25HLtnCgiRCFX9fcryDyWJzOxPEeXfKx";
//    private static final String BASE_URL = "https://financialmodelingprep.com/api/v3";
//    private static final String REDIS_KEY_PREFIX = "financial_data:";
//    private static final long CACHE_DURATION_MINUTES = 60;
//
//    private RestTemplate restTemplate;
//
//    @PostConstruct
//    public void init() {
//        this.restTemplate = restTemplateBuilder
//            .setConnectTimeout(Duration.ofSeconds(5))
//            .setReadTimeout(Duration.ofSeconds(5))
//            .build();
//    }
//
//    private Optional<FinancialData> fetchAndSaveFinancialData(String symbol) {
//        try {
//            String url = UriComponentsBuilder.fromUriString(BASE_URL + "/quote/" + symbol)
//                .queryParam("apikey", API_KEY)
//                .build()
//                .toUriString();
//
//            log.info("Fetching data from FMP for symbol: {} URL: {}", symbol, url);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.set(HttpHeaders.ACCEPT, "application/json");
//
//            ResponseEntity<JsonNode[]> response = restTemplate.exchange(
//                url,
//                HttpMethod.GET,
//                new HttpEntity<>(headers),
//                JsonNode[].class
//            );
//
//            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().length == 0) {
//                log.error("Failed to fetch data from FMP. Status code: {}", response.getStatusCode());
//                return Optional.empty();
//            }
//
//            JsonNode quote = response.getBody()[0];
//            FinancialData data = parseFinancialData(quote, symbol);
//
//            // Validate parsed data
//            if (data.getCurrentPrice() == null || data.getMarketCap() == null) {
//                log.error("Failed to parse essential financial data for symbol: {}", symbol);
//                return Optional.empty();
//            }
//
//            // Save to MongoDB
//            FinancialData savedData = financialDataRepository.save(data);
//            log.info("Saved financial data to MongoDB for symbol: {}", symbol);
//
//            // Cache in Redis
//            cacheFinancialData(symbol, savedData);
//            log.info("Cached financial data in Redis for symbol: {}", symbol);
//
//            return Optional.of(savedData);
//
//        } catch (Exception e) {
//            log.error("Error fetching financial data for symbol: " + symbol, e);
//            return Optional.empty();
//        }
//    }
//
//    private FinancialData parseFinancialData(JsonNode quote, String symbol) {
//        try {
//            FinancialData data = new FinancialData();
//            data.setSymbol(symbol);
//            data.setLastUpdated(LocalDateTime.now());
//
//            // Parse market data
//            if (quote.has("price")) {
//                data.setCurrentPrice(getBigDecimal(quote.path("price")));
//            }
//            if (quote.has("previousClose")) {
//                data.setPreviousClose(getBigDecimal(quote.path("previousClose")));
//            }
//            if (quote.has("dayHigh")) {
//                data.setDayHigh(getBigDecimal(quote.path("dayHigh")));
//            }
//            if (quote.has("dayLow")) {
//                data.setDayLow(getBigDecimal(quote.path("dayLow")));
//            }
//            if (quote.has("volume")) {
//                data.setVolume(quote.path("volume").asLong());
//            }
//            if (quote.has("marketCap")) {
//                data.setMarketCap(getBigDecimal(quote.path("marketCap")));
//            }
//            if (quote.has("sharesOutstanding")) {
//                data.setSharesOutstanding(quote.path("sharesOutstanding").asLong());
//            }
//
//            // Calculate price change
//            if (data.getCurrentPrice() != null && data.getPreviousClose() != null) {
//                data.setPriceChange(data.getCurrentPrice().subtract(data.getPreviousClose()));
//                if (!data.getPreviousClose().equals(BigDecimal.ZERO)) {
//                    data.setPriceChangePercent(data.getPriceChange()
//                        .multiply(BigDecimal.valueOf(100))
//                        .divide(data.getPreviousClose(), 2, BigDecimal.ROUND_HALF_UP));
//                }
//            }
//
//            // Parse additional market data
//            if (quote.has("priceAvg50")) {
//                data.setFiftyDayAverage(getBigDecimal(quote.path("priceAvg50")));
//            }
//            if (quote.has("priceAvg200")) {
//                data.setTwoHundredDayAverage(getBigDecimal(quote.path("priceAvg200")));
//            }
//            if (quote.has("beta")) {
//                data.setBeta(getBigDecimal(quote.path("beta")));
//            }
//
//            // Log the parsed data
//            log.info("Parsed financial data for {}: price={}, marketCap={}, volume={}",
//                symbol, data.getCurrentPrice(), data.getMarketCap(), data.getVolume());
//
//            return data;
//        } catch (Exception e) {
//            log.error("Error parsing financial data for symbol: " + symbol, e);
//            throw e;
//        }
//    }
//
//    private BigDecimal getBigDecimal(JsonNode node) {
//        if (node == null || node.isNull()) {
//            return null;
//        }
//        try {
//            return BigDecimal.valueOf(node.asDouble());
//        } catch (Exception e) {
//            log.error("Error converting node to BigDecimal: {}", node, e);
//            return null;
//        }
//    }
//
//    private void cacheFinancialData(String symbol, FinancialData data) {
//        try {
//            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + symbol, data, CACHE_DURATION_MINUTES, TimeUnit.MINUTES);
//            log.debug("Successfully cached data for symbol: {}", symbol);
//        } catch (Exception e) {
//            log.error("Error caching financial data for symbol: " + symbol, e);
//        }
//    }
//
//    @Cacheable(value = "financialData", key = "#symbol")
//    public Optional<FinancialData> getFinancialData(String symbol) {
//        log.info("Fetching financial data for symbol: {}", symbol);
//        String redisKey = REDIS_KEY_PREFIX + symbol;
//
//        // Try Redis first
//        FinancialData cachedData = redisTemplate.opsForValue().get(redisKey);
//        if (cachedData != null) {
//            log.info("Found cached data for symbol: {}", symbol);
//            return Optional.of(cachedData);
//        }
//
//        // Try MongoDB next
//        Optional<FinancialData> latestData = financialDataRepository.findFirstBySymbolOrderByLastUpdatedDesc(symbol);
//        if (latestData.isPresent()) {
//            log.info("Found MongoDB data for symbol: {}", symbol);
//            cacheFinancialData(symbol, latestData.get());
//            return latestData;
//        }
//
//        // Fetch from API
//        return fetchAndSaveFinancialData(symbol);
//    }
//
//    @CacheEvict(value = "financialData", key = "#symbol")
//    public void evictCache(String symbol) {
//        try {
//            redisTemplate.delete(REDIS_KEY_PREFIX + symbol);
//            log.info("Evicted cache for symbol: {}", symbol);
//        } catch (Exception e) {
//            log.error("Error evicting cache for symbol: " + symbol, e);
//        }
//    }
//
//    @Scheduled(cron = "0 */15 * * * *")
//    public void updateAllFinancialData() {
//        log.info("Starting scheduled update of all financial data");
//        financialDataRepository.findAll().forEach(data -> {
//            try {
//                fetchAndSaveFinancialData(data.getSymbol());
//            } catch (Exception e) {
//                log.error("Error updating financial data for symbol: " + data.getSymbol(), e);
//            }
//        });
//        log.info("Completed scheduled update of all financial data");
//    }
//
//    public List<FinancialData> getByMarketCap(BigDecimal minMarketCap) {
//        return financialDataRepository.findByMarketCapGreaterThanEqual(minMarketCap);
//    }
//
//    public void deleteBySymbol(String symbol) {
//        financialDataRepository.deleteBySymbol(symbol);
//        evictCache(symbol);
//    }
//}