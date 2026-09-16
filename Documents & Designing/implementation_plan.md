# Midas Core — Implementation Plan
### JP Morgan Chase Software Engineering Simulation

## Background

**Midas Core** is a Spring Boot financial transaction processing service. It integrates:
- **Apache Kafka** — async transaction ingestion
- **Spring Data JPA + H2** — validation & persistence
- **External Incentive REST API** — enrichment
- **REST Controller** — balance querying

The design documents in `phase-1_Documents&Designing/` define a 5-module, gated build plan. We must implement each module sequentially, passing its prototype test gate before moving to the next.

---

## Document Reading Order (Already Analyzed)

| # | Document | Purpose |
|---|----------|---------|
| 1 | `Project_Understanding_Draft.md` | High-level overview of Midas — tasks, tech stack |
| 2 | `Midas_Core_Module_Decomposition.md` | Per-module spec: files to create, validation rules, test gates |
| 3 | `Midas_Core_MVP_Integration.md` | Integration test suite, acceptance criteria, risk register |
| 4 | `Midas_Core_Post_MVP_Upgrade.md` | FFWP enhancements (BigDecimal, idempotency, circuit breakers, etc.) |
| 5 | `Midas_Core_Commercial_Ready.md` | Production-grade concerns (security, compliance, ops) |

> **Immediate goal**: Implement M1 → M5 to achieve MVP certification (the JP Morgan simulation deliverable).
> Post-MVP upgrades and CRP phases are documented for awareness but are out of scope for the initial implementation.

---

## Open Questions

> [!IMPORTANT]
> **Q1**: Has the scaffold repo (`https://github.com/vagabond-systems/forage-midas`) already been forked and cloned locally? If so, where on disk is it located? I need to know the path before touching any source files.

> [!IMPORTANT]
> **Q2**: Is the `services/incentive-api.jar` present in the scaffold repo? This is required for Tasks 4 and 5 tests.

> [!NOTE]
> **Q3**: What is your preferred execution mode?
> - **Option A** — I implement and commit all 5 modules fully (code-complete), you run the tests yourself in IntelliJ/terminal.
> - **Option B** — I implement module by module, and after each module you confirm the test passes before I proceed.

---

## Proposed Implementation Sequence

### Phase 0 — Verify Prerequisites

- Confirm scaffold repo is cloned and accessible
- Confirm Java 17 + Maven installed
- Confirm `services/incentive-api.jar` is present

---

### Phase 1 — MODULE 1: Project Foundation (Task 1)

**Files to modify:**
#### [MODIFY] `pom.xml`
Add 7 Maven dependencies (spring-boot-starter-data-jpa, spring-boot-starter-web, spring-kafka, h2, spring-boot-starter-test, spring-kafka-test, testcontainers kafka).

#### [MODIFY] `src/main/resources/application.yml`
Add three configuration blocks:
```yaml
general:
  kafka-topic: trader-updates
server:
  port: 33400
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
```

**Gate**: `mvn clean install` → BUILD SUCCESS + `TaskOneTests` PASS

---

### Phase 2 — MODULE 2: Kafka Ingestion Layer (Task 2)

**Files to create:**
#### [NEW] `KafkaListener.java`
- `@Component` class with `@KafkaListener(topics = "${general.kafka-topic}")`
- `listen(Transaction transaction)` method
- Delegates to `TransactionService` (wired in M3)

**Gate**: `TaskTwoTests` PASS — first 4 amounts: 122.86, 42.87, 161.79, 22.22

---

### Phase 3 — MODULE 3: Validation & Persistence (Task 3)

**Files to create:**
#### [NEW] `TransactionRecord.java`
- `@Entity` with `@ManyToOne User sender`, `@ManyToOne User recipient`, `double amount`, `double incentive`

#### [NEW] `TransactionService.java`
- `@Service` with validation rules VR-01, VR-02, VR-03
- Debit sender, credit recipient
- Save `TransactionRecord` via repository
- `@Transactional` to ensure atomic writes

#### [NEW] `UserRepository.java`
- `JpaRepository<User, String>`

#### [NEW] `TransactionRecordRepository.java`
- `JpaRepository<TransactionRecord, Long>`

#### [MODIFY/CREATE] `domain/User.java`
- `@Entity` with `@Id String id`, `double balance`

**Gate**: `TaskThreeTests` PASS — inspect `waldorf` final balance in debugger

---

### Phase 4 — MODULE 4: Incentive API Client (Task 4)

**Files to create:**
#### [NEW] `IncentiveClient.java`
- `@Component` using `RestTemplate.postForObject("http://localhost:8080/incentive", transaction, Incentive.class)`

#### [NEW] `domain/Incentive.java`
- DTO with single field `double amount`

#### [MODIFY] `TransactionService.java`
- After validation: call `IncentiveClient.getIncentive(transaction)`
- `recipient.balance += amount + incentive.amount`
- `sender.balance -= amount` (incentive NOT deducted from sender)
- Store `incentive.amount` in `TransactionRecord.incentive`

**Pre-condition**: `java -jar services/incentive-api.jar` must be running
**Gate**: `TaskFourTests` PASS — inspect `wilbur` final balance in debugger

---

### Phase 5 — MODULE 5: Balance REST API (Task 5)

**Files to create:**
#### [NEW] `BalanceController.java`
- `@RestController` with `GET /balance?userId=...`
- Returns `Balance` DTO: `{ userId, amount }`
- Unknown user → `{ amount: 0.0 }` (200 OK, no 404)

#### [NEW] `domain/Balance.java`
- DTO with `String userId`, `double amount`
- **Do NOT modify** `toString()` format

**Gate**: `TaskFiveTests` PASS + BEGIN/END markers captured

---

### Phase 6 — MVP Integration Verification

Run full regression:
```bash
java -jar services/incentive-api.jar  # Terminal 1
mvn clean install                      # Terminal 2
```

All 5 task test suites must pass → **MVP CERTIFIED** ✅

---

## File Creation Summary

| File | Module | Action |
|------|--------|--------|
| `pom.xml` | M1 | MODIFY — add 7 dependencies |
| `application.yml` | M1 | MODIFY — add 3 config blocks |
| `KafkaListener.java` | M2 | CREATE |
| `domain/User.java` | M3 | CREATE or verify scaffold |
| `TransactionRecord.java` | M3 | CREATE |
| `TransactionService.java` | M3 | CREATE |
| `UserRepository.java` | M3 | CREATE |
| `TransactionRecordRepository.java` | M3 | CREATE |
| `IncentiveClient.java` | M4 | CREATE |
| `domain/Incentive.java` | M4 | CREATE |
| `BalanceController.java` | M5 | CREATE |
| `domain/Balance.java` | M5 | CREATE |

---

## Verification Plan

### Per-Module Tests (Automated)
```bash
mvn test -Dtest=TaskOneTests
mvn test -Dtest=TaskTwoTests
mvn test -Dtest=TaskThreeTests
# Start incentive-api.jar first for Tasks 4 & 5
mvn test -Dtest=TaskFourTests
mvn test -Dtest=TaskFiveTests
```

### MVP Final Regression
```bash
java -jar services/incentive-api.jar &
mvn clean install
```
Expected: `BUILD SUCCESS` with 0 failures.

### Manual Debugger Verification
- Task 3: Inspect `waldorf.balance` (rounded-down integer for submission)
- Task 4: Inspect `wilbur.balance` (rounded-down integer for submission)
