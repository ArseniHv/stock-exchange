package com.remitly.stock_exchange.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remitly.stock_exchange.model.LogEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditService {

    static final String LOG_KEY = "audit:log";

    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;

    public void log(String type, String walletId, String stockName) {
        try {
            String entry = mapper.writeValueAsString(new LogEntry(type, walletId, stockName));
            redis.opsForList().rightPush(LOG_KEY, entry);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize log entry", e);
        }
    }

    public List<LogEntry> getAll() {
        List<String> raw = redis.opsForList().range(LOG_KEY, 0, -1);
        if (raw == null) return List.of();
        return raw.stream().map(s -> {
            try {
                return mapper.readValue(s, LogEntry.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize log entry", e);
            }
        }).toList();
    }
}