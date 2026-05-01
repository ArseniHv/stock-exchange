# stock-exchange

A simplified stock market simulation REST API. Supports wallet management, bank stock control, buy/sell operations, and an audit log. Built to be highly available — killing one instance doesn't kill the service.

## Stack

- **Java 21** + Spring Boot 3.5
- **Redis** — shared state across all instances (atomic operations via Lua scripts)
- **nginx** — load balancer, retries requests on failed instances
- **Docker Compose** — orchestration

## Architecture

```
Client
  │
  ▼
nginx (port XXXX)
  │  round-robin + failover
  ├──▶ app instance 1
  ├──▶ app instance 2
  └──▶ app instance 3
          │
          ▼
        Redis
```

All app instances are stateless — Redis holds everything. When an instance is killed via `POST /chaos`, nginx automatically routes subsequent requests to the remaining instances. Docker's `restart: always` brings the killed container back up in the background.

## Startup

```bash
PORT=8080 docker compose up --build --scale app=3
```

On Windows:
```cmd
set PORT=8080 && docker compose up --build --scale app=3
```

The service will be available at `http://localhost:PORT`.

To stop:
```bash
docker compose down
```

## Endpoints

### Bank

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/stocks` | Returns current bank state |
| `POST` | `/stocks` | Replaces bank state entirely |

**POST /stocks** — request body:
```json
{
  "stocks": [
    {"name": "AAPL", "quantity": 100},
    {"name": "GOOG", "quantity": 50}
  ]
}
```

**GET /stocks** — response:
```json
{
  "stocks": [
    {"name": "AAPL", "quantity": 99},
    {"name": "GOOG", "quantity": 50}
  ]
}
```

---

### Wallets

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/wallets/{wallet_id}/stocks/{stock_name}` | Buy or sell a single stock |
| `GET` | `/wallets/{wallet_id}` | Returns all stocks in a wallet |
| `GET` | `/wallets/{wallet_id}/stocks/{stock_name}` | Returns quantity of one stock in a wallet |

**POST /wallets/{wallet_id}/stocks/{stock_name}** — request body:
```json
{"type": "buy"}
```
or
```json
{"type": "sell"}
```

Responses:
- `200` — operation succeeded
- `404` — stock was never added to the bank
- `400` — bank has no supply (buy) or wallet has no stock (sell)

Wallets are created implicitly on first operation — no separate creation step needed.

**GET /wallets/{wallet_id}** — response:
```json
{
  "id": "w1",
  "stocks": [
    {"name": "AAPL", "quantity": 2}
  ]
}
```

**GET /wallets/{wallet_id}/stocks/{stock_name}** — response:
```
2
```

---

### Audit log

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/log` | Returns all successful operations in order |

Only successful buy/sell operations are logged. Failed attempts (404, 400) are not recorded.

**GET /log** — response:
```json
{
  "log": [
    {"type": "buy", "wallet_id": "w1", "stock_name": "AAPL"},
    {"type": "sell", "wallet_id": "w1", "stock_name": "AAPL"}
  ]
}
```

---

### Chaos

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/chaos` | Kills the instance serving this request |

The service remains available because nginx routes to the other running instances. The killed container restarts automatically via Docker's `restart: always` policy.

## Quick test flow

```bash
# Seed the bank
curl -X POST http://localhost:8080/stocks \
  -H "Content-Type: application/json" \
  -d '{"stocks":[{"name":"AAPL","quantity":10}]}'

# Buy
curl -X POST http://localhost:8080/wallets/w1/stocks/AAPL \
  -H "Content-Type: application/json" \
  -d '{"type":"buy"}'

# Check wallet
curl http://localhost:8080/wallets/w1

# Check log
curl http://localhost:8080/log

# Kill an instance — service should stay up
curl -X POST http://localhost:8080/chaos
curl http://localhost:8080/stocks
```