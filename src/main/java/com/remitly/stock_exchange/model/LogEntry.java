package com.remitly.stock_exchange.model;

public record LogEntry(String type, String wallet_id, String stock_name) {}