package com.tab.StockAnalysis.controller;

import com.tab.StockAnalysis.entity.AssetProfile;
import com.tab.StockAnalysis.service.AssetProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asset-profiles")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AssetProfileController {

    private final AssetProfileService assetProfileService;

    @GetMapping
    public ResponseEntity<List<AssetProfile>> getAllAssetProfiles() {
        return ResponseEntity.ok(assetProfileService.getAllAssetProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetProfile> getAssetProfileById(@PathVariable String id) {
        return assetProfileService.getAssetProfileById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/symbol/{symbol}")
    public ResponseEntity<List<AssetProfile>> getAssetProfilesBySymbol(@PathVariable String symbol) {
        List<AssetProfile> profiles = assetProfileService.getAssetProfilesBySymbol(symbol);
        return !profiles.isEmpty() ? ResponseEntity.ok(profiles) : ResponseEntity.notFound().build();
    }

    @GetMapping("/industry/{industry}")
    public ResponseEntity<List<AssetProfile>> getAssetProfilesByIndustry(@PathVariable String industry) {
        List<AssetProfile> profiles = assetProfileService.getAssetProfilesByIndustry(industry);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/sector/{sector}")
    public ResponseEntity<List<AssetProfile>> getAssetProfilesBySector(@PathVariable String sector) {
        List<AssetProfile> profiles = assetProfileService.getAssetProfilesBySector(sector);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/market-cap")
    public ResponseEntity<List<AssetProfile>> getAssetProfilesByMarketCap(
            @RequestParam(name = "minMarketCap") double minMarketCap) {
        List<AssetProfile> profiles = assetProfileService.getAssetProfilesByMarketCap(minMarketCap);
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/country/{country}")
    public ResponseEntity<List<AssetProfile>> getAssetProfilesByCountry(@PathVariable String country) {
        List<AssetProfile> profiles = assetProfileService.getAssetProfilesByCountry(country);
        return ResponseEntity.ok(profiles);
    }

    @PostMapping
    public ResponseEntity<AssetProfile> createAssetProfile(@RequestBody AssetProfile assetProfile) {
        return ResponseEntity.ok(assetProfileService.saveAssetProfile(assetProfile));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssetProfile(@PathVariable String id) {
        assetProfileService.deleteAssetProfile(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/update/status")
    public ResponseEntity<Map<String, Object>> getUpdateStatus() {
        return ResponseEntity.ok(assetProfileService.getUpdateStatus());
    }

    @PostMapping("/update/trigger")
    public ResponseEntity<Map<String, Object>> triggerUpdate() {
        return ResponseEntity.ok(assetProfileService.triggerManualUpdate());
    }
} 