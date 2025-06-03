package com.tab.StockAnalysis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tab.StockAnalysis.entity.MarketData;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

public class MarketDataRedisSerializer implements RedisSerializer<MarketData> {
    private final ObjectMapper objectMapper;

    public MarketDataRedisSerializer() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public byte[] serialize(MarketData marketData) throws SerializationException {
        if (marketData == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(marketData);
        } catch (Exception e) {
            throw new SerializationException("Error serializing MarketData", e);
        }
    }

    @Override
    public MarketData deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, MarketData.class);
        } catch (Exception e) {
            throw new SerializationException("Error deserializing MarketData", e);
        }
    }
} 