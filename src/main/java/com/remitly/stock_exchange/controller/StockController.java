package com.remitly.stock_exchange.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.remitly.stock_exchange.dto.StocksRequest;
import com.remitly.stock_exchange.service.BankService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StockController {

    private final BankService bankService;

    @GetMapping("/stocks")
    public Map<String, Object> getStocks() {
        return Map.of("stocks", bankService.getStocks());
    }

    @PostMapping("/stocks")
    public ResponseEntity<Void> setStocks(@RequestBody StocksRequest req) {
        bankService.setStocks(req.stocks());
        return ResponseEntity.ok().build();
    }
}