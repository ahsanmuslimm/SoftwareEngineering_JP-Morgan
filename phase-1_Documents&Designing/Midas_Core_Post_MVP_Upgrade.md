# 🏗️ Midas Core — Post-MVP Upgrade Roadmap
## From MVP → Full Fledged Working Prototype
### JP Morgan Chase — Software Engineering Job Simulation

> **Document Type**: Post-MVP Upgrade Plan + Full Product Test Suite  
> **Picks Up From**: `Midas_Core_MVP_Integration.md` (MVP Certified)  
> **Addresses**: Post-MVP Hardening Items PH-01 through PH-10  
> **Final Product Name**: **Midas Core — Full Fledged Working Prototype (FFWP)**  
> **Author Date**: September 2026  
> **Status**: Phase 1 — Design & Planning

---

## How This Document Fits the Journey

```
Phase 1 — Design & Planning (Documents)
│
├── Project_Understanding_Draft.md       ← System understanding
├── Midas_Core_Module_Decomposition.md   ← 5 modules + prototype gates
├── Midas_Core_MVP_Integration.md        ← MVP integration + 32 integration tests
└── Midas_Core_Post_MVP_Upgrade.md       ← YOU ARE HERE
         │
         ▼
Phase 2 — Implementation
         │
         ▼
Phase 3 — Full Fledged Working Prototype (FFWP) CERTIFIED
```

---

## The Upgrade Journey at a Glance

```
MVP (Certified)
    │
    ├──► UP-1: Core Business Correctness
    │         BigDecimal precision + zero-amount rule + idempotency
    │         ↓ Gate: UP-1 test suite passes
    │
    ├──► UP-2: Resilience & External Safety
    │         Circuit breaker + incentive bounds check + dead-letter queue
    │         ↓ Gate: UP-2 test suite passes
    │
    ├──► UP-3: Security & Auth Layer
    │         Spring Security + API key authentication on /balance
    │         ↓ Gate: UP-3 test suite passes
    │
    ├──► UP-4: Observability & Diagnostics
    │         Structured logging + Micrometer metrics + health endpoint
    │         ↓ Gate: UP-4 test suite passes
    │
    └──► UP-5: Infrastructure & CI/CD Readiness
              PostgreSQL swap + GitHub Actions + Docker
              ↓ Gate: UP-5 test suite passes
                       │
                       ▼
              FULL FLEDGED WORKING PROTOTYPE (FFWP)
              Full Product Test Suite (FP-1 to FP-5) PASS
              20 Final Acceptance Criteria MET
```

> Each upgrade is **sequential** and **additive** — each layer builds on the previous one.  
> No upgrade phase begins until the previous gate passes 100%.

---

## Pre-Condition: MVP Must Be Certified

Before any upgrade phase begins, the following must be confirmed:

| Check | Document | Status |
|-------|----------|--------|
| All 5 module prototype gates passed | `Midas_Core_Module_Decomposition.md` | Must be PROTOTYPE |
| All 32 MVP integration tests pass | `Midas_Core_MVP_Integration.md` | Must be 100% PASS |
| All 12 MVP Acceptance Criteria met | `Midas_Core_MVP_Integration.md` (AC-01–AC-12) | Must all be VERIFIED |
| `mvn clean install` returns BUILD SUCCESS | Local machine | Must be GREEN |

> If any pre-condition is not met — stop. Fix the MVP first.

---

---

## UPGRADE PHASE 1 — Core Business Correctness
> **Gap IDs Addressed**: PH-02, PH-03, PH-07  
> **Effort Estimate**: 1.5 days  
> **Priority**: CRITICAL — financial correctness issues

### Why This Phase Comes First

The MVP works — but it contains three correctness flaws that would be **unacceptable in any real financial system**:
1. `double` arithmetic has floating-point rounding errors (0.1 + 0.2 ≠ 0.3 in IEEE 754)
2. A transaction of `amount = 0.0` passes all validation rules — meaningless but accepted
3. Kafka can redeliver the same message — the MVP processes it twice (double debit/credit)

These are not "nice to haves." They are defects.

---

### UP-1A: Monetary Precision — Replace `double` with `BigDecimal` (PH-03)

**Addresses Risk**: R-03 — `double` used for financial amounts

Every `amount` and `balance` field in the system must be migrated from `double` to `BigDecimal`.

#### Files to Modify

| File | Change |
|------|--------|
| `domain/User.java` | `balance` field: `double` → `BigDecimal` |
| `domain/Transaction.java` | `amount` field: `double` → `BigDecimal` *(check if scaffold allows; if locked, wrap at service boundary)* |
| `domain/Incentive.java` | `amount` field: `double` → `BigDecimal` |
| `domain/Balance.java` | `amount` field: `double` → `BigDecimal` |
| `TransactionRecord.java` | `amount` + `incentive` fields: `double` → `BigDecimal` |
| `TransactionService.java` | All arithmetic uses `BigDecimal.add()`, `subtract()` — NOT `+`, `-` operators |

#### Arithmetic Migration Pattern

```java
// BEFORE (MVP — floating-point risk):
recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive.getAmount());
sender.setBalance(sender.getBalance() - transaction.getAmount());

// AFTER (FFWP — precision safe):
recipient.setBalance(
    recipient.getBalance()
        .add(transaction.getAmount())
        .add(incentive.getAmount())
);
sender.setBalance(
    sender.getBalance()
        .subtract(transaction.getAmount())
);
```

#### JPA Configuration for BigDecimal

```java
// In User.java:
@Column(precision = 19, scale = 4)  // 19 digits total, 4 decimal places
private BigDecimal balance;

// In TransactionRecord.java:
@Column(precision = 19, scale = 4)
private BigDecimal amount;

@Column(precision = 19, scale = 4)
private BigDecimal incentive;
```

> **Note**: If `Transaction.java` is scaffold-locked and cannot be changed, wrap the `double` to `BigDecimal` at the service layer boundary: `BigDecimal.valueOf(transaction.getAmount())`

---

### UP-1B: Zero-Amount Validation Rule — VR-04 (PH-07)

**Addresses Risk**: R-09 — zero or negative transaction amount not rejected

Add a fourth validation rule to `TransactionService`:

| Rule ID | Rule | Failure Action |
|---------|------|----------------|
| **VR-04** | `transaction.amount > 0` (must be strictly positive) | Discard transaction |

#### Implementation in TransactionService

```java
// VR-04: Amount must be strictly positive
if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
    log.warn("Transaction rejected — amount is zero or negative: {}", transaction.getAmount());
    return; // discard
}
```

---

### UP-1C: Idempotency — Deduplication on Kafka Redelivery (PH-02)

**Addresses Risk**: R-02 — Kafka redelivery causes double-processing

When Kafka redelivers a message (network blip, consumer restart), the MVP processes it again — double debiting the sender, double crediting the recipient. This is catastrophic for a financial system.

#### Implementation Steps

**Step 1**: Add a `transactionId` field to the `Transaction` domain class (or use a composite key of `senderId + recipientId + amount + timestamp`).

```java
// In domain/Transaction.java (if modifiable) or generate at service boundary:
private String transactionId;  // UUID, assigned by producer
```

**Step 2**: Add a `transactionId` field to `TransactionRecord`:

```java
@Column(unique = true)  // database-level uniqueness constraint
private String transactionId;
```

**Step 3**: Check for duplicate before processing in `TransactionService`:

```java
// Idempotency check — before any validation
if (transactionRecordRepository.existsByTransactionId(transaction.getTransactionId())) {
    log.warn("Duplicate transaction detected and skipped: {}", transaction.getTransactionId());
    return; // idempotent skip — not an error
}
```

**Step 4**: Add repository method:
```java
// In TransactionRecordRepository.java:
boolean existsByTransactionId(String transactionId);
```

---

### UP-1 — New Files & Modified Files

| File | Action | Change |
|------|--------|--------|
| `domain/User.java` | MODIFY | `balance`: double → BigDecimal |
| `domain/Transaction.java` | MODIFY (if allowed) | `amount`: double → BigDecimal; add `transactionId` |
| `domain/Incentive.java` | MODIFY | `amount`: double → BigDecimal |
| `domain/Balance.java` | MODIFY | `amount`: double → BigDecimal |
| `TransactionRecord.java` | MODIFY | `amount`/`incentive`: double → BigDecimal; add `transactionId` (unique) |
| `TransactionService.java` | MODIFY | BigDecimal arithmetic; add VR-04; add idempotency check |
| `TransactionRecordRepository.java` | MODIFY | Add `existsByTransactionId()` method |

---

### UP-1 Proprietary Test Suite

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| UP1-01 | BigDecimal Storage | `User.balance` and `TransactionRecord.amount` stored as `BigDecimal` | No `ClassCastException`; H2 stores with correct precision |
| UP1-02 | Floating-Point Precision Safe | 0.1 + 0.2 == 0.3 after BigDecimal arithmetic | `BigDecimal("0.1").add(BigDecimal("0.2"))` equals `BigDecimal("0.3")` exactly |
| UP1-03 | Balance Arithmetic Precision | Complex multi-transaction balance is exact | Final balance matches expected value to 4 decimal places — no rounding drift |
| UP1-04 | VR-04 Rejects Zero Amount | Transaction with `amount = 0.0` is discarded | No `TransactionRecord` persisted; no balance change |
| UP1-05 | VR-04 Rejects Negative Amount | Transaction with `amount = -50.00` is discarded | No `TransactionRecord` persisted; no balance change |
| UP1-06 | VR-04 Passes Positive Amount | Transaction with `amount = 0.01` is accepted | Transaction proceeds through full pipeline |
| UP1-07 | Idempotency — Duplicate Skipped | Same transaction ID sent twice | Second message silently skipped; only 1 `TransactionRecord` row; balances mutated once only |
| UP1-08 | Idempotency — Different ID Processed | Two messages with different IDs | Both processed; 2 `TransactionRecord` rows; balances mutated twice |
| UP1-09 | Existing Tests Still Pass | Full regression after UP-1 changes | `mvn clean install` — all `TaskOneTests` through `TaskFiveTests` still PASS |

**Run Command**:
```bash
mvn clean install
mvn test -Dtest=UpgradeOneTests   # new test class for UP-1 verification
```

---

### UP-1 Upgrade Gate

> **UP-1 is complete when ALL of the following are true:**

- [ ] All `double` fields migrated to `BigDecimal` — no `double` in financial fields anywhere
- [ ] VR-04 (`amount > 0`) added and verified — zero and negative amounts rejected
- [ ] Idempotency check present — duplicate `transactionId` causes silent skip, not error
- [ ] `TransactionRecord.transactionId` has `@Column(unique = true)` constraint
- [ ] UP-1 test suite (UP1-01 through UP1-09) passes 100%
- [ ] All previous MVP tests (TaskOne through TaskFive) still pass — no regressions

---
---

## UPGRADE PHASE 2 — Resilience & External Safety
> **Gap IDs Addressed**: PH-04, PH-06, PH-08  
> **Effort Estimate**: 1.5 days  
> **Priority**: HIGH — system stability and external dependency risk

### Why This Phase Comes Second

The MVP has a **single point of external failure**: the Incentive API. If it is slow, down, or returns a malformed response:
- In the MVP: the entire Kafka listener thread blocks or crashes — **all transaction processing halts**
- In the FFWP: the system degrades gracefully, retries intelligently, and routes failures safely

---

### UP-2A: Circuit Breaker — Resilience4j (PH-04)

**Addresses Risk**: R-01 — Incentive API unavailability blocks entire pipeline

Add the Resilience4j Spring Boot starter to `pom.xml`:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

Wrap `IncentiveClient.getIncentive()` with a circuit breaker + timeout + fallback:

```java
@Component
public class IncentiveClient {

    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";
    private static final BigDecimal FALLBACK_INCENTIVE = BigDecimal.ZERO;

    @CircuitBreaker(name = "incentiveApi", fallbackMethod = "fallbackIncentive")
    @TimeLimiter(name = "incentiveApi")
    @Retry(name = "incentiveApi")
    public Incentive getIncentive(Transaction transaction) {
        return restTemplate.postForObject(INCENTIVE_URL, transaction, Incentive.class);
    }

    // Fallback: if Incentive API is down, apply zero incentive and continue
    public Incentive fallbackIncentive(Transaction transaction, Throwable ex) {
        log.warn("Incentive API unavailable — applying zero incentive. Cause: {}", ex.getMessage());
        return new Incentive(FALLBACK_INCENTIVE);
    }
}
```

#### Resilience4j Configuration in `application.yml`

```yaml
resilience4j:
  circuitbreaker:
    instances:
      incentiveApi:
        slidingWindowSize: 10
        failureRateThreshold: 50        # open circuit after 50% failure rate
        waitDurationInOpenState: 10s    # wait 10s before half-open retry
        permittedNumberOfCallsInHalfOpenState: 3
  timelimiter:
    instances:
      incentiveApi:
        timeoutDuration: 3s             # 3-second timeout per call
  retry:
    instances:
      incentiveApi:
        maxAttempts: 3
        waitDuration: 500ms
```

---

### UP-2B: Incentive Response Bounds Check (PH-08)

**Addresses Risk**: R-10 — malformed or out-of-bounds incentive applied without sanity check

Add a validation step in `TransactionService` after receiving the incentive:

```java
// After IncentiveClient.getIncentive() returns:
private static final BigDecimal MAX_INCENTIVE_AMOUNT = new BigDecimal("10000.00");

private Incentive validateIncentive(Incentive incentive, Transaction tx) {
    if (incentive == null || incentive.getAmount() == null) {
        log.warn("Null incentive received — defaulting to zero");
        return new Incentive(BigDecimal.ZERO);
    }
    if (incentive.getAmount().compareTo(BigDecimal.ZERO) < 0) {
        log.warn("Negative incentive received — defaulting to zero");
        return new Incentive(BigDecimal.ZERO);
    }
    if (incentive.getAmount().compareTo(MAX_INCENTIVE_AMOUNT) > 0) {
        log.warn("Incentive {} exceeds max bound {} — capping", incentive.getAmount(), MAX_INCENTIVE_AMOUNT);
        return new Incentive(MAX_INCENTIVE_AMOUNT);
    }
    return incentive;
}
```

---

### UP-2C: Dead-Letter Queue for Kafka Failures (PH-06)

**Addresses Risk**: R-06 — no visibility into failed/invalid Kafka messages

Configure a Dead-Letter Topic (DLT) so that messages that cause exceptions in the listener are not silently dropped:

```java
// In KafkaListener.java:
@KafkaListener(topics = "${general.kafka-topic}")
public void listen(Transaction transaction) {
    try {
        transactionService.process(transaction);
    } catch (Exception e) {
        log.error("Failed to process transaction — routing to DLT: {}", e.getMessage());
        // Spring Kafka's DefaultErrorHandler will route to DLT automatically
        throw e;
    }
}
```

#### Dead-Letter Topic Configuration in `application.yml`

```yaml
spring:
  kafka:
    consumer:
      group-id: midas-core-group
    listener:
      ack-mode: RECORD    # acknowledge each record individually
```

```java
// In KafkaConfig.java (new file):
@Configuration
public class KafkaConfig {

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition("trader-updates.DLT", record.partition()));
        return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3));
    }
}
```

---

### UP-2 — New Files & Modified Files

| File | Action | Change |
|------|--------|--------|
| `pom.xml` | MODIFY | Add Resilience4j dependency |
| `IncentiveClient.java` | MODIFY | Add `@CircuitBreaker`, `@TimeLimiter`, `@Retry`, fallback method |
| `TransactionService.java` | MODIFY | Add `validateIncentive()` bounds check after API call |
| `KafkaConfig.java` | CREATE | Dead-letter error handler + DLT configuration bean |
| `application.yml` | MODIFY | Add `resilience4j` config block + Kafka consumer config |

---

### UP-2 Proprietary Test Suite

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| UP2-01 | Circuit Breaker Opens on Failures | Incentive API returns errors 5+ times | Circuit opens; subsequent calls go directly to fallback; no HTTP calls made |
| UP2-02 | Fallback Returns Zero Incentive | Circuit is open / API unreachable | `TransactionRecord.incentive = 0.0`; transaction still persisted with zero incentive |
| UP2-03 | Pipeline Continues When API Down | Incentive API is stopped mid-run | Transactions still processed (with zero incentive fallback); no listener crash |
| UP2-04 | Timeout Enforced | Incentive API responds after 5s (> 3s timeout) | Call times out within 3s; fallback invoked; no thread hang |
| UP2-05 | Retry Attempted Before Fallback | Incentive API fails twice then succeeds | 3 retry attempts visible in logs; third attempt succeeds; correct incentive applied |
| UP2-06 | Negative Incentive Clamped to Zero | API returns `{ "amount": -100.00 }` | System applies `0.00` incentive; no negative credit to recipient |
| UP2-07 | Oversized Incentive Capped | API returns `{ "amount": 99999999.00 }` | System applies `10000.00` (MAX_INCENTIVE_AMOUNT); warning log emitted |
| UP2-08 | Null Incentive Response Handled | API returns null body | Fallback zero incentive applied; no `NullPointerException` |
| UP2-09 | Dead-Letter Topic Receives Failures | Malformed Kafka message sent | Message routed to `trader-updates.DLT`; not silently dropped; DLT record verifiable |
| UP2-10 | Existing Tests Still Pass | Full regression after UP-2 changes | `mvn clean install` — all prior tests still PASS |

**Run Command**:
```bash
mvn clean install
mvn test -Dtest=UpgradeTwoTests
```

---

### UP-2 Upgrade Gate

> **UP-2 is complete when ALL of the following are true:**

- [ ] Circuit breaker opens after configurable failure threshold
- [ ] Fallback method returns zero-incentive `Incentive` — transaction pipeline never halts due to Incentive API failure
- [ ] Timeout enforced at 3 seconds — no thread blocks beyond this limit
- [ ] Retry attempts 3 times before invoking fallback
- [ ] Negative incentive returns → clamped to `0.00`
- [ ] Incentive exceeding `MAX_INCENTIVE_AMOUNT` → capped with warning
- [ ] Dead-letter topic receives unprocessable messages — nothing silently dropped
- [ ] UP-2 test suite (UP2-01 through UP2-10) passes 100%
- [ ] All previous tests still pass — no regressions

---
---

## UPGRADE PHASE 3 — Security & Authentication
> **Gap ID Addressed**: PH-01  
> **Effort Estimate**: 1 day  
> **Priority**: HIGH — currently anyone can query any user's balance

### Why This Phase Comes Third

The MVP's `GET /balance` endpoint is completely **unauthenticated** — any caller, internal or external, can query the balance of any user by simply knowing their ID. This is a critical security gap for a financial service.

---

### UP-3: API Key Authentication on GET /balance (PH-01)

**Addresses Risk**: R-04 — no authentication or authorization

#### Approach: API Key Header Authentication (simpler than JWT for this scope)

Add Spring Security to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>3.2.5</version>
</dependency>
```

#### New Files to Create

**`ApiKeyAuthFilter.java`** — Custom filter that reads `X-API-Key` header:

```java
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    @Value("${security.api-key}")
    private String validApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
                                    throws ServletException, IOException {
        String apiKey = request.getHeader("X-API-Key");
        if (validApiKey.equals(apiKey)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Unauthorized — invalid or missing API key\"}");
        }
    }
}
```

**`SecurityConfig.java`** — Disable CSRF for REST; register filter:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyFilter)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/balance").authenticated()
                .anyRequest().denyAll()
            );
        return http.build();
    }
}
```

#### Updated `application.yml`

```yaml
security:
  api-key: ${MIDAS_API_KEY:midas-dev-key-2026}   # override with env var in production
```

#### Updated API Contract (FFWP)

```
GET http://localhost:33400/balance?userId={userId}
Headers:
  X-API-Key: midas-dev-key-2026

Success (200 OK):       { "userId": "waldorf", "amount": 400.0000 }
Unauthorized (401):     { "error": "Unauthorized — invalid or missing API key" }
```

---

### UP-3 — New Files & Modified Files

| File | Action | Change |
|------|--------|--------|
| `pom.xml` | MODIFY | Add `spring-boot-starter-security` |
| `ApiKeyAuthFilter.java` | CREATE | Custom `OncePerRequestFilter` checking `X-API-Key` header |
| `SecurityConfig.java` | CREATE | Spring Security config — stateless, API key filter registered |
| `application.yml` | MODIFY | Add `security.api-key` config property |

---

### UP-3 Proprietary Test Suite

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| UP3-01 | Valid API Key Accepted | `GET /balance` with correct `X-API-Key` | Returns `200 OK` with balance JSON |
| UP3-02 | Missing API Key Rejected | `GET /balance` with no header | Returns `401 Unauthorized` |
| UP3-03 | Wrong API Key Rejected | `GET /balance` with `X-API-Key: wrong-key` | Returns `401 Unauthorized` |
| UP3-04 | Empty API Key Rejected | `GET /balance` with `X-API-Key:` (empty) | Returns `401 Unauthorized` |
| UP3-05 | Auth Failure Returns JSON Error Body | `401` response inspected | Response body: `{ "error": "Unauthorized..." }` — not an HTML error page |
| UP3-06 | Kafka Listener Unaffected by Security | Security config does not block Kafka consumer | Kafka listener still processes transactions normally; not intercepted by filter |
| UP3-07 | API Key Externalized via Config | Key value is not hard-coded in source | Key read from `security.api-key` in `application.yml`; overridable via env var |
| UP3-08 | Existing Tests Updated for Auth | TaskFiveTests now include API key header | All `TaskFiveTests` still pass after adding `X-API-Key` to test HTTP calls |

**Run Command**:
```bash
mvn clean install
mvn test -Dtest=UpgradeThreeTests
```

---

### UP-3 Upgrade Gate

> **UP-3 is complete when ALL of the following are true:**

- [ ] `GET /balance` without valid `X-API-Key` returns `401 Unauthorized` — not `200`
- [ ] `GET /balance` with valid API key works correctly — balance returned
- [ ] API key is externalized in `application.yml` — NOT hard-coded in source
- [ ] Kafka listener is **unaffected** by Spring Security configuration
- [ ] `TaskFiveTests` updated to include API key header and still pass
- [ ] UP-3 test suite (UP3-01 through UP3-08) passes 100%
- [ ] All previous tests still pass — no regressions

---
---

## UPGRADE PHASE 4 — Observability & Diagnostics
> **Gap ID Addressed**: PH-05  
> **Effort Estimate**: 0.5 day  
> **Priority**: MEDIUM — essential for incident diagnosis and operational visibility

### Why This Phase Comes Fourth

A system with no structured logging or metrics is a **black box in production**. When something goes wrong, there is no way to diagnose what happened, how long it took, or which transactions were affected. This phase adds professional-grade observability.

---

### UP-4A: Structured Logging with SLF4J (PH-05)

Add structured log statements to every critical processing step:

```java
// In TransactionService.java:
private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

public void process(Transaction transaction) {
    log.info("TRANSACTION RECEIVED | id={} sender={} recipient={} amount={}",
        transaction.getTransactionId(),
        transaction.getSenderId(),
        transaction.getRecipientId(),
        transaction.getAmount()
    );

    // VR-01
    if (sender is not found) {
        log.warn("TRANSACTION REJECTED | id={} reason=UNKNOWN_SENDER sender={}",
            transaction.getTransactionId(), transaction.getSenderId());
        return;
    }
    // ... VR-02, VR-03, VR-04 similarly logged

    log.info("TRANSACTION VALID | id={} — proceeding to incentive enrichment",
        transaction.getTransactionId());

    // After incentive:
    log.info("INCENTIVE APPLIED | id={} incentiveAmount={} recipientNewBalance={}",
        transaction.getTransactionId(), incentive.getAmount(), recipient.getBalance());

    log.info("TRANSACTION PERSISTED | id={} recordId={}",
        transaction.getTransactionId(), savedRecord.getId());
}
```

#### Log Levels by Event

| Level | Events |
|-------|--------|
| `INFO` | Transaction received, valid, persisted, incentive applied |
| `WARN` | Transaction rejected (any VR), duplicate skipped, incentive clamped/zeroed |
| `ERROR` | Unhandled exceptions, Incentive API failures beyond retry |
| `DEBUG` | Detailed balance values before/after, intermediate computation steps |

---

### UP-4B: Metrics with Micrometer + Actuator (PH-05)

Add Spring Boot Actuator + Micrometer to `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
    <version>3.2.5</version>
</dependency>
```

Register custom metrics counters in `TransactionService`:

```java
@Component
public class TransactionService {

    private final Counter transactionsReceived;
    private final Counter transactionsValid;
    private final Counter transactionsRejected;
    private final Counter duplicatesSkipped;
    private final Counter incentiveApiFallbacks;

    public TransactionService(MeterRegistry registry) {
        this.transactionsReceived  = registry.counter("midas.transactions.received");
        this.transactionsValid     = registry.counter("midas.transactions.valid");
        this.transactionsRejected  = registry.counter("midas.transactions.rejected");
        this.duplicatesSkipped     = registry.counter("midas.transactions.duplicates");
        this.incentiveApiFallbacks = registry.counter("midas.incentive.fallbacks");
    }

    public void process(Transaction transaction) {
        transactionsReceived.increment();
        // ... on rejection:
        transactionsRejected.increment();
        // ... on valid:
        transactionsValid.increment();
    }
}
```

#### Actuator Endpoints Exposed

```yaml
# In application.yml:
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, info
  endpoint:
    health:
      show-details: always
```

| Endpoint | URL | Purpose |
|----------|-----|---------|
| Health | `GET /actuator/health` | Service up/down + DB connectivity |
| Metrics | `GET /actuator/metrics/midas.transactions.received` | Transaction throughput counter |
| Info | `GET /actuator/info` | Version, build info |

---

### UP-4 — New Files & Modified Files

| File | Action | Change |
|------|--------|--------|
| `pom.xml` | MODIFY | Add `spring-boot-starter-actuator` |
| `TransactionService.java` | MODIFY | Add SLF4J log statements at every decision point; inject `MeterRegistry`; register counters |
| `IncentiveClient.java` | MODIFY | Add log statements around API call; increment fallback counter |
| `KafkaListener.java` | MODIFY | Add log statement on message receipt |
| `BalanceController.java` | MODIFY | Add log statement on balance query |
| `application.yml` | MODIFY | Add `management.endpoints` config block |

---

### UP-4 Proprietary Test Suite

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| UP4-01 | INFO Log on Transaction Received | Valid transaction processed | Log line with level INFO and `TRANSACTION RECEIVED` visible |
| UP4-02 | WARN Log on Transaction Rejected | Invalid transaction (VR fail) | Log line with level WARN and `TRANSACTION REJECTED` + reason visible |
| UP4-03 | WARN Log on Duplicate Skipped | Duplicate transactionId submitted | Log line with level WARN and `DUPLICATE` visible |
| UP4-04 | ERROR Log on Incentive API Failure | Incentive API unreachable beyond retries | Log line with level ERROR visible; circuit breaker fallback invoked |
| UP4-05 | Health Endpoint Returns UP | `GET /actuator/health` | `{ "status": "UP" }` with DB indicator shown |
| UP4-06 | Received Counter Increments | 10 transactions processed | `midas.transactions.received` counter = 10 via `/actuator/metrics` |
| UP4-07 | Valid Counter Increments | 7 of 10 transactions valid | `midas.transactions.valid` counter = 7 |
| UP4-08 | Rejected Counter Increments | 3 of 10 transactions invalid | `midas.transactions.rejected` counter = 3 |
| UP4-09 | Existing Tests Still Pass | Full regression | `mvn clean install` — all prior tests still PASS |

---

### UP-4 Upgrade Gate

> **UP-4 is complete when ALL of the following are true:**

- [ ] Every transaction decision point (received, valid, rejected, duplicate, persisted) has a structured log line
- [ ] Log levels are correct: INFO for normal flow, WARN for business rejects, ERROR for system failures
- [ ] `/actuator/health` returns `{ "status": "UP" }` with H2 DB indicator
- [ ] `midas.transactions.received`, `midas.transactions.valid`, `midas.transactions.rejected` counters visible at `/actuator/metrics`
- [ ] UP-4 test suite (UP4-01 through UP4-09) passes 100%
- [ ] All previous tests still pass — no regressions

---
---

## UPGRADE PHASE 5 — Infrastructure & CI/CD Readiness
> **Gap IDs Addressed**: PH-09, PH-10  
> **Effort Estimate**: 1 day  
> **Priority**: MEDIUM — production infrastructure readiness

### Why This Phase Comes Last

This phase is the **final structural upgrade** — it replaces the development-only infrastructure (H2, manual test runs) with production-grade equivalents (PostgreSQL, automated CI). It is last because everything before it must be correct before the infrastructure is hardened.

---

### UP-5A: Database Migration — H2 to PostgreSQL (PH-09)

**Addresses**: Architectural gap — H2 is not a production database

#### JPA Abstraction Makes This Trivial

Because the MVP correctly used Spring Data JPA with `@Entity` annotations and `ddl-auto`, the swap from H2 to PostgreSQL requires **zero business logic changes** — only configuration changes.

Add PostgreSQL driver to `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.3</version>
</dependency>
```

Update `application.yml` (create a `application-prod.yml` profile):

```yaml
# application-prod.yml (production profile — do NOT commit credentials)
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:midas}
    username: ${DB_USER:midas_user}
    password: ${DB_PASSWORD:changeme}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate    # production: validate schema, never auto-drop
```

```yaml
# application.yml (development profile — unchanged)
spring:
  datasource:
    url: jdbc:h2:mem:testdb   # keep H2 for local dev and unit tests
    driver-class-name: org.h2.Driver
```

> **Design decision**: Keep H2 for dev/test (speed), PostgreSQL for prod (persistence). Spring profiles handle the switch. The JPA abstraction we built in M3 makes this work without touching a single line of business logic.

---

### UP-5B: CI/CD Pipeline — GitHub Actions (PH-10)

**Addresses**: Operational gap — no automated test-on-push

#### Create `.github/workflows/ci.yml`

```yaml
name: Midas Core CI Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: midas_test
          POSTGRES_USER: midas_user
          POSTGRES_PASSWORD: testpassword
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Cache Maven dependencies
        uses: actions/cache@v4
        with:
          path: ~/.m2
          key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}

      - name: Build and run unit + integration tests
        run: mvn clean install
        env:
          MIDAS_API_KEY: ci-test-api-key-2026

      - name: Report test results
        if: always()
        uses: dorny/test-reporter@v1
        with:
          name: Maven Test Results
          path: target/surefire-reports/*.xml
          reporter: java-junit
```

---

### UP-5C: Docker Containerization

Create a `Dockerfile` for Midas Core:

```dockerfile
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR
COPY target/midas-core-*.jar app.jar

# Expose the REST API port
EXPOSE 33400

# Run with production profile
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
```

Create `docker-compose.yml` for local full-stack development:

```yaml
version: '3.9'

services:
  midas-core:
    build: .
    ports:
      - "33400:33400"
    environment:
      - DB_HOST=postgres
      - DB_NAME=midas
      - DB_USER=midas_user
      - DB_PASSWORD=midas_pass
      - MIDAS_API_KEY=dev-key-2026
    depends_on:
      - postgres
      - incentive-api

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: midas
      POSTGRES_USER: midas_user
      POSTGRES_PASSWORD: midas_pass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

  incentive-api:
    image: openjdk:17-jre-alpine
    command: java -jar /app/incentive-api.jar
    volumes:
      - ./services:/app
    ports:
      - "8080:8080"

volumes:
  postgres_data:
```

---

### UP-5 — New Files & Modified Files

| File | Action | Change |
|------|--------|--------|
| `pom.xml` | MODIFY | Add PostgreSQL driver dependency |
| `application.yml` | MODIFY | Keep H2 for dev; split prod config |
| `application-prod.yml` | CREATE | PostgreSQL datasource config with env var injection |
| `.github/workflows/ci.yml` | CREATE | GitHub Actions CI pipeline |
| `Dockerfile` | CREATE | Container image definition for Midas Core |
| `docker-compose.yml` | CREATE | Full local stack: Midas Core + PostgreSQL + Incentive API |

---

### UP-5 Proprietary Test Suite

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| UP5-01 | H2 Dev Profile Still Works | Run with default `application.yml` | `mvn clean install` passes using H2; no change in behavior |
| UP5-02 | PostgreSQL Prod Profile Activates | `--spring.profiles.active=prod` | Application starts with PostgreSQL connection; no H2 in logs |
| UP5-03 | JPA Schema Valid on PostgreSQL | Start with `ddl-auto: validate` + PostgreSQL | Application starts without `SchemaManagementException` |
| UP5-04 | Full Business Logic Works on PostgreSQL | Run integration tests against PostgreSQL | All 32 MVP integration tests pass against PostgreSQL datasource |
| UP5-05 | GitHub Actions CI Triggers | Push to `main` branch | Workflow runs; all tests pass; green checkmark on commit |
| UP5-06 | Docker Image Builds | `docker build .` | Image created without error; tagged correctly |
| UP5-07 | Docker Compose Stack Starts | `docker compose up` | All 3 services healthy; Midas Core responds at `localhost:33400` |
| UP5-08 | Containerized E2E Works | Full pipeline test via Docker Compose | `GET /balance` returns correct result through containerized stack |

---

### UP-5 Upgrade Gate

> **UP-5 is complete when ALL of the following are true:**

- [ ] H2 remains the default for local dev and unit tests — zero test speed regression
- [ ] `application-prod.yml` profile switches to PostgreSQL with env var injection — no credentials in source
- [ ] `ddl-auto: validate` (not `create-drop`) in production profile
- [ ] GitHub Actions CI pipeline runs and passes on every push to `main`
- [ ] `Dockerfile` builds a working image
- [ ] `docker-compose up` brings up the full stack — Midas Core, PostgreSQL, Incentive API
- [ ] UP-5 test suite (UP5-01 through UP5-08) passes 100%
- [ ] All previous tests still pass — no regressions

---
---

## Full Product Test Suite (FFWP Certification)

> This is the **final comprehensive test run** that certifies Midas Core as a Full Fledged Working Prototype.  
> It is run **after all 5 upgrade phases** pass their individual gates.  
> It covers the entire system end-to-end — from the original 5 task tests through all 5 upgrade layers.

### Test Suite Pyramid

```
                    ┌──────────────────────────┐
                    │  FP-5 Production Readiness│  8 tests
                    │  (Docker, CI, PostgreSQL) │
                    └──────────────┬───────────┘
               ┌──────────────────┼──────────────────┐
               │  FP-4 Observability                  │  9 tests
               │  (Logs, Metrics, Health)             │
               └──────────────────┬───────────────────┘
          ┌───────────────────────┼───────────────────────┐
          │  FP-3 Security                                 │  8 tests
          │  (API Key auth on /balance)                   │
          └───────────────────────┬────────────────────────┘
     ┌────────────────────────────┼────────────────────────────┐
     │  FP-2 Resilience                                        │  10 tests
     │  (Circuit breaker, DLT, bounds check)                  │
     └────────────────────────────┬────────────────────────────┘
┌─────────────────────────────────┼─────────────────────────────────┐
│  FP-1 Core Correctness                                            │  9 tests
│  (BigDecimal, VR-04, Idempotency)                                │
└─────────────────────────────────┬─────────────────────────────────┘
                     ┌────────────┴──────────────────┐
                     │  Original: Module Tests (T-xx) │  20 tests
                     │  + MVP Integration Tests (IT-xx)│  32 tests
                     └──────────────────────────────────┘

TOTAL:  20 (module) + 32 (MVP) + 9 + 10 + 8 + 9 + 8 = 96 tests
```

### Final Execution Command

```bash
# Step 1: Ensure Incentive API is running
java -jar services/incentive-api.jar

# Step 2: Full regression — all 96 tests
mvn clean install

# Step 3: Run specific upgrade test classes
mvn test -Dtest="TaskOneTests+TaskTwoTests+TaskThreeTests+TaskFourTests+TaskFiveTests"
mvn test -Dtest="UpgradeOneTests+UpgradeTwoTests+UpgradeThreeTests+UpgradeFourTests+UpgradeFiveTests"
mvn test -Dtest="FullProductTests"
```

---

## Full Product Acceptance Criteria (20 Criteria)

> **Midas Core is a Full Fledged Working Prototype** when ALL 20 criteria are verified:

| ID | Criterion | Source | Status |
|----|-----------|--------|--------|
| **MVP Inherited (AC-01–AC-12)** | | | |
| AC-01 | All 5 module prototypes verified | MVP Doc | [ ] |
| AC-02 | Full build passes (`mvn clean install` BUILD SUCCESS) | MVP Doc | [ ] |
| AC-03 | End-to-end pipeline works (Kafka → balance queryable) | MVP Doc | [ ] |
| AC-04 | Invalid transactions leave zero DB trace | MVP Doc | [ ] |
| AC-05 | Incentive applied to recipient only, never sender | MVP Doc | [ ] |
| AC-06 | Unknown user returns zero balance (not error) | MVP Doc | [ ] |
| AC-07 | Concurrent Kafka + REST operation verified | MVP Doc | [ ] |
| AC-08 | `waldorf` final balance correct (Task 3) | MVP Doc | [ ] |
| AC-09 | `wilbur` final balance correct (Task 4) | MVP Doc | [ ] |
| AC-10 | Scaffold contracts unbroken | MVP Doc | [ ] |
| AC-11 | No hard-coded config values | MVP Doc | [ ] |
| AC-12 | BEGIN/END markers captured (5 tasks) | MVP Doc | [ ] |
| **Post-MVP Upgrades (AC-13–AC-20)** | | | |
| AC-13 | All financial amounts use `BigDecimal` — no `double` | UP-1 | [ ] |
| AC-14 | Zero/negative amounts rejected (VR-04 active) | UP-1 | [ ] |
| AC-15 | Kafka redelivery does not cause double-processing (idempotency) | UP-1 | [ ] |
| AC-16 | Incentive API failure triggers graceful fallback — pipeline never halts | UP-2 | [ ] |
| AC-17 | `GET /balance` requires valid API key — unauthenticated calls return `401` | UP-3 | [ ] |
| AC-18 | Structured logs visible for all transaction lifecycle events | UP-4 | [ ] |
| AC-19 | `/actuator/health` returns `UP`; transaction counters visible at `/actuator/metrics` | UP-4 | [ ] |
| AC-20 | CI/CD pipeline runs and passes on GitHub; Docker Compose stack starts cleanly | UP-5 | [ ] |

---

## Full Fledged Working Prototype Gate

```
MIDAS CORE — FULL FLEDGED WORKING PROTOTYPE CERTIFICATION
══════════════════════════════════════════════════════════

  MVP Inherited Criteria    :  AC-01 → AC-12   [ ] All 12 VERIFIED
  Post-MVP Upgrade Criteria :  AC-13 → AC-20   [ ]  All 8 VERIFIED

  Upgrade Gates Cleared     :
    [ ] UP-1 Core Correctness      — 9  tests PASS
    [ ] UP-2 Resilience            — 10 tests PASS
    [ ] UP-3 Security              — 8  tests PASS
    [ ] UP-4 Observability         — 9  tests PASS
    [ ] UP-5 Infrastructure        — 8  tests PASS

  Full Regression Run       :  mvn clean install → BUILD SUCCESS
  Total Tests Passing       :  96 / 96

  FFWP STATUS:
  ⬜ Not Started
  🟡 Upgrade In Progress
  🟠 Partial (some AC failing)
  ✅ FULL FLEDGED WORKING PROTOTYPE CERTIFIED

══════════════════════════════════════════════════════════
```

---

## Complete Document Chain Summary

```
Phase 1 — Documents & Designing
│
├── 1. Project_Understanding_Draft.md
│      └── What is Midas Core? Architecture, tasks, stack.
│
├── 2. Midas_Core_Module_Decomposition.md
│      └── 5 modules (M1–M5) + prototype gates + module test suites
│           20 module-level tests total
│
├── 3. Midas_Core_MVP_Integration.md
│      └── How M1–M5 integrate into the MVP
│           4 integration points + 32 integration tests + 12 ACs
│           → Output: MVP CERTIFIED
│
└── 4. Midas_Core_Post_MVP_Upgrade.md     ← THIS DOCUMENT
       └── 5 upgrade phases (UP-1 through UP-5)
            44 upgrade tests total (9+10+8+9+8)
            8 additional ACs (AC-13 through AC-20)
            → Output: FULL FLEDGED WORKING PROTOTYPE CERTIFIED

TOTAL DESIGNED TEST COVERAGE:  96 tests across all phases
TOTAL ACCEPTANCE CRITERIA:     20 (12 MVP + 8 Post-MVP)
```

---

*Document created as part of Phase 1 — Documents & Designing*  
*Source: `Midas_Core_MVP_Integration.md` (Post-MVP gap items PH-01 through PH-10)*  
*This is the final planning document before Phase 2 — Implementation begins.*
