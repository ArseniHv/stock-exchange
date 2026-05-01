package com.remitly.stock_exchange.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.remitly.stock_exchange.model.StockEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BankService {

    static final String BANK_KEY = "bank:stocks";

    private final StringRedisTemplate redis;

    public void setStocks(List<StockEntry> stocks) {
        redis.delete(BANK_KEY);
        if (stocks.isEmpty()) return;

        Map<String, String> entries = stocks.stream()
                .collect(Collectors.toMap(StockEntry::name, s -> String.valueOf(s.quantity())));
        redis.opsForHash().putAll(BANK_KEY, entries);
    }

    public List<StockEntry> getStocks() {
        return redis.opsForHash().entries(BANK_KEY)
                .entrySet().stream()
                .map(e -> new StockEntry((String) e.getKey(), Long.parseLong((String) e.getValue())))
                .toList();
    }

    // stock "exists" means it was ever added via POST /stocks — quantity 0 still counts
    public boolean stockExists(String name) {
        return Boolean.TRUE.equals(redis.opsForHash().hasKey(BANK_KEY, name));
    }

    public long getQuantity(String name) {
        Object val = redis.opsForHash().get(BANK_KEY, name);
        return val == null ? 0L : Long.parseLong((String) val);
    }

    // delta can be negative (sell) or positive (buy return)
    public void adjustQuantity(String name, long delta) {
        redis.opsForHash().increment(BANK_KEY, name, delta);
    }
}

