package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.CalendarEvents;
import com.tab.StockAnalysis.service.CalendarEventsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarEventsController {

    private final CalendarEventsService calendarEventsService;

    @GetMapping("/{symbol}")
    public ResponseEntity<CalendarEvents> getCalendarEvents(@PathVariable String symbol) {
        return ResponseEntity.ok(calendarEventsService.getCalendarEvents(symbol));
    }

    @GetMapping("/earnings")
    public ResponseEntity<List<CalendarEvents>> getUpcomingEarnings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(3);
        }
        return ResponseEntity.ok(calendarEventsService.getUpcomingEarnings(startDate, endDate));
    }

    @GetMapping("/dividends")
    public ResponseEntity<List<CalendarEvents>> getUpcomingDividends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(3);
        }
        return ResponseEntity.ok(calendarEventsService.getUpcomingDividends(startDate, endDate));
    }

    @GetMapping("/splits")
    public ResponseEntity<List<CalendarEvents>> getUpcomingSplits(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusMonths(3);
        }
        return ResponseEntity.ok(calendarEventsService.getUpcomingSplits(startDate, endDate));
    }
} 