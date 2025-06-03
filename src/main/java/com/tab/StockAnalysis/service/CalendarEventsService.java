package com.tab.StockAnalysis.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tab.StockAnalysis.entity.CalendarEvents;
import com.tab.StockAnalysis.repository.CalendarEventsRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarEventsService {

    private final CalendarEventsRepository calendarEventsRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "calendar_events:";
    private static final String YAHOO_FINANCE_URL = "https://query1.finance.yahoo.com/v8/finance/chart/";
    private static final long CACHE_DURATION_HOURS = 24;
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 5000;

    public CalendarEvents getCalendarEvents(String symbol) {
        // Try to get from Redis cache
        String redisKey = REDIS_KEY_PREFIX + symbol;
        Object cachedData = redisTemplate.opsForValue().get(redisKey);
        if (cachedData != null) {
            log.info("Retrieved calendar events for {} from Redis cache", symbol);
            return (CalendarEvents) cachedData;
        }

        // Try to get from MongoDB
        Optional<CalendarEvents> events = calendarEventsRepository.findFirstBySymbolOrderByLastUpdatedDesc(symbol);
        if (events.isPresent()) {
            // Cache in Redis
            redisTemplate.opsForValue().set(redisKey, events.get(), CACHE_DURATION_HOURS, TimeUnit.HOURS);
            return events.get();
        }

        // Fetch from Yahoo Finance
        return fetchAndSaveCalendarEvents(symbol);
    }

    public List<CalendarEvents> getUpcomingEarnings(LocalDate startDate, LocalDate endDate) {
        return calendarEventsRepository.findByNextEarningsDateBetween(startDate, endDate);
    }

    public List<CalendarEvents> getUpcomingDividends(LocalDate startDate, LocalDate endDate) {
        return calendarEventsRepository.findByNextDividendDateBetween(startDate, endDate);
    }

    public List<CalendarEvents> getUpcomingSplits(LocalDate startDate, LocalDate endDate) {
        return calendarEventsRepository.findByNextSplitDateBetween(startDate, endDate);
    }

    private CalendarEvents fetchAndSaveCalendarEvents(String symbol) {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                log.info("Fetching calendar events from Yahoo Finance for symbol: {} (attempt {})", symbol, retryCount + 1);

                HttpHeaders headers = new HttpHeaders();
                headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = YAHOO_FINANCE_URL + symbol + "?interval=1d&range=1d";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode root = objectMapper.readTree(response.getBody());
                    JsonNode result = root.path("chart").path("result").get(0);
                    JsonNode meta = result.path("meta");

                    CalendarEvents events = parseCalendarEvents(meta, symbol);
                    
                    // Save to MongoDB
                    events = calendarEventsRepository.save(events);

                    // Cache in Redis
                    String redisKey = REDIS_KEY_PREFIX + symbol;
                    redisTemplate.opsForValue().set(redisKey, events, CACHE_DURATION_HOURS, TimeUnit.HOURS);

                    return events;
                }

                retryCount++;
                if (retryCount < MAX_RETRIES) {
                    Thread.sleep(RETRY_DELAY_MS);
                }
            } catch (Exception e) {
                log.error("Error fetching calendar events for symbol: " + symbol, e);
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

        throw new RuntimeException("Failed to fetch calendar events after " + MAX_RETRIES + " attempts");
    }

    private CalendarEvents parseCalendarEvents(JsonNode meta, String symbol) {
        CalendarEvents events = new CalendarEvents();
        events.setSymbol(symbol);
        events.setLastUpdated(LocalDateTime.now());

        // Example parsing logic - in real implementation, you would parse actual data from Yahoo Finance API
        // This is just placeholder logic to demonstrate the structure
        if (meta.has("nextEarningsDate")) {
            long timestamp = meta.path("nextEarningsDate").asLong() * 1000;
            events.setNextEarningsDate(LocalDate.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
            ));
        }

        // Set example values (in real implementation, these would come from the API)
        events.setNextEarningsQuarter("Q" + ((LocalDate.now().getMonthValue() - 1) / 3 + 1));
        events.setEarningsEstimate(BigDecimal.valueOf(1.25));
        events.setRevenueEstimate(BigDecimal.valueOf(1000000000));
        events.setNumberOfAnalysts(15);

        // Set dividend information
        events.setDividendFrequency("Quarterly");
        events.setDividendYield(BigDecimal.valueOf(0.025));
        events.setDividendAmount(BigDecimal.valueOf(0.88));
        events.setNextDividendDate(LocalDate.now().plusMonths(3));
        events.setExDividendDate(LocalDate.now().plusMonths(2).plusWeeks(2));

        // Set fiscal period information
        events.setFiscalYearEnd("December");
        events.setMostRecentQuarter("2023-Q4");

        return events;
    }

    @Scheduled(cron = "0 0 */12 * * *") // Run every 12 hours
    public void updateAllCalendarEvents() {
        log.info("Starting scheduled calendar events update");
        calendarEventsRepository.findAll().forEach(events -> {
            try {
                fetchAndSaveCalendarEvents(events.getSymbol());
            } catch (Exception e) {
                log.error("Error updating calendar events for symbol: " + events.getSymbol(), e);
            }
        });
    }
} 