# 🚀 Midas Core — MVP Integration Document
### JP Morgan Chase — Software Engineering Job Simulation
> **Project**: Midas Core — Financial Transaction Processing Service  
> **Document Type**: MVP Integration Plan + Proprietary Integration Test Suite  
> **Integrates**: M1 (Foundation) + M2 (Kafka) + M3 (Validation & Persistence) + M4 (Incentive Client) + M5 (Balance REST API)  
> **Author Date**: September 2026  
> **Status**: Phase 1 — Design & Planning  
> **Preceding Document**: `Midas_Core_Module_Decomposition.md`

---

## What is the MVP?

The **Minimum Viable Product (MVP)** of Midas Core is the **first state of the system where all five module prototypes are integrated and operating together as a single, coherent end-to-end pipeline.**

> A module being a prototype means it works **in isolation**.  
> The MVP means all prototypes work **together, simultaneously, without breaking each other.**

The MVP is declared when:
- All 5 module prototypes have individually passed their prototype gates
- All cross-module integration points are verified to handshake correctly
- The full pipeline can ingest a Kafka transaction message and produce a correct, queryable balance — end to end — **in a single running process**
- The proprietary MVP Integration Test Suite (this document) passes 100%

---

## MVP Integration Architecture

### Full System Component Diagram

```
╔══════════════════════════════════════════════════════════════════════╗
║                    MIDAS CORE MVP — Single Spring Boot Process       ║
║                          (Port: 33400)                               ║
║                                                                      ║
║  ┌─────────────────────────────────────────────────────────────┐    ║
║  │                    SPRING APPLICATION CONTEXT                │    ║
║  │                                                             │    ║
║  │  [M2] KafkaListener ──────────────────────────────────►    │    ║
║  │         │ @KafkaListener                    TransactionService│   ║
║  │         │ topic: ${general.kafka-topic}            │   [M3] │    ║
║  │         │                              ┌───────────┘         │    ║
║  │         │                              │                     │    ║
║  │         │                    ┌─────────▼──────────┐         │    ║
║  │         │                    │  Validation Engine  │         │    ║
║  │         │                    │  VR-01: senderId    │         │    ║
║  │         │                    │  VR-02: recipientId │         │    ║
║  │         │                    │  VR-03: balance>=amt│         │    ║
║  │         │                    └────────┬────────────┘         │    ║
║  │         │                             │ valid only           │    ║
║  │         │                    ┌────────▼────────────┐         │    ║
║  │         │                    │  IncentiveClient    │ [M4]    │    ║
║  │         │                    │  POST /incentive    │         │    ║
║  │         │                    │  → Incentive.amount │         │    ║
║  │         │                    └────────┬────────────┘         │    ║
║  │         │                             │                      │    ║
║  │         │                    ┌────────▼────────────┐         │    ║
║  │         │                    │   H2 Database (JPA) │ [M3]    │    ║
║  │         │                    │   TransactionRecord │         │    ║
║  │         │                    │   User.balance      │         │    ║
║  │         │                    └────────┬────────────┘         │    ║
║  │         │                             │                      │    ║
║  │  [M5] BalanceController ◄─────────────┘                     │    ║
║  │         │ GET /balance?userId=...                            │    ║
║  │         │ → Balance JSON                                     │    ║
║  └─────────────────────────────────────────────────────────────┘    ║
║                                                                      ║
╚══════════════════════════════════════════════════════════════════════╝
         ▲                                         │
         │                                         │
  [Kafka Broker]                           [API Consumers]
  trader-updates topic                    GET /balance queries
         ▲
         │
  [Transaction Producers]
  (Trading systems / test harness)


EXTERNAL DEPENDENCY (must run separately):
  [Incentive API JAR] ──► http://localhost:8080/incentive
```

---

### MVP Bean Dependency Graph

```
application.yml (M1)
    ├──► KafkaListener (M2)
    │         └──► TransactionService (M3)
    │                   ├──► UserRepository (M3)
    │                   ├──► TransactionRecordRepository (M3)
    │                   └──► IncentiveClient (M4)
    │                               └──► RestTemplate → Incentive API (External)
    └──► BalanceController (M5)
              └──► UserRepository (M3) [shared]
```

> **Critical**: `UserRepository` is **shared** between `TransactionService` (M3) and `BalanceController` (M5).  
> This shared state is what makes concurrent read + write the central integration risk (see IT-5).

---

## Integration Points

There are **4 explicit cross-module handshakes** that must be verified during integration. These are the seams where individually-prototyped modules connect.

### Integration Point IP-1: M2 → M3 (Kafka → Service)

| Property | Value |
|---|---|
| **From** | `KafkaListener.listen(Transaction t)` |
| **To** | `TransactionService.process(Transaction t)` |
| **Data Contract** | `Transaction` object: `senderId` (String), `recipientId` (String), `amount` (double) |
| **Failure Mode** | `NullPointerException` if `TransactionService` not injected into `KafkaListener` |
| **Integration Risk** | LOW — simple method call between two Spring beans |

### Integration Point IP-2: M3 → M3 (Service → Database)

| Property | Value |
|---|---|
| **From** | `TransactionService.process()` after validation |
| **To** | `UserRepository` (read/write) + `TransactionRecordRepository` (write) |
| **Data Contract** | `User.balance` (double), `TransactionRecord` with `@ManyToOne` User refs |
| **Failure Mode** | Partial write if `@Transactional` is missing; stale balance on concurrent access |
| **Integration Risk** | HIGH — financial state mutation; must be atomic |

### Integration Point IP-3: M3 → M4 (Service → Incentive API)

| Property | Value |
|---|---|
| **From** | `TransactionService.process()` after validation passes |
| **To** | `IncentiveClient.getIncentive(Transaction t)` → `POST http://localhost:8080/incentive` |
| **Data Contract** | Request: JSON `Transaction`; Response: `{ "amount": double }` |
| **Failure Mode** | `ResourceAccessException` if Incentive API JAR is not running |
| **Integration Risk** | HIGH — only outbound network call; single point of external failure |

### Integration Point IP-4: M3 → M5 (Database → REST Controller)

| Property | Value |
|---|---|
| **From** | `UserRepository` (written by M3 service) |
| **To** | `BalanceController.getBalance()` (reads same `UserRepository`) |
| **Data Contract** | `User.balance` (double) → serialized as `Balance { userId, amount }` JSON |
| **Failure Mode** | Stale read if `@Transactional` is missing on service writes |
| **Integration Risk** | MEDIUM — concurrent read during active write window |

---

## MVP Integration Data Flow

### Happy Path (Valid Transaction — Full Pipeline)

```
Step 1: Transaction Producer publishes to Kafka topic "trader-updates"
        Message: { senderId: "waldorf", recipientId: "wilbur", amount: 100.00 }

Step 2: [M2] KafkaListener.listen() receives and deserializes Transaction
        → hands Transaction to TransactionService

Step 3: [M3] TransactionService.process() — Validation
        VR-01: waldorf exists in User table?             → PASS
        VR-02: wilbur exists in User table?              → PASS
        VR-03: waldorf.balance (500.00) >= 100.00?       → PASS
        All rules passed → proceed to enrichment

Step 4: [M4] IncentiveClient.getIncentive(transaction)
        POST http://localhost:8080/incentive
        → Response: { "amount": 15.00 }

Step 5: [M3] TransactionService — Balance Update
        waldorf.balance  = 500.00 - 100.00          = 400.00
        wilbur.balance   = 200.00 + 100.00 + 15.00  = 315.00

Step 6: [M3] Persistence
        TransactionRecord persisted: { sender: waldorf, recipient: wilbur,
                                       amount: 100.00, incentive: 15.00 }
        User rows updated in H2

Step 7: [M5] BalanceController serves GET /balance?userId=wilbur
        → { "userId": "wilbur", "amount": 315.00 }
```

### Rejection Path (Invalid Transaction)

```
Step 1–2: Same as above — Transaction received and deserialized by M2

Step 3: [M3] TransactionService.process() — Validation
        VR-03: waldorf.balance (50.00) >= 5000.00?   → FAIL
        → Transaction DISCARDED. Pipeline stops here.

Result: No IncentiveClient call (M4 not invoked)
        No TransactionRecord row created
        No balance change for either user
        GET /balance returns unchanged balances
```

---

## Proprietary MVP Integration Test Suite

> This is the **definitive test suite** for the MVP.  
> All 6 test groups must pass for MVP certification.  
> Individual module tests (T1-xx through T5-xx) must have ALREADY passed before running these.

---

### Test Suite Structure

| Suite ID | Suite Name | # Tests | Focus Area |
|----------|-----------|---------|------------|
| IT-1 | Environment & Context Integrity | 5 | All beans wire correctly in one Spring context |
| IT-2 | Pipeline Smoke Test | 4 | A single valid transaction flows end-to-end |
| IT-3 | Transaction Lifecycle Scenarios | 8 | Valid, invalid, boundary — all paths |
| IT-4 | Error & Boundary Conditions | 6 | Edge cases, unknown users, zero amounts |
| IT-5 | Concurrent Operation Test | 4 | Kafka + REST running simultaneously |
| IT-6 | System Correctness & State Audit | 5 | Final balances match exact expected values |
| **TOTAL** | | **32 tests** | Full end-to-end MVP coverage |

---

### IT-1 — Environment & Context Integrity
> **Goal**: Confirm all 5 module prototypes coexist in a single Spring Boot application context without conflict.  
> **Pre-condition**: Incentive API JAR running at `localhost:8080`  
> **Run**: `mvn clean install`

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| IT-1-01 | Full Context Load | All module beans initialize without error | No `BeanCreationException`, no missing dependencies |
| IT-1-02 | KafkaListener Bean Registration | M2 listener is present and wired | `@Autowired KafkaListener` resolves; no `NoSuchBeanDefinitionException` |
| IT-1-03 | TransactionService Bean Registration | M3 service present with all repos injected | `UserRepository` + `TransactionRecordRepository` both autowired inside service |
| IT-1-04 | IncentiveClient Bean Registration | M4 client present in context | `@Autowired IncentiveClient` resolves without error |
| IT-1-05 | BalanceController Bean Registration | M5 controller present on correct port | `@RestController` bean registered; server binds to port `33400` |

**Expected Result**: Spring Boot startup log shows no errors. All beans listed in context refresh output.

---

### IT-2 — Pipeline Smoke Test
> **Goal**: Verify that a single, hand-crafted valid transaction flows through the entire pipeline — from Kafka ingestion to REST-queryable balance.  
> **Pre-condition**: Incentive API JAR running. Embedded Kafka active via test harness.

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| IT-2-01 | Kafka Message Received | M2 listener picks up published transaction | `listen()` invoked; `Transaction` object populated with correct fields |
| IT-2-02 | Service Processes Transaction | M3 validates and routes the transaction | No exception thrown; validation rules evaluated against H2 user data |
| IT-2-03 | Incentive API Called | M4 client issues POST to Incentive API | `IncentiveClient.getIncentive()` returns non-null `Incentive` with `amount >= 0.0` |
| IT-2-04 | Balance Updated and Queryable | M3 persists; M5 exposes result | `GET /balance?userId={recipient}` returns balance that includes `amount + incentive` |

**Expected Result**: A transaction published to Kafka is fully processed and the resulting balance is visible via HTTP within the same test run.

---

### IT-3 — Transaction Lifecycle Scenarios
> **Goal**: Verify the system correctly handles all transaction lifecycle paths — valid, each type of invalid, and boundary conditions.

| Test ID | Test Name | Scenario | Pass Condition |
|---------|-----------|----------|----------------|
| IT-3-01 | Valid Transaction Persisted | All 3 validation rules pass | `TransactionRecord` row exists in H2; both user balances updated |
| IT-3-02 | Invalid — Unknown Sender | `senderId` does not exist (VR-01 fails) | No `TransactionRecord` row; sender/recipient balances unchanged |
| IT-3-03 | Invalid — Unknown Recipient | `recipientId` does not exist (VR-02 fails) | No `TransactionRecord` row; sender/recipient balances unchanged |
| IT-3-04 | Invalid — Insufficient Funds | `sender.balance < transaction.amount` (VR-03 fails) | No `TransactionRecord` row; sender balance unchanged |
| IT-3-05 | Incentive Applied to Recipient Only | Valid transaction, non-zero incentive | `recipient.balance += amount + incentive.amount`; `sender.balance -= amount only` |
| IT-3-06 | Incentive NOT Applied to Sender | Valid transaction with incentive | `sender.balance` reduced by `transaction.amount` exactly — not by `amount + incentive` |
| IT-3-07 | Multiple Valid Transactions Sequential | 5 valid transactions in order | All 5 persisted; balances accumulate correctly across all 5 |
| IT-3-08 | Mix of Valid + Invalid Transactions | 3 valid, 2 invalid interleaved | Only 3 `TransactionRecord` rows; balances reflect only 3 valid mutations |

---

### IT-4 — Error & Boundary Conditions
> **Goal**: Verify the system handles edge cases gracefully — no crashes, no data corruption.

| Test ID | Test Name | Scenario | Pass Condition |
|---------|-----------|----------|----------------|
| IT-4-01 | Balance Query for Unknown User | `GET /balance?userId=nonexistent` | Returns `200 OK` with `{ "amount": 0.0 }` — NOT a 404 or exception |
| IT-4-02 | Balance Query with No userId Param | `GET /balance` (missing param) | Returns `400 Bad Request` — Spring handles missing `@RequestParam` |
| IT-4-03 | Transaction Amount Equals Exact Balance | `sender.balance == transaction.amount` (boundary VR-03) | Transaction IS valid (balance >= amount is satisfied); transaction persisted |
| IT-4-04 | Incentive API Returns Zero Amount | `incentive.amount = 0.0` | `recipient.balance += amount + 0.0`; `TransactionRecord.incentive = 0.0`; no crash |
| IT-4-05 | Multiple Balance Queries Same User | 3 `GET /balance` calls for same user | All 3 return identical, consistent balance — no phantom reads |
| IT-4-06 | Same User as Sender and Recipient | `senderId == recipientId` | System processes without crash; net balance change depends on incentive only |

---

### IT-5 — Concurrent Operation Test
> **Goal**: Verify the `BalanceController` (M5) and `KafkaListener` (M2) can operate simultaneously within the same Spring Boot process — the defining integration risk of the MVP architecture.

| Test ID | Test Name | Scenario | Pass Condition |
|---------|-----------|----------|----------------|
| IT-5-01 | REST Query During Kafka Processing | `GET /balance` called while listener is actively consuming | HTTP response returned correctly; no deadlock; no `DataAccessException` |
| IT-5-02 | Balance Consistency Under Write | Query balance of a user whose balance is being actively mutated | Response reflects either pre-write or post-write state — never a partial/corrupted value |
| IT-5-03 | No Connection Pool Exhaustion | 10 sequential Kafka + 5 REST operations interleaved | No `HikariPool` timeout or `DataSource` connection error |
| IT-5-04 | Spring Context Shared Correctly | `UserRepository` shared between `TransactionService` and `BalanceController` | Both beans read/write same data; no `EntityNotFoundException` or stale entity |

**Key principle**: This suite validates FR-14 (Balance query must work while Kafka listener is active). This is the concurrent operation requirement that makes M5 non-trivial despite its apparent simplicity.

---

### IT-6 — System Correctness & State Audit
> **Goal**: Run the full program-provided test suite against the integrated MVP and verify exact numeric balance values against the specification.  
> **Pre-condition**: Incentive API JAR running. Fresh H2 database (ddl-auto: create-drop).

| Test ID | Test Name | What Is Verified | Pass Condition |
|---------|-----------|-----------------|----------------|
| IT-6-01 | All Task Tests Pass | `mvn clean install` — all 5 task test suites | BUILD SUCCESS; zero test failures; zero test errors |
| IT-6-02 | waldorf Final Balance Correct | Post-Task-3 balance inspection | `waldorf.balance` (rounded-down integer) matches Task 3 expected submission value |
| IT-6-03 | wilbur Final Balance Correct | Post-Task-4 balance inspection (includes incentives) | `wilbur.balance` (rounded-down integer) matches Task 4 expected submission value |
| IT-6-04 | TransactionRecord Count Correct | Total rows in `TransactionRecord` table | Equals number of valid transactions processed — zero extra rows from invalid ones |
| IT-6-05 | BEGIN/END Markers Present All Tasks | Console output scan | BEGIN marker + END marker visible for each of the 5 task test runs |

**Run Command for IT-6**:
```bash
# Pre-condition: Incentive API must be running
java -jar services/incentive-api.jar

# Full regression — must show BUILD SUCCESS
mvn clean install

# Individual verification
mvn test -Dtest=TaskOneTests
mvn test -Dtest=TaskTwoTests
mvn test -Dtest=TaskThreeTests
mvn test -Dtest=TaskFourTests
mvn test -Dtest=TaskFiveTests
```

---

## Integration Test Execution Order

> Run in this exact order. Do not skip steps. Failure at any step blocks subsequent steps.

```
Phase 0 — Prerequisites Check
    [ ] Java 17 installed and active
    [ ] Maven installed (mvn --version)
    [ ] Scaffold cloned from: https://github.com/vagabond-systems/forage-midas
    [ ] Incentive API JAR present at: services/incentive-api.jar

Phase 1 — Pre-Integration: Module Prototype Verification
    [ ] M1 Prototype Gate: TaskOneTests PASS
    [ ] M2 Prototype Gate: TaskTwoTests PASS
    [ ] M3 Prototype Gate: TaskThreeTests PASS
    [ ] M4 Prototype Gate: TaskFourTests PASS
    [ ] M5 Prototype Gate: TaskFiveTests PASS
    → If ANY module prototype fails, STOP. Fix that module before integration.

Phase 2 — Launch External Dependency
    [ ] Start Incentive API: java -jar services/incentive-api.jar
    [ ] Verify it responds: curl -X POST http://localhost:8080/incentive
                           (expect 200 OK with { "amount": <double> })

Phase 3 — Integration Test Suites
    [ ] IT-1: Environment & Context Integrity  → mvn clean install (check startup logs)
    [ ] IT-2: Pipeline Smoke Test              → mvn test -Dtest=TaskFiveTests (end-to-end)
    [ ] IT-3: Transaction Lifecycle Scenarios  → mvn test -Dtest=TaskThreeTests,TaskFourTests
    [ ] IT-4: Error & Boundary Conditions      → mvn test -Dtest=TaskFiveTests (unknown user cases)
    [ ] IT-5: Concurrent Operation Test        → mvn test -Dtest=TaskFiveTests (concurrent flag)
    [ ] IT-6: System Correctness & State Audit → mvn clean install (full regression)

Phase 4 — MVP Certification
    [ ] All IT-1 through IT-6 tests: 100% PASS
    [ ] Debugger verification: waldorf balance (Task 3) + wilbur balance (Task 4)
    [ ] BEGIN/END output captured for all 5 tasks
    → MVP DECLARED ✅
```

---

## MVP Acceptance Criteria

> The MVP is **officially accepted** when ALL of the following are satisfied:

| ID | Criterion | Verification Method |
|----|-----------|-------------------|
| AC-01 | All 5 module prototypes independently verified | Module prototype gates M1–M5 each checked individually |
| AC-02 | Full `mvn clean install` returns BUILD SUCCESS | Command output — zero failures, zero errors |
| AC-03 | Pipeline processes valid transaction end-to-end | IT-2 smoke test passes; balance queryable via HTTP after Kafka ingestion |
| AC-04 | Invalid transactions leave zero DB trace | IT-3-02, IT-3-03, IT-3-04 pass — no phantom records |
| AC-05 | Incentive applied to recipient only, never sender | IT-3-05, IT-3-06 pass — sender deducted by `amount` only |
| AC-06 | Balance endpoint handles unknown user gracefully | IT-4-01 passes — returns `{ "amount": 0.0 }` not an error |
| AC-07 | Concurrent Kafka + REST operation verified | IT-5-01 passes — no deadlock, no corrupted read |
| AC-08 | `waldorf` final balance verified (Task 3) | Debugger inspection — rounded-down integer matches expected |
| AC-09 | `wilbur` final balance verified (Task 4) | Debugger inspection — rounded-down integer matches expected |
| AC-10 | No scaffold contracts broken | `Transaction.java` unmodified; `Balance.toString()` unmodified; port stays 33400 |
| AC-11 | All config externalized — nothing hard-coded | Kafka topic, server port, DB URL all in `application.yml` |
| AC-12 | BEGIN/END markers captured for all 5 tasks | Console screenshots saved for program submission |

---

## MVP Prototype Gate — Final Certification

> **The Midas Core MVP is certified when ALL 12 Acceptance Criteria are met.**

```
AC-01  Module Prototypes Verified          [ ] Not Started  [ ] Verified
AC-02  Full Build Passes                   [ ] Not Started  [ ] Verified
AC-03  End-to-End Pipeline Works           [ ] Not Started  [ ] Verified
AC-04  Invalid Tx Leaves No DB Trace       [ ] Not Started  [ ] Verified
AC-05  Incentive Applied Correctly         [ ] Not Started  [ ] Verified
AC-06  Unknown User Returns Zero Balance   [ ] Not Started  [ ] Verified
AC-07  Concurrent Operation Verified       [ ] Not Started  [ ] Verified
AC-08  waldorf Balance Correct (Task 3)    [ ] Not Started  [ ] Verified
AC-09  wilbur Balance Correct (Task 4)     [ ] Not Started  [ ] Verified
AC-10  Scaffold Contracts Unbroken         [ ] Not Started  [ ] Verified
AC-11  No Hard-Coded Config Values         [ ] Not Started  [ ] Verified
AC-12  BEGIN/END Markers Captured (5 tasks)[ ] Not Started  [ ] Verified
```

**MVP Status**:  
`⬜ Not Started` → `🟡 Integration In Progress` → `🟠 Partial (some ACs failing)` → `✅ MVP CERTIFIED`

---

## Integration Risk Register

These are the risks that are specifically elevated at integration time (vs. per-module):

| Risk ID | Risk | Probability | Impact | Mitigation |
|---------|------|-------------|--------|------------|
| IR-01 | Incentive API JAR not running when TaskFour/FiveTests execute | HIGH | HIGH | Add pre-condition check; document startup order clearly |
| IR-02 | `UserRepository` shared between `TransactionService` and `BalanceController` causes stale entity reads | MEDIUM | HIGH | Ensure `@Transactional` on service writes; Spring Data handles session scope |
| IR-03 | `TransactionService` not injected into `KafkaListener` (NullPointerException at runtime) | LOW | HIGH | Verify `@Autowired` or constructor injection; check component scan coverage |
| IR-04 | H2 in-memory database is wiped between test runs (ddl-auto: create-drop) | HIGH | MEDIUM | Expected behavior — seed data must reload for each test class; note in test setup |
| IR-05 | Port 33400 already bound (another process) | LOW | HIGH | `server.port: 33400` must be free; kill conflicting process before test run |
| IR-06 | `Balance.toString()` modified inadvertently | LOW | HIGH | Code review gate — mark file as DO NOT MODIFY in comments |
| IR-07 | Incentive applied to BOTH sender and recipient (logic error) | MEDIUM | HIGH | IT-3-06 specifically catches this; trace `TransactionService` balance update lines |

---

## Known Gaps — Post-MVP Hardening (Not in MVP Scope)

The following items are **out of MVP scope** but must be addressed before any production deployment:

| Gap ID | Gap | Risk (from Spec) | Post-MVP Action |
|--------|-----|-----------------|-----------------|
| PH-01 | No authentication on `GET /balance` | R-04 | Add Spring Security + JWT or API key |
| PH-02 | No idempotency — Kafka redelivery causes double-processing | R-02 | Add unique transaction ID + deduplication check |
| PH-03 | `double` used for financial amounts — precision risk | R-03 | Migrate all amount fields to `BigDecimal` |
| PH-04 | No circuit breaker around Incentive API call | R-01 | Add Resilience4j `@CircuitBreaker` + timeout |
| PH-05 | No structured logging or metrics | R-08 | Add SLF4J logging + Micrometer metrics |
| PH-06 | No dead-letter handling for invalid Kafka messages | R-06 | Configure dead-letter topic in Kafka consumer |
| PH-07 | Zero-amount transaction not explicitly rejected | R-09 | Add VR-04: `transaction.amount > 0` validation rule |
| PH-08 | Incentive API response not bounds-checked | R-10 | Validate `incentive.amount` against defined upper bound |
| PH-09 | H2 is not a production database | Architectural | Replace with PostgreSQL/MySQL via JPA abstraction |
| PH-10 | No CI/CD pipeline | Operational | Add GitHub Actions for automated test-on-push |

---

## Integration Document Summary

```
 MIDAS CORE — MVP INTEGRATION SUMMARY
 ═══════════════════════════════════════════════════════

  Modules Integrated  :  M1 + M2 + M3 + M4 + M5
  Integration Points  :  4 (IP-1 through IP-4)
  Total Test Cases    :  32 (IT-1-01 through IT-6-05)
  Acceptance Criteria :  12 (AC-01 through AC-12)
  External Services   :  1 (Incentive API JAR)
  Technology Stack    :  Java 17 / Spring Boot 3.2.5 / Kafka / H2 / JPA
  Prototype → MVP     :  All 5 module gates + all 6 IT suites pass

 THE MVP IS:
  A single Spring Boot process that:
    1. Listens on a Kafka topic for transaction messages
    2. Validates each transaction against user accounts in H2
    3. Enriches valid transactions with Incentive API rewards
    4. Persists valid transactions and updates user balances atomically
    5. Exposes a queryable HTTP endpoint for current account balances
    6. Does all of the above concurrently, reliably, and correctly.

 ═══════════════════════════════════════════════════════
```

---

*Document created as part of Phase 1 — Documents & Designing*  
*Source Documents: `Midas_Core_Specification.docx`, `Project_Understanding_Draft.md`, `Midas_Core_Module_Decomposition.md`*  
*Next Phase: Phase 2 — Implementation (M1 → M2 → M3 → M4 → M5 → MVP Integration)*
