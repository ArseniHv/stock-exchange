package com.remitly.stock_exchange.dto;


import java.util.List;

import com.remitly.stock_exchange.model.StockEntry;

public record StocksRequest(List<StockEntry> stocks) {}