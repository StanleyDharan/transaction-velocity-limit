# Transaction Velocity Limits

A Java Spring Boot REST service that accepts or declines fund load attempts based on per-customer velocity limits. Each load request is evaluated against daily and weekly thresholds, persisted to a SQLite database, and the decision is returned to the caller.

## Velocity Limits

Each customer is subject to three constraints, evaluated per load attempt:

| Limit | Threshold |
|-------|-----------|
| Max loads per day | 3 |
| Max amount per day | $5,000 |
| Max amount per week | $20,000 |

- A "day" is defined as midnight-to-midnight UTC.
- A "week" runs Monday 00:00 UTC through Sunday 23:59:59 UTC.
- Declined loads are recorded but do not count toward any limit.
- Duplicate loads (same `id` + `customer_id`) are silently ignored.

## API

### `POST /api/loads`

**Request:**
```json
{"id": "1234", "customer_id": "1234", "load_amount": "$123.45", "time": "2018-01-01T00:00:00Z"}
```

**Accepted or declined (200 OK):**
```json
{"id": "1234", "customer_id": "1234", "accepted": true}
```

**Duplicate load (204 No Content):** No response body.

## Prerequisites

- Java 17+
- Maven 3.8+

## Getting Started

Clone the repository and use the provided Makefile:

```bash
git clone https://github.com/StanleyDharan/transaction-velocity-limit.git
cd transaction-velocity-limit
```

### Available Commands

| Command | Description |
|---------|-------------|
| `make build` | Compile and package the application (skips tests) |
| `make run` | Start the service on `http://localhost:8080` |
| `make test` | Run all tests and print a formatted report |
| `make unit-test` | Run unit tests only with report |
| `make integration-test` | Run integration tests only with report |
| `make clean` | Remove build artifacts and the SQLite database file |

### Running the Service

```bash
make run
```

Then send a load request:

```bash
curl -X POST http://localhost:8080/api/loads \
  -H "Content-Type: application/json" \
  -d '{"id":"1","customer_id":"100","load_amount":"$1000.00","time":"2024-01-15T10:00:00Z"}'
```

### Running Tests

```bash
make test
```

This runs all tests and prints a color-coded summary to the terminal:

A detailed HTML report is also generated at `target/site/surefire-report.html`.

---

## Design Choices

### SQLite with WAL Mode

SQLite was chosen as the persistence layer to keep the service self-contained with zero infrastructure dependencies -- no separate database server to install or configure. WAL (Write-Ahead Logging) mode is enabled via a connection init pragma to allow concurrent reads while a write is in progress. The HikariCP pool size is set to 1 because SQLite only supports a single writer at a time; a larger pool would just create contention.

### Composite Primary Key

The `load_transactions` table uses a composite primary key of `(id, customer_id)` rather than a surrogate key. This makes duplicate detection a simple `existsById` call against the primary key index -- no secondary index or unique constraint needed. The `@IdClass` approach was chosen over `@EmbeddedId` to keep the entity fields flat and the JPQL queries straightforward.

### Short-Circuit Evaluation

The three velocity checks (daily count, daily amount, weekly amount) are evaluated in order with short-circuit logic. Once a load is determined to be declined, the remaining queries are skipped. The cheapest check (count) runs first since it avoids a `SUM` aggregation.

### Declined Loads Are Persisted

Both accepted and declined loads are saved to the database. This ensures that a re-submitted load with the same `(id, customer_id)` is correctly detected as a duplicate regardless of whether it was originally accepted or declined. The velocity queries filter on `accepted = true` so declined loads never inflate the running totals.

### Duplicate Handling via HTTP Status

Duplicates return `204 No Content` with no body, rather than re-returning the original decision. This separates "I've already processed this" from "here is a decision," making the API idempotent without ambiguity. The `Optional.empty()` return from `LoadService.processLoad` maps cleanly to this -- the controller just checks `isEmpty()`.

### `BigDecimal` for Money

All monetary values use `BigDecimal` to avoid floating-point rounding errors. The `$` prefix is stripped during parsing, and all limit comparisons use `compareTo` rather than equality checks. This ensures that `$3000.00 + $2000.00` is exactly `$5000.00`, not `$4999.999999999999`.

### UTC Time Boundaries

Day and week boundaries are calculated in UTC using `java.time` rather than relying on the server's local timezone. This makes the service deterministic regardless of where it runs. Weeks start on Monday per ISO-8601, computed via `TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)`.

### Error Handling -- Fail Closed

The service is designed to fail closed: if a transaction cannot be reliably persisted, the customer receives an error rather than a silent acceptance. A global exception handler (`@RestControllerAdvice`) catches all exceptions and returns structured JSON error responses:

- **Validation errors (400):** Missing or malformed fields (`id`, `customer_id`, `load_amount`, `time`) are rejected before reaching the service layer. The `load_amount` field must match the `$0.00` format.
- **Malformed JSON (400):** Unparseable request bodies return a clear error message rather than a raw stack trace.
- **Database errors (500):** Any failure during persistence (connection loss, write failure, constraint violation) returns a 500 with a message prompting the customer to retry. The `@Transactional` annotation ensures partial writes are rolled back.
- **Unexpected errors (500):** A catch-all handler prevents internal details from leaking to the caller.

File writing is deliberately separated from the database transaction -- the `ResponseFileWriter` is called from the controller only after the service method (and its transaction) has committed. This prevents a scenario where the output file records a response that the database rolled back. File write failures are logged but do not fail the request, since the database is the source of truth.

### Response File Output

Every non-duplicate response is appended as a JSON line to `data/output.txt`. The output directory is configurable via the `velocity-limits.output-dir` property, defaulting to `data`. File write failures are logged but do not fail the request -- the API response is the source of truth, and the file is a secondary record.

### Database Indexing

A composite index on `(customer_id, accepted, time)` covers all three velocity check queries. SQLite uses the B-tree to jump to the customer, narrow to accepted loads, then range-scan the time window -- avoiding a full table scan on every request. The trade-off is a small write penalty (one extra B-tree update per insert) and additional disk space, but since each request triggers up to 3 read queries against only 1 write, the trade-off favours read performance.

### Test Isolation Without `@DirtiesContext`

The unit tests use `repository.deleteAll()` in a `@BeforeEach` method instead of `@DirtiesContext(classMode = BEFORE_EACH_TEST_METHOD)`. Both approaches give each test a clean database, but `deleteAll()` reuses the Spring context across all tests while `@DirtiesContext` tears down and rebuilds the entire application context for every test method. This dropped the unit test suite runtime from ~2.7s to ~1.9s.

### In-Memory SQLite for Tests

Tests use `jdbc:sqlite::memory:` with `ddl-auto=create-drop`, configured in a separate `application-test.properties` activated by `@ActiveProfiles("test")`. This keeps test runs fast and avoids leaving database files on disk. The production config uses a file-based SQLite database that survives restarts.
