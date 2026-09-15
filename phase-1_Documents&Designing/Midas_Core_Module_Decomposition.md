# 🧩 Midas Core — Module Decomposition & Prototype Roadmap
### JP Morgan Chase — Software Engineering Job Simulation
> **Project**: Midas Core — Financial Transaction Processing Service  
> **Document Type**: Module Decomposition + Test-Gated Prototype Plan  
> **Source Documents**: `Midas_Core_Specification.docx`, `Project_Understanding_Draft.md`  
> **Author Date**: September 2026  
> **Status**: Phase 1 — Design & Planning

---

## How to Read This Document

Each section below represents **one self-contained module** of Midas Core.  
Every module follows this structure:

| Section | Purpose |
|---|---|
| **Overview** | What the module is and why it exists |
| **Responsibilities** | Exact duties this module owns |
| **Classes & Files** | Java files to create or configure |
| **Dependencies** | What this module needs from other modules |
| **Proprietary Test Suite** | The exact test(s) that prove this module works |
| **Prototype Gate** | Criteria that must all pass before this module is considered a prototype |

> **Rule**: A module becomes a **Prototype** only when **100% of its test suite passes**. No partial credit.

---

## Module Map Overview

```
[M1] Project Foundation
      └──► [M2] Kafka Ingestion Layer
                 └──► [M3] Validation & Persistence Layer
                               └──► [M4] Incentive Client
                                         └──► [M5] Balance REST API
```

> Each module is a **strict dependency** of the next.  
> M1 must prototype before M2 begins. M2 before M3. And so on.

---

## MODULE 1 — Project Foundation & Environment
> **Task Reference**: Task 1 | **Prototype Target**: Day 1

### 1.1 Overview

This is the **bedrock module**. It does not implement any business logic — it establishes the environment, configures the build system, wires Spring Boot's auto-configuration, and validates that the entire scaffold compiles and starts correctly. Every subsequent module inherits from this foundation.

Without M1 passing, nothing else can be built.

---

### 1.2 Responsibilities

- Fork and configure the scaffold repository
- Manage all Maven dependencies in `pom.xml`
- Configure `application.yml` with environment values (Kafka topic, server port, datasource)
- Ensure the Spring Boot application context starts without errors
- Validate the baseline build compiles and the test harness is reachable

---

### 1.3 Files & Configuration

| File | Action | Purpose |
|------|--------|---------|
| `pom.xml` | MODIFY | Add all required Maven dependencies |
| `src/main/resources/application.yml` | MODIFY | Add kafka-topic, server.port, H2 datasource config |
| `MidasCoreApplication.java` | GIVEN | Spring Boot entry point — do not modify |

#### Required `pom.xml` Dependencies

| Dependency | GroupId | Version |
|------------|---------|---------|
| `spring-boot-starter-data-jpa` | `org.springframework.boot` | 3.2.5 |
| `spring-boot-starter-web` | `org.springframework.boot` | 3.2.5 |
| `spring-kafka` | `org.springframework.kafka` | 3.1.4 |
| `h2` | `com.h2database` | 2.2.224 |
| `spring-boot-starter-test` | `org.springframework.boot` | 3.2.5 |
| `spring-kafka-test` | `org.springframework.kafka` | 3.1.4 |
| `kafka` (Testcontainers) | `org.testcontainers` | 1.19.1 |

#### Required `application.yml` Configuration

```yaml
general:
  kafka-topic: trader-updates       # M2 - Kafka listener topic

server:
  port: 33400                       # M5 - REST API port

spring:
  datasource:
    url: jdbc:h2:mem:testdb         # M3 - H2 in-memory database
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

---

### 1.4 Dependencies

| Dependency | Type |
|---|---|
| Java 17 | Runtime |
| Maven | Build tool |
| IntelliJ IDEA (recommended) | IDE |
| Git + GitHub | Source control |

> No other Midas Core modules are required. This is the root.

---

### 1.5 Proprietary Test Suite

**Test Class**: `TaskOneTests.java` (provided in scaffold)

| Test ID | What It Verifies | Pass Condition |
|---------|-----------------|----------------|
| T1-01 | Application context loads without error | Spring Boot starts with no BeanCreationException |
| T1-02 | All Maven dependencies resolve correctly | `mvn clean install` completes with BUILD SUCCESS |
| T1-03 | `application.yml` is valid and loaded | No BindException or missing property errors at startup |
| T1-04 | BEGIN/END console markers appear | TaskOneTests outputs a START and END marker in console |

**Run Command**:
```bash
mvn clean install
mvn test -Dtest=TaskOneTests
```

**Expected Output**: A BEGIN and END marker visible in test output — capture screenshot for submission.

---

### 1.6 Prototype Gate — M1

> **M1 is a Prototype when ALL of the following are true:**

- [ ] `mvn clean install` exits with code 0 (BUILD SUCCESS)
- [ ] `mvn spring-boot:run` starts without any exceptions
- [ ] `TaskOneTests` passes 100% — BEGIN/END markers visible in console
- [ ] `application.yml` contains all three configuration blocks (kafka-topic, server.port, datasource)
- [ ] No hard-coded values exist where externalized config is required

**Status**: Not Started → In Progress → PROTOTYPE

---

## MODULE 2 — Kafka Ingestion Layer
> **Task Reference**: Task 2 | **Prototype Target**: Day 2 | **Depends on**: M1

### 2.1 Overview

This module wires Midas Core into the **Apache Kafka message ecosystem**. It creates a listener that subscribes to the `trader-updates` topic, deserializes incoming JSON messages into `Transaction` domain objects, and hands them downstream for processing.

This is the **entry point** of the entire transaction pipeline.

**Why Kafka?**
- **Decoupling**: Producers publish without knowing Midas Core exists
- **Burst tolerance**: Kafka absorbs traffic spikes — Midas Core processes at its own pace
- **Scalability**: Multiple consumer groups can be added independently

---

### 2.2 Responsibilities

- Subscribe to the Kafka topic defined in `application.yml` via `${general.kafka-topic}`
- Deserialize each Kafka message payload into a `Transaction` object
- Pass deserialized `Transaction` to the Transaction Service (M3)
- Use the embedded Kafka broker in tests (no external broker required)

---

### 2.3 Files & Configuration

| File | Action | Purpose |
|------|--------|---------|
| `KafkaListener.java` | CREATE | Kafka consumer class with `@KafkaListener` annotation |
| `domain/Transaction.java` | GIVEN | Domain class — do not modify |
| `application.yml` | ALREADY SET (M1) | `general.kafka-topic` injects the topic name |

#### Key Design Pattern

```java
@Component
public class KafkaListener {

    @org.springframework.kafka.annotation.KafkaListener(
        topics = "${general.kafka-topic}"
    )
    public void listen(Transaction transaction) {
        // Hand off to TransactionService (M3)
    }
}
```

> **Rule**: Do NOT hard-code `"trader-updates"` — it MUST be injected from config.  
> **Rule**: Do NOT configure host/port manually — tests use embedded Kafka (autowired).

---

### 2.4 Dependencies

| Dependency | Module |
|---|---|
| Maven dependencies (spring-kafka, spring-kafka-test, Testcontainers) | M1 |
| `application.yml` with `general.kafka-topic` | M1 |
| `domain/Transaction.java` (scaffold-provided) | Given |

---

### 2.5 Proprietary Test Suite

**Test Class**: `TaskTwoTests.java` (provided in scaffold)

| Test ID | What It Verifies | Pass Condition |
|---------|-----------------|----------------|
| T2-01 | `KafkaListener` bean is registered in Spring context | No NoSuchBeanDefinitionException |
| T2-02 | Topic is read from config, not hard-coded | Topic resolves to `"trader-updates"` via config |
| T2-03 | Messages are correctly deserialized | First 4 amounts match: 122.86, 42.87, 161.79, 22.22 |
| T2-04 | Embedded Kafka works without external broker | Test passes without any external Kafka process running |

**Run Command**:
```bash
mvn test -Dtest=TaskTwoTests
```

**Verification Step**: Set breakpoint inside `listen()`, inspect first 4 Transaction objects:
```
Transaction[0].amount = 122.86
Transaction[1].amount = 42.87
Transaction[2].amount = 161.79
Transaction[3].amount = 22.22
```

---

### 2.6 Prototype Gate — M2

> **M2 is a Prototype when ALL of the following are true:**

- [ ] `TaskTwoTests` passes 100%
- [ ] First 4 transaction amounts verified via debugger: 122.86, 42.87, 161.79, 22.22
- [ ] Topic name is injected from config, NOT hard-coded in `@KafkaListener`
- [ ] No external Kafka broker process required to run tests
- [ ] `KafkaListener` correctly passes `Transaction` objects downstream

**Status**: Not Started → In Progress → PROTOTYPE

---

## MODULE 3 — Validation & Persistence Layer
> **Task Reference**: Task 3 | **Prototype Target**: Day 3–4 | **Depends on**: M1, M2

### 3.1 Overview

This is the **core business logic module** — the most complex and highest-risk component of Midas Core. It receives `Transaction` objects from M2, applies three business validation rules, and either:

- **Persists** the transaction + updates both account balances (if valid), OR  
- **Discards** the transaction silently (if invalid — no DB writes, no balance changes)

**Why SQL over NoSQL?**  
Financial state requires **atomic, consistent writes**. SQL + JPA transactions guarantee all-or-nothing semantics.

---

### 3.2 Responsibilities

- Define and persist the `TransactionRecord` JPA entity
- Define the `User` JPA entity (if not provided in scaffold)
- Implement Spring Data JPA repositories for `User` and `TransactionRecord`
- Enforce the 3 business validation rules (VR-01, VR-02, VR-03)
- On valid transaction: persist record, debit sender balance, credit recipient balance
- On invalid transaction: discard — zero database mutations

---

### 3.3 Business Validation Rules

| Rule ID | Rule | Failure Action |
|---------|------|----------------|
| VR-01 | `senderId` must match an existing `User` in the database | Discard transaction |
| VR-02 | `recipientId` must match an existing `User` in the database | Discard transaction |
| VR-03 | Sender's current `balance >= transaction.amount` | Discard transaction |

> All three rules must pass. If any one fails, the entire transaction is rejected.

---

### 3.4 Balance Update Logic (Valid Transactions Only)

```
sender.balance    = sender.balance    - transaction.amount
recipient.balance = recipient.balance + transaction.amount
```

---

### 3.5 Entity Relationship Design

```
User (1) ─── (Many) TransactionRecord [as sender]
User (1) ─── (Many) TransactionRecord [as recipient]

TransactionRecord:
  - id          : Long (auto-generated PK)
  - sender      : @ManyToOne → User
  - recipient   : @ManyToOne → User
  - amount      : double
  - incentive   : double  ← populated by M4 (leave 0.0 for now)
```

> **Critical Rule**: Do NOT add `@Entity` to the provided `Transaction` class. Create a separate `TransactionRecord` entity.

---

### 3.6 Files & Configuration

| File | Action | Purpose |
|------|--------|---------|
| `TransactionRecord.java` | CREATE | JPA entity for persisted transactions |
| `TransactionService.java` | CREATE | Orchestrator: validation → incentive → persist |
| `UserRepository.java` | CREATE | Spring Data JPA repository for User |
| `TransactionRecordRepository.java` | CREATE | Spring Data JPA repository for TransactionRecord |
| `domain/User.java` | GIVEN or CREATE | JPA entity with id (String PK) and balance (double) |
| `domain/Transaction.java` | GIVEN | Incoming DTO — do not modify |

---

### 3.7 Proprietary Test Suite

**Test Class**: `TaskThreeTests.java` (provided in scaffold)

| Test ID | What It Verifies | Pass Condition |
|---------|-----------------|----------------|
| T3-01 | Valid transactions are persisted to H2 | `TransactionRecord` rows appear in H2 after processing |
| T3-02 | Invalid transactions (bad senderId) are discarded | No DB row created; no balance change |
| T3-03 | Invalid transactions (bad recipientId) are discarded | Same as T3-02 |
| T3-04 | Invalid transactions (insufficient balance) are discarded | Same as T3-02 |
| T3-05 | Sender balance is correctly debited | `sender.balance` = original - amount |
| T3-06 | Recipient balance is correctly credited | `recipient.balance` = original + amount |
| T3-07 | `waldorf` user final balance is correct | Inspect integer part of waldorf's balance in debugger |

**Run Command**:
```bash
mvn test -Dtest=TaskThreeTests
```

**Verification Step**: Inspect `waldorf` user's final `balance` in the debugger — note the rounded-down integer value (Task 3 submission answer).

---

### 3.8 Prototype Gate — M3

> **M3 is a Prototype when ALL of the following are true:**

- [ ] `TaskThreeTests` passes 100%
- [ ] `waldorf` user's final balance verified in debugger
- [ ] Invalid transactions leave **zero trace** in the database
- [ ] `Transaction.java` scaffold class remains **unmodified**
- [ ] All balance mutations happen within a single atomic operation
- [ ] `TransactionRecord` has proper `@ManyToOne` to `User` for both sender and recipient

**Status**: Not Started → In Progress → PROTOTYPE

---

## MODULE 4 — Incentive API Client
> **Task Reference**: Task 4 | **Prototype Target**: Day 5 | **Depends on**: M1, M2, M3

### 4.1 Overview

This module connects Midas Core to an **external, independently owned Incentive API** — a black-box REST microservice that computes a reward amount per transaction. Midas Core does not own or understand its internal logic — only its **API contract** matters.

After a transaction passes validation (M3), Midas Core now **enriches** it by:
1. POSTing the `Transaction` object to the Incentive API
2. Receiving an `{ "amount": <double> }` response
3. Applying the incentive amount **only to the recipient's balance**

Key principle: **"API as a Contract"** — two teams work in parallel as long as neither breaks the shared interface.

---

### 4.2 Responsibilities

- Run the Incentive API JAR locally (`services/incentive-api.jar`) before tests
- Create `IncentiveClient.java` to POST transactions and parse responses
- Create `Incentive.java` DTO to deserialize the API response
- Update `TransactionService` to call the client after validation
- Store the incentive amount in `TransactionRecord.incentive`
- Update balance logic: recipient gets `amount + incentive`, sender loses only `amount`

---

### 4.3 Updated Balance Logic

```
sender.balance    = sender.balance    - transaction.amount          (unchanged)
recipient.balance = recipient.balance + transaction.amount + incentive.amount
```

> The incentive is a **reward to the recipient** — it is never deducted from anyone.

---

### 4.4 Incentive API Contract

| Property | Value |
|---|---|
| Base URL | `http://localhost:8080` |
| Endpoint | `POST /incentive` |
| Request Body | JSON-serialized `Transaction` object |
| Response Body | `{ "amount": <double> }` where amount >= 0 |

---

### 4.5 Files & Configuration

| File | Action | Purpose |
|------|--------|---------|
| `IncentiveClient.java` | CREATE | Calls POST /incentive using `RestTemplate` |
| `domain/Incentive.java` | CREATE | DTO for Incentive API response: single field `amount` |
| `TransactionService.java` | MODIFY | Add Incentive API call; apply incentive to recipient |
| `TransactionRecord.java` | MODIFY | Add `incentive` field to persist incentive amount |

#### Key Design Pattern

```java
@Component
public class IncentiveClient {
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String INCENTIVE_URL = "http://localhost:8080/incentive";

    public Incentive getIncentive(Transaction transaction) {
        return restTemplate.postForObject(INCENTIVE_URL, transaction, Incentive.class);
    }
}
```

> **Rule**: Let Spring's RestTemplate handle JSON serialization automatically.

---

### 4.6 Dependencies

| Dependency | Module |
|---|---|
| `TransactionService` with validated transactions | M3 |
| `TransactionRecord` entity (add `incentive` field) | M3 |
| Maven `spring-boot-starter-web` for RestTemplate | M1 |
| Incentive API JAR running at `localhost:8080` | External |

---

### 4.7 Proprietary Test Suite

**Test Class**: `TaskFourTests.java` (provided in scaffold)

> **Pre-condition**: Incentive API JAR MUST be running before executing this test.
> ```bash
> java -jar services/incentive-api.jar
> ```

| Test ID | What It Verifies | Pass Condition |
|---------|-----------------|----------------|
| T4-01 | `IncentiveClient` bean exists in Spring context | No NoSuchBeanDefinitionException |
| T4-02 | POST to `/incentive` succeeds and returns non-null | `Incentive.amount >= 0.0` received |
| T4-03 | Incentive amount stored in `TransactionRecord.incentive` | Field is non-zero for eligible transactions |
| T4-04 | Recipient balance = `amount + incentive` | Recipient's final balance matches expected value |
| T4-05 | Sender balance = original - `amount` only | Sender is NOT charged the incentive |
| T4-06 | `wilbur` user final balance is correct | Inspect `wilbur.balance` in debugger (rounded-down integer) |

**Run Command**:
```bash
# Step 1: Start Incentive API
java -jar services/incentive-api.jar

# Step 2: Run tests
mvn test -Dtest=TaskFourTests
```

---

### 4.8 Prototype Gate — M4

> **M4 is a Prototype when ALL of the following are true:**

- [ ] Incentive API JAR starts and responds on `http://localhost:8080/incentive`
- [ ] `TaskFourTests` passes 100%
- [ ] `wilbur` user's final balance verified in debugger
- [ ] `TransactionRecord.incentive` field stores the received incentive amount
- [ ] Recipient balance correctly reflects `amount + incentive`
- [ ] Sender balance reflects **only** `amount` deduction — incentive does NOT affect sender

**Status**: Not Started → In Progress → PROTOTYPE

---

## MODULE 5 — Balance REST API
> **Task Reference**: Task 5 | **Prototype Target**: Day 6 | **Depends on**: M1, M2, M3, M4

### 5.1 Overview

This is the **output layer** of Midas Core — it makes the system's state queryable by external consumers. It exposes a simple HTTP GET endpoint for querying a user's current account balance.

This completes the **full end-to-end pipeline**:
```
Kafka → Validation → Incentive Enrichment → Persistence → REST Query
```

**Architectural Decision**: Balance endpoint lives inside Midas Core (not a separate microservice) — chosen for practicality at current scale. Architecture should serve the team, not aesthetics.

---

### 5.2 Responsibilities

- Create a `@RestController` that exposes `GET /balance?userId=...`
- Accept `userId` as a `@RequestParam`
- Look up the corresponding `User` in the database via `UserRepository`
- Return a `Balance` DTO as JSON
- If `userId` not found: return `Balance` with `amount = 0.0` (NOT a 404 error)
- Ensure REST controller runs **alongside** the Kafka listener (same Spring Boot process)
- Confirm port is `33400` as configured in `application.yml`

---

### 5.3 API Contract

```
GET http://localhost:33400/balance?userId={userId}

Success Response (200 OK):
{
  "userId": "waldorf",
  "amount": 128.40
}

Unknown User Response (200 OK — by design):
{
  "userId": "unknown",
  "amount": 0.0
}
```

---

### 5.4 Files & Configuration

| File | Action | Purpose |
|------|--------|---------|
| `BalanceController.java` | CREATE | Spring RestController with GET /balance endpoint |
| `domain/Balance.java` | CREATE | DTO for JSON response (userId + amount) |
| `application.yml` | ALREADY SET (M1) | `server.port: 33400` already configured |
| `UserRepository.java` | ALREADY EXISTS (M3) | Reuse for balance lookup |

#### Key Design Pattern

```java
@RestController
public class BalanceController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam String userId) {
        return userRepository.findById(userId)
            .map(user -> new Balance(userId, user.getBalance()))
            .orElse(new Balance(userId, 0.0));
    }
}
```

> **Critical Rule**: Do NOT modify `Balance.toString()` — it is used for automated test verification.  
> **Critical Rule**: Do NOT change the server port from `33400`.

---

### 5.5 Dependencies

| Dependency | Module |
|---|---|
| `UserRepository` for balance lookup | M3 |
| `application.yml` `server.port: 33400` | M1 |
| Incentive API running (for full pipeline test) | M4 |
| All previous modules passing | M1–M4 |

---

### 5.6 Proprietary Test Suite

**Test Class**: `TaskFiveTests.java` (provided in scaffold)

> **Pre-condition**: Incentive API JAR MUST be running before executing this test.
> ```bash
> java -jar services/incentive-api.jar
> ```

| Test ID | What It Verifies | Pass Condition |
|---------|-----------------|----------------|
| T5-01 | `BalanceController` bean registered in Spring context | No NoSuchBeanDefinitionException |
| T5-02 | Server listens on port `33400` | HTTP response received on `localhost:33400` |
| T5-03 | `GET /balance?userId=<valid>` returns correct balance | JSON response matches expected balance value |
| T5-04 | `GET /balance?userId=<unknown>` returns `amount: 0.0` | 200 OK with `{ "amount": 0.0 }` |
| T5-05 | REST endpoint works while Kafka listener is active | Concurrent operation — no deadlock or stale reads |
| T5-06 | `Balance.toString()` format is unchanged | Test verification uses toString() — must match scaffold format |
| T5-07 | BEGIN/END markers appear in console output | Capture for submission |

**Run Command**:
```bash
# Step 1: Start Incentive API
java -jar services/incentive-api.jar

# Step 2: Full regression run
mvn clean install

# Step 3: Run Task Five specifically
mvn test -Dtest=TaskFiveTests
```

---

### 5.7 Prototype Gate — M5

> **M5 is a Prototype when ALL of the following are true:**

- [ ] `TaskFiveTests` passes 100%
- [ ] **ALL FIVE** task test suites pass in a full `mvn clean install` run
- [ ] `GET /balance?userId=waldorf` returns the correct balance via HTTP
- [ ] `GET /balance?userId=unknownUser` returns `{ "amount": 0.0 }` — NOT an error
- [ ] `Balance.toString()` is **unmodified** from scaffold
- [ ] Kafka listener is active AND REST endpoint responds correctly simultaneously
- [ ] BEGIN/END console output captured for Task 5 submission

**Status**: Not Started → In Progress → PROTOTYPE

---

## System-Level Prototype Gate (Final Milestone)

> The entire **Midas Core system** is a working prototype when:

| # | Criterion |
|---|-----------|
| 1 | All 5 TaskXxxTests suites pass — `mvn clean install` returns BUILD SUCCESS |
| 2 | BEGIN/END markers captured for ALL 5 tasks (submission requirement) |
| 3 | Debugger-inspected balances verified: `waldorf` (Task 3), `wilbur` (Task 4) |
| 4 | No scaffold-provided files modified: `Transaction.java`, `Balance.toString()` |
| 5 | No hard-coded config values — all environment values injected via `application.yml` |
| 6 | Full pipeline demonstrated: Kafka → validation → incentive → persistence → HTTP query |

---

## Module Summary Table

| Module | Name | Task | Key Output | Prototype Test | Est. Effort |
|--------|------|------|-----------|----------------|-------------|
| M1 | Project Foundation | Task 1 | Working scaffold + config | `TaskOneTests` | 0.5 day |
| M2 | Kafka Ingestion Layer | Task 2 | `KafkaListener.java` | `TaskTwoTests` | 1 day |
| M3 | Validation & Persistence | Task 3 | `TransactionService` + JPA entities | `TaskThreeTests` | 1.5 days |
| M4 | Incentive API Client | Task 4 | `IncentiveClient.java` + updated balance logic | `TaskFourTests` | 1 day |
| M5 | Balance REST API | Task 5 | `BalanceController.java` | `TaskFiveTests` | 0.5 day |
| FULL | System Prototype | — | End-to-end pipeline | All 5 suites | ~5 days total |

---

## Key Engineering Principles Per Module

| Principle | Module(s) |
|---|---|
| Separation of Concerns | All — each module owns exactly one layer |
| API as a Contract | M4 — Incentive API is a black box; only the interface matters |
| Externalized Configuration | M1, M2 — no hard-coded topic names, ports, or URLs |
| Async / Resilient Architecture | M2 — Kafka absorbs producer bursts independently |
| SQL for Financial Integrity | M3 — atomic writes prevent partial balance mutations |
| JPA Abstraction | M3 — H2 can be swapped for PostgreSQL with zero business logic changes |
| Test-Driven Verification | M1–M5 — every module gates on automated test suite |
| Practical Architecture | M5 — balance endpoint in Midas Core, not a new microservice |

---

*Document created as part of Phase 1 — Documents & Designing*  
*Source: `Midas_Core_Specification.docx` + `Project_Understanding_Draft.md`*
