package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.Earnings;
import com.tab.StockAnalysis.repository.EarningsRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EarningsService {

    private final EarningsRepository earningsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "earnings:";
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final long CACHE_DURATION_HOURS = 24;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    public Earnings getEarnings(String symbol) {
        // Try to get from Redis cache
        String redisKey = REDIS_KEY_PREFIX + symbol;
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("Retrieved earnings data for {} from Redis cache", symbol);
            return (Earnings) cachedData;
        }

        // Try to get from MongoDB
        Optional<Earnings> earnings = earningsRepository.findFirstBySymbolOrderByLastUpdatedDesc(symbol);
        if (earnings.isPresent()) {
            // Cache in Redis
            redisTemplate.opsForValue().set(redisKey, earnings.get(), CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return earnings.get();
        }

        // Fetch from Yahoo Finance
        return fetchAndSaveEarnings(symbol);
    }

    public List<Earnings> getUpcomingEarnings(LocalDate startDate, LocalDate endDate) {
        return earningsRepository.findByCurrentQuarterDateBetween(startDate, endDate);
    }

    public List<Earnings> getHighGrowthEarnings(BigDecimal growthThreshold) {
        return earningsRepository.findByEstimateGrowthGreaterThan(growthThreshold);
    }

    public List<Earnings> getPositiveSurpriseEarnings(BigDecimal surpriseThreshold) {
        return earningsRepository.findByEarningsSurpriseScoreGreaterThan(surpriseThreshold);
    }

    private Earnings fetchAndSaveEarnings(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching earnings data from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = YAHOO_FINANCE_URL + symbol + "?interval=1d&range=5y"; // Get 5 years of data
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode meta = result.path("meta");

                    Earnings earnings = parseEarnings(result, symbol);
                    
                    // Save to MongoDB
                    earnings = earningsRepository.save(earnings);

                    // Cache in Redis
                    String redisKey = REDIS_KEY_PREFIX + symbol;
                    redisTemplate.opsForValue().set(redisKey, earnings, CACHE_DURATION_HOURS, TimeUnit.HOURS);

                    return earnings;
                }

                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                log.error("Error fetching earnings data for symbol: " + symbol, e);
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

        throw new RuntimeException("Failed to fetch earnings data after " + MAX_RETRIES + " attempts");
    }

    private Earnings parseEarnings(JsonNode result, String symbol) {
        Earnings earnings = new Earnings();
        earnings.setSymbol(symbol);
        earnings.setLastUpdated(LocalDateTime.now());

        // Set current quarter info (example values - in real implementation, parse from API)
        earnings.setCurrentQuarter("Q" + ((LocalDate.now().getMonthValue() - 1) / 3 + 1));
        earnings.setCurrentQuarterDate(LocalDate.now().plusMonths(1));
        earnings.setCurrentQuarterEstimateEps(BigDecimal.valueOf(2.50));
        earnings.setCurrentQuarterEstimateRevenue(BigDecimal.valueOf(1000000000));
        earnings.setNumberOfAnalysts(15);
        earnings.setEstimateGrowth(BigDecimal.valueOf(0.12));

        // Set next quarter estimates
        earnings.setNextQuarter("Q" + ((LocalDate.now().plusMonths(3).getMonthValue() - 1) / 3 + 1));
        earnings.setNextQuarterDate(LocalDate.now().plusMonths(4));
        earnings.setNextQuarterEstimateEps(BigDecimal.valueOf(2.75));
        earnings.setNextQuarterEstimateRevenue(BigDecimal.valueOf(1100000000));
        earnings.setNextQuarterNumberOfAnalysts(14);

        // Set historical quarterly results
        List<Earnings.QuarterlyEarnings> quarterlyEarnings = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Earnings.QuarterlyEarnings qe = new Earnings.QuarterlyEarnings();
            qe.setQuarter("Q" + i);
            qe.setDate(LocalDate.now().minusMonths(i * 3));
            qe.setReportedEps(BigDecimal.valueOf(2.0 + i * 0.25));
            qe.setEstimatedEps(BigDecimal.valueOf(1.9 + i * 0.25));
            qe.setSurprise(BigDecimal.valueOf(0.1));
            qe.setSurprisePercentage(BigDecimal.valueOf(5.0));
            qe.setReportedRevenue(BigDecimal.valueOf(900000000 + i * 25000000));
            qe.setEstimatedRevenue(BigDecimal.valueOf(880000000 + i * 25000000));
            qe.setRevenueSurprise(BigDecimal.valueOf(20000000));
            qe.setRevenueSurprisePercentage(BigDecimal.valueOf(2.27));
            quarterlyEarnings.add(qe);
        }
        earnings.setQuarterlyEarnings(quarterlyEarnings);

        // Set growth metrics
        earnings.setQuarterlyGrowth(BigDecimal.valueOf(0.15));
        earnings.setYearlyGrowth(BigDecimal.valueOf(0.25));
        earnings.setFiveYearGrowthRate(BigDecimal.valueOf(0.18));

        // Set quality metrics
        earnings.setEarningsQualityScore(BigDecimal.valueOf(85));
        earnings.setEarningsConsistencyScore(BigDecimal.valueOf(90));
        earnings.setEarningsSurpriseScore(BigDecimal.valueOf(75));

        // Set revision metrics
        earnings.setUpwardRevisions30Days(8);
        earnings.setDownwardRevisions30Days(2);
        earnings.setUpwardRevisions90Days(20);
        earnings.setDownwardRevisions90Days(5);

        return earnings;
    }

    @Scheduled(cron = "0 0 */6 * * *") // Run every 6 hours
    public void updateAllEarnings() {
        log.info("Starting scheduled earnings update");
        earningsRepository.findAll().forEach(earnings -> {
            try {
                fetchAndSaveEarnings(earnings.getSymbol());
            } catch (Exception e) {
                log.error("Error updating earnings data for symbol: " + earnings.getSymbol(), e);
            }
        });
    }
} 