package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.AssetProfile;
import com.tab.StockAnalysis.repository.AssetProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetProfileService {

    private final AssetProfileRepository assetProfileRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, AssetProfile> assetProfileRedisTemplate;

    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final String REDIS_PROFILE_KEY = "asset_profile:";
    private static final long CACHE_TTL_HOURS = 24;

    // Rate limiting constants
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000; // 5 seconds

    private LocalDateTime lastUpdateTime;

    @Cacheable(value = "assetProfiles", key = "'all'")
    public List<AssetProfile> getAllAssetProfiles() {
        return assetProfileRepository.findAll();
    }

    @Cacheable(value = "assetProfiles", key = "#id")
    public Optional<AssetProfile> getAssetProfileById(String id) {
        return assetProfileRepository.findById(id);
    }

    @Cacheable(value = "assetProfiles", key = "#symbol")
    public List<AssetProfile> getAssetProfilesBySymbol(String symbol) {
        String redisKey = REDIS_PROFILE_KEY + symbol;
        
        // Try Redis cache first
        AssetProfile cachedProfile = assetProfileRedisTemplate.opsForValue().get(redisKey);
        if (cachedProfile != null) {
            log.info("Retrieved asset profile for {} from Redis cache", symbol);
            return Collections.singletonList(cachedProfile);
        }

        // Then try MongoDB
        List<AssetProfile> profiles = assetProfileRepository.findAllBySymbolOrderByIdDesc(symbol);
        if (!profiles.isEmpty()) {
            log.info("Retrieved asset profiles for {} from MongoDB", symbol);
            // Cache in Redis - store only the most recent profile
            cacheProfiles(symbol, profiles);
            return profiles;
        }

        // If not in database, fetch from Yahoo Finance API
        log.info("Fetching asset profiles for {} from Yahoo Finance", symbol);
        profiles = fetchAndSaveAssetProfiles(symbol);
        // Cache in Redis - store only the most recent profile
        if (!profiles.isEmpty()) {
            cacheProfiles(symbol, profiles);
        }
        return profiles;
    }

    private void cacheProfiles(String symbol, List<AssetProfile> profiles) {
        if (profiles.isEmpty()) {
            return;
        }
        String redisKey = REDIS_PROFILE_KEY + symbol;
        // Store only the most recent profile
        AssetProfile mostRecentProfile = profiles.get(0);
        assetProfileRedisTemplate.opsForValue().set(redisKey, mostRecentProfile, CACHE_TTL_HOURS, TimeUnit.HOURS);
        log.info("Cached most recent profile for symbol {} in Redis with TTL {} hours", symbol, CACHE_TTL_HOURS);
    }

    @CacheEvict(value = "assetProfiles", key = "#symbol")
    public void evictCache(String symbol) {
        String redisKey = REDIS_PROFILE_KEY + symbol;
        assetProfileRedisTemplate.delete(redisKey);
        log.info("Evicted cache for symbol: {}", symbol);
    }

    private List<AssetProfile> fetchAndSaveAssetProfiles(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching data from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = YAHOO_FINANCE_URL + symbol + "?interval=3mo&range=5y"; // 5 years of quarterly data
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode timestamps = result.path("timestamp");
                    JsonNode indicators = result.path("indicators");
                    JsonNode quote = indicators.path("quote").get(0);

                    List<AssetProfile> profiles = parseAssetProfiles(timestamps, quote, symbol);

                    // Save to MongoDB
                    log.info("Saving asset profiles to MongoDB for symbol: {}", symbol);
                    return assetProfileRepository.saveAll(profiles);
                }
            } catch (Exception e) {
                log.error("Error fetching asset profile data for symbol: " + symbol, e);
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
        throw new RuntimeException("Failed to fetch asset profile data after " + MAX_RETRIES + " attempts");
    }

    private List<AssetProfile> parseAssetProfiles(JsonNode timestamps, JsonNode quote, String symbol) {
        List<AssetProfile> profiles = new ArrayList<>();
        
        for (int i = 0; i < timestamps.size(); i++) {
            if (quote.path("close").path(i).isNull()) continue;

            AssetProfile profile = new AssetProfile();
            profile.setSymbol(symbol);

            // Set financial data from quote
            BigDecimal close = BigDecimal.valueOf(quote.path("close").path(i).asDouble());
            BigDecimal volume = BigDecimal.valueOf(quote.path("volume").path(i).asDouble());
            BigDecimal high = BigDecimal.valueOf(quote.path("high").path(i).asDouble());
            BigDecimal low = BigDecimal.valueOf(quote.path("low").path(i).asDouble());

            // Set profile data based on market data
            profile.setMarketCap(close.multiply(volume));
            profile.setRevenueGrowth(BigDecimal.valueOf(0.1)); // Estimate 10% growth
            profile.setGrossMargins(BigDecimal.valueOf(0.3)); // Estimate 30% margin
            profile.setOperatingMargins(BigDecimal.valueOf(0.15)); // Estimate 15% margin
            profile.setProfitMargins(BigDecimal.valueOf(0.08)); // Estimate 8% margin

            // Set other fields with placeholder data
            profile.setCompanyName(symbol + " Company");
            profile.setIndustry("Technology"); // Default industry
            profile.setSector("Technology"); // Default sector
            profile.setWebsite("www." + symbol.toLowerCase() + ".com");
            profile.setDescription("Company profile for " + symbol);
            profile.setCountry("US");
            profile.setAddress("123 Main St");
            profile.setPhone("+1-555-0123");
            profile.setBusinessSummary("Business summary for " + symbol);
            profile.setFullTimeEmployees(1000); // Default employee count
            profile.setFinancialCurrency("USD");
            profile.setExchange("NYSE");
            profile.setQuoteType("EQUITY");
            profile.setMarket("us_market");
            
            profiles.add(profile);
        }
        
        return profiles;
    }

    @Cacheable(value = "assetProfiles", key = "'industry:' + #industry")
    public List<AssetProfile> getAssetProfilesByIndustry(String industry) {
        return assetProfileRepository.findAllByIndustry(industry);
    }

    @Cacheable(value = "assetProfiles", key = "'sector:' + #sector")
    public List<AssetProfile> getAssetProfilesBySector(String sector) {
        return assetProfileRepository.findAllBySector(sector);
    }

    @Cacheable(value = "assetProfiles", key = "'marketCap:' + #marketCap")
    public List<AssetProfile> getAssetProfilesByMarketCap(double marketCap) {
        return assetProfileRepository.findByMarketCapGreaterThanEqual(marketCap);
    }

    @Cacheable(value = "assetProfiles", key = "'country:' + #country")
    public List<AssetProfile> getAssetProfilesByCountry(String country) {
        return assetProfileRepository.findByCountry(country);
    }

    public AssetProfile saveAssetProfile(AssetProfile assetProfile) {
        AssetProfile saved = assetProfileRepository.save(assetProfile);
        evictCache(assetProfile.getSymbol());
        return saved;
    }

    public void deleteAssetProfile(String id) {
        Optional<AssetProfile> profile = assetProfileRepository.findById(id);
        profile.ifPresent(p -> evictCache(p.getSymbol()));
        assetProfileRepository.deleteById(id);
    }

    @Scheduled(cron = "0 0 1 * * ?") // Run at 1 AM every day
    @CacheEvict(value = "assetProfiles", allEntries = true)
    public void scheduledUpdateAssetProfiles() {
        log.info("Starting scheduled asset profile update at {}", LocalDateTime.now());
        updateAllAssetProfiles();
    }

    public void updateAllAssetProfiles() {
        int successCount = 0;
        int failureCount = 0;
        List<String> failedSymbols = new ArrayList<>();

        // Get unique symbols to update
        Set<String> uniqueSymbols = new HashSet<>();
        assetProfileRepository.findAll().forEach(profile -> uniqueSymbols.add(profile.getSymbol()));
        
        int totalSymbols = uniqueSymbols.size();
        log.info("Found {} unique symbols to update", totalSymbols);

        for (String symbol : uniqueSymbols) {
            try {
                log.info("Updating profiles for symbol: {}", symbol);
                // Clear cache for this symbol
                evictCache(symbol);
                
                List<AssetProfile> updatedProfiles = fetchAndSaveAssetProfiles(symbol);
                if (!updatedProfiles.isEmpty()) {
                    successCount++;
                    // Update cache with new data
                    cacheProfiles(symbol, updatedProfiles);
                    log.info("Successfully updated {} profiles for symbol: {}", updatedProfiles.size(), symbol);
                } else {
                    failureCount++;
                    failedSymbols.add(symbol);
                    log.error("No profiles fetched for symbol: {}", symbol);
                }
                // Add delay to respect rate limits
                Thread.sleep(500); // 500ms delay between requests
            } catch (Exception e) {
                failureCount++;
                failedSymbols.add(symbol);
                log.error("Error updating profile for symbol: " + symbol, e);
            }
        }

        lastUpdateTime = LocalDateTime.now();
        log.info("Asset profile update completed at {}", lastUpdateTime);
        log.info("Update summary - Success: {}, Failures: {}, Total: {}", successCount, failureCount, totalSymbols);
        if (!failedSymbols.isEmpty()) {
            log.info("Failed symbols: {}", String.join(", ", failedSymbols));
        }
    }

    public Map<String, Object> getUpdateStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("lastUpdateTime", lastUpdateTime);
        status.put("totalProfiles", assetProfileRepository.count());
        status.put("uniqueSymbols", assetProfileRepository.findAll()
            .stream()
            .map(AssetProfile::getSymbol)
            .distinct()
            .count());
        return status;
    }

    public Map<String, Object> triggerManualUpdate() {
        log.info("Manual update triggered at {}", LocalDateTime.now());
        updateAllAssetProfiles();
        return getUpdateStatus();
    }
} 