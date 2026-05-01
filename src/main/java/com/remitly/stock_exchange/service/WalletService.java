package com.remitly.stock_exchange.service;

import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.remitly.stock_exchange.model.StockEntry;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final StringRedisTemplate redis;
    private final BankService bankService;
    private final AuditService auditService;

    // Lua return codes — keep them in sync with the script below
    private static final long OK        =  0L;
    private static final long NO_STOCK  = -1L; // stock never existed in bank
    private static final long NO_SUPPLY = -2L; // bank has 0 of this stock
    private static final long NO_WALLET_STOCK = -3L; // wallet has 0 to sell

    /*
     * BUY script:
     *   KEYS[1] = bank:stocks
     *   KEYS[2] = wallet:{id}:stocks
     *   ARGV[1] = stock name
     *
     * Atomically: checks stock exists in bank, checks quantity > 0,
     * decrements bank, increments wallet.
     */
    private static final RedisScript<Long> BUY_SCRIPT = RedisScript.of("""
            local exists = redis.call('HEXISTS', KEYS[1], ARGV[1])
            if exists == 0 then return -1 end
            local qty = tonumber(redis.call('HGET', KEYS[1], ARGV[1]))
            if qty <= 0 then return -2 end
            redis.call('HINCRBY', KEYS[1], ARGV[1], -1)
            redis.call('HINCRBY', KEYS[2], ARGV[1], 1)
            return 0
            """, Long.class);

    /*
     * SELL script:
     *   KEYS[1] = bank:stocks
     *   KEYS[2] = wallet:{id}:stocks
     *   ARGV[1] = stock name
     *
     * Atomically: checks stock exists in bank, checks wallet has it,
     * decrements wallet, increments bank.
     */
    private static final RedisScript<Long> SELL_SCRIPT = RedisScript.of("""
            local exists = redis.call('HEXISTS', KEYS[1], ARGV[1])
            if exists == 0 then return -1 end
            local walletQty = tonumber(redis.call('HGET', KEYS[2], ARGV[1]))
            if walletQty == nil or walletQty <= 0 then return -3 end
            redis.call('HINCRBY', KEYS[2], ARGV[1], -1)
            redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
            return 0
            """, Long.class);

    public void buy(String walletId, String stockName) {
        String bankKey   = BankService.BANK_KEY;
        String walletKey = walletKey(walletId);

        Long result = redis.execute(BUY_SCRIPT, List.of(bankKey, walletKey), stockName);
        handleResult(result, walletId, stockName, "buy");
    }

    public void sell(String walletId, String stockName) {
        String bankKey   = BankService.BANK_KEY;
        String walletKey = walletKey(walletId);

        Long result = redis.execute(SELL_SCRIPT, List.of(bankKey, walletKey), stockName);
        handleResult(result, walletId, stockName, "sell");
    }

    private void handleResult(Long result, String walletId, String stockName, String type) {
    long code = result == null ? OK : result;
    if (code == NO_STOCK) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found: " + stockName);
    } else if (code == NO_SUPPLY) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bank has no supply of: " + stockName);
    } else if (code == NO_WALLET_STOCK) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Wallet has no stock to sell: " + stockName);
    } else {
        auditService.log(type, walletId, stockName);
    }
}

    public List<StockEntry> getWalletStocks(String walletId) {
        return redis.opsForHash().entries(walletKey(walletId))
                .entrySet().stream()
                .map(e -> new StockEntry((String) e.getKey(), Long.parseLong((String) e.getValue())))
                .toList();
    }

    public long getWalletStockQty(String walletId, String stockName) {
        Object val = redis.opsForHash().get(walletKey(walletId), stockName);
        return val == null ? 0L : Long.parseLong((String) val);
    }

    private String walletKey(String walletId) {
        return "wallet:" + walletId + ":stocks";
    }
}