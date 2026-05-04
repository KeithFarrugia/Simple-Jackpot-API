# Jackpot Service

A Spring Boot REST API for managing jackpot games. Players place bets
against a growing pot — each losing bet adds to the pool, and a winning
bet claims the entire pot.

---

## Requirements

- Docker
- Docker Compose

No local Java or Maven installation needed — everything runs inside
containers.

---

## Running the Service

### Start everything (build, test, generate reports, launch API)

```bash
docker compose up --build
```

This will run in order:

1. Start the PostgreSQL database and wait until healthy
2. Run all tests and generate the JaCoCo coverage report and Doxygen docs
3. If all tests pass, build and launch the API

### Stop everything

```bash
docker compose down
```

### Stop and wipe the database volume

```bash
docker compose down -v
```

---

## Reports

After a successful build, reports are available on your host machine at:

| Report   | Location                          |
|----------|-----------------------------------|
| JaCoCo   | `./reports/jacoco/index.html`     |
| Doxygen  | `./reports/doxygen/html/index.html` |

Open either file directly in your browser.

---

## Health Check

The API exposes a health endpoint via Spring Boot Actuator:

```
GET http://localhost:8080/actuator/health
```

Expected response when healthy:

```json
{
  "status": "UP"
}
```

---

## Swagger UI

Interactive API documentation is available at:

```
http://localhost:8080/swagger-ui/index.html
```

All endpoints can be explored and called directly from the browser.

---

## Endpoints & Example Inputs

### POST `/addjackpot` — Create or Update a Jackpot

Creates a new jackpot with the given name. If a jackpot with that name
already exists, its win probability is updated instead.

**Request body:**
```json
{
  "name": "MegaJackpot",
  "winProbability": 0.05
}
```

- `name` — display name, max 100 characters, must not be blank
- `winProbability` — chance of winning per bet, must be between 0 and 1

**Response:** the UUID of the created or updated jackpot

```json
"a3f1c2d4-e5b6-7890-abcd-ef1234567890"
```

---

### GET `/jackpots` — List All Jackpots

Returns all jackpots and their current state.

**No request body.**

**Response:**
```json
[
  {
    "id": "a3f1c2d4-e5b6-7890-abcd-ef1234567890",
    "currentSize": 250.0,
    "numWins": 3,
    "lastWin": "2026-04-30T14:22:10Z"
  }
]
```

---

### POST `/bet` — Place a Bet

Places a bet on a jackpot. Requires an `Idempotency-Key` header — a
UUID you generate to ensure duplicate requests are not processed twice.
Send the same key to replay the original result safely.

**Header:**
```
Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
```

**Request body:**
```json
{
  "jackpotId": "a3f1c2d4-e5b6-7890-abcd-ef1234567890",
  "betAmount": 10.0,
  "playerAlias": "keith"
}
```

- `jackpotId` — UUID of an existing jackpot, must not be null
- `betAmount` — amount to wager, must be positive
- `playerAlias` — player display name, max 50 characters, must not be blank

**Response:**
```json
{
  "winAmount": 0.0,
  "newSize": 260.0
}
```

- `winAmount` — amount won. Zero if the player lost
- `newSize` — current jackpot pool size after the bet

**Winning response example:**
```json
{
  "winAmount": 260.0,
  "newSize": 0.0
}
```

---

### POST `/wins` — Query Winning Bets

Returns a paginated list of winning bets. All filters are optional —
omit any field or pass null to skip that filter.

**Query params (pagination):**

| Param  | Default     | Example                   |
|--------|-------------|---------------------------|
| `page` | `0`         | `?page=0`                 |
| `size` | `10`        | `?size=5`                 |
| `sort` | `timestamp,desc` | `?sort=winAmount,desc` |

Full URL example:
```
POST http://localhost:8080/wins?page=0&size=5&sort=winAmount,desc
```

**Request body — no filters (returns all wins):**
```json
{
  "jackpotIds": null,
  "winAmounts": null,
  "playerAliases": null,
  "timeRangeList": null
}
```

**Request body — with all filters:**
```json
{
  "jackpotIds": [
    "a3f1c2d4-e5b6-7890-abcd-ef1234567890"
  ],
  "winAmounts": [
    260.0,
    500.0
  ],
  "playerAliases": [
    "keith",
    "bob"
  ],
  "timeRangeList": [
    {
      "start": "2026-01-01T00:00:00Z",
      "end":   "2026-12-31T23:59:59Z"
    }
  ]
}
```

**Response:**
```json
{
  "wins": [
    {
      "betAmount": 10.0,
      "playerAlias": "keith",
      "hasWon": true,
      "winAmount": 260.0
    }
  ],
  "currentPage": 0,
  "totalPages": 1,
  "totalWins": 1
}
```

---

## Error Responses

| Status | Meaning                                              |
|--------|------------------------------------------------------|
| `404`  | Jackpot ID not found                                 |
| `409`  | Jackpot was claimed by a concurrent request          |
| `500`  | Internal error — bet record missing for idempotency key |

---

## Default Ports

| Service   | Port   |
|-----------|--------|
| API       | `8080` |
| Database  | `5432` |