package com.remitly.stock_exchange.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.remitly.stock_exchange.service.WalletService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/wallets")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/{walletId}/stocks/{stockName}")
    public ResponseEntity<Void> trade(@PathVariable String walletId,
                                      @PathVariable String stockName,
                                      @RequestBody Map<String, String> body) {
        String type = body.get("type");
        if ("buy".equals(type)) {
            walletService.buy(walletId, stockName);
        } else if ("sell".equals(type)) {
            walletService.sell(walletId, stockName);
        } else {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{walletId}")
    public Map<String, Object> getWallet(@PathVariable String walletId) {
        return Map.of(
                "id", walletId,
                "stocks", walletService.getWalletStocks(walletId)
        );
    }

    @GetMapping("/{walletId}/stocks/{stockName}")
    public long getStockQty(@PathVariable String walletId,
                            @PathVariable String stockName) {
        return walletService.getWalletStockQty(walletId, stockName);
    }
}
