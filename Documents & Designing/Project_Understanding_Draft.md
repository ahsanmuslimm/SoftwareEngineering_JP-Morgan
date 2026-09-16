# 📘 Project Understanding Draft
### JP Morgan Chase — Software Engineering Job Simulation
> **Program**: Forage × JPMorganChase Software Engineering Virtual Internship
> **Project Name**: **Midas** — A Financial Transaction Processing System
> **Focus Component**: **Midas Core** — the transaction ingestion, validation, and balance management service
> **Author Draft Date**: September 2026

---

## 1. 🌐 What Is the Midas System?

The **Midas system** is a high-profile, enterprise-grade backend platform responsible for **processing financial transactions at scale**. It is built using the **Java + Spring Boot** ecosystem and simulates how real backend engineering teams at JPMorganChase architect and implement transaction pipelines.

The system is composed of **multiple interacting components**:

| Component         | Role                                                                          |
|-------------------|-------------------------------------------------------------------------------|
| **Midas Core**    | The central service: receives, validates, persists transactions, exposes APIs |
| **Kafka Broker**  | Message queue between transaction producers (frontends) and Midas Core        |
| **H2 Database**   | In-memory SQL database for persisting users, transactions, and balances       |
| **Incentive API** | External REST microservice that computes incentive amounts per transaction     |

> **This simulation focuses entirely on building Midas Core from a scaffold** — implementing one integration layer at a time across 5 progressive tasks.

---

## 2. 🏗️ System Architecture Overview

```
+-------------------------+
|   Transaction Producers  |  (Frontends, Trading Systems)
|   (External / Frontend)  |
+----------+--------------+
           |  Publishes messages to
           v
+----------------------------------+
|        Apache Kafka Broker        |
|  Topic: "trader-updates"          |
+----------+------------------------+
           |  KafkaListener consumes
           v
+----------------------------------------------------------+
|                     MIDAS CORE (Spring Boot)              |
|                                                           |
|  +----------------+   +--------------------------------+  |
|  |  Kafka Listener |-->|  Transaction Validation Logic  |  |
|  +----------------+   +--------------+-----------------+  |
|                                       |                   |
|              +------------------------+                   |
|              |                        |                   |
|              v                        v                   |
|  +---------------------+  +--------------------------+   |
|  |  H2 SQL Database     |  |   Incentive REST API Call |   |
|  |  (JPA / Spring Data) |  |   (RestTemplate -> POST)  |   |
|  |  - Users             |  |   http://localhost:8080   |   |
|  |  - TransactionRecord |  |   /incentive              |   |
|  |  - Balances          |  +--------------------------+   |
|  +---------------------+                                  |
|                                                           |
|  +-----------------------------------------------------+  |
|  |  REST Controller (GET /balance?userId=...)           |  |
|  |  Port: 33400                                         |  |
|  +-----------------------------------------------------+  |
+----------------------------------------------------------+
           |
           v
+------------------------------+
|   API Consumers / Users       |
|  (Balance Queries via HTTP)   |
+------------------------------+
```

---

## 3. 📋 Task-by-Task Breakdown

### Task 1 — Project Setup & Environment Configuration

**Goal**: Set up the local development environment, understand the scaffold, and add all required dependencies.

**Key Steps**:
1. Fork and clone: https://github.com/vagabond-systems/forage-midas
2. Open in IntelliJ IDEA (recommended for Spring Boot)
3. Install and configure Java 17
4. Add Maven dependencies to pom.xml:

| Dependency                     | Group ID                  | Version  |
|--------------------------------|---------------------------|----------|
| spring-boot-starter-data-jpa   | org.springframework.boot  | 3.2.5    |
| spring-boot-starter-web        | org.springframework.boot  | 3.2.5    |
| spring-kafka                   | org.springframework.kafka | 3.1.4    |
| h2                             | com.h2database            | 2.2.224  |
| spring-boot-starter-test       | org.springframework.boot  | 3.2.5    |
| spring-kafka-test              | org.springframework.kafka | 3.1.4    |
| kafka (Testcontainers)         | org.testcontainers        | 1.19.1   |

5. Update application.yml:
   general:
     kafka-topic: trader-updates

6. Build: mvn clean install | Run: mvn spring-boot:run
7. Run TaskOneTests — capture BEGIN/END output snippet.

**What it teaches**: Project scaffolding, Maven dependency management, Spring Boot configuration.

---

### Task 2 — Apache Kafka Integration

**Goal**: Implement a Kafka listener that consumes transaction messages from the configured topic.

**Why Kafka?**
- Decoupling — frontend and backend are fully independent
- Async communication — burst tolerance
- Scalability — multiple producers/consumers; acts as a load balancer

**Implementation Requirements**:
- Create a class with @KafkaListener annotation
- Read topic from application.yml via ${general.kafka-topic}
- Deserialize messages into the provided Transaction domain class
- No need to specify host/port — tests use embedded Kafka (autowired)

**Verification**:
- Run TaskTwoTests
- Inspect first 4 transactions via debugger
- Correct amounts: 122.86, 42.87, 161.79, 22.22

**What it teaches**: Event-driven architecture, Kafka listener config, message deserialization.

---

### Task 3 — H2 Database Integration (Spring Data JPA)

**Goal**: Integrate an H2 in-memory SQL database; implement transaction validation and persistence.

**Why SQL?**
- SQL chosen over NoSQL for stronger failure guarantees (critical for financial data)
- H2 simplifies development; JPA abstraction makes production DB swap trivial

**Validation Rules** — A transaction is valid only if:
1. senderId is a valid, existing user
2. recipientId is a valid, existing user
3. Sender balance >= transaction amount

**If valid**:
- Persist as a TransactionRecord JPA entity
- Deduct amount from sender balance
- Add amount to recipient balance

**If invalid**: Discard — no DB changes.

**Entity Design**:
- Create TransactionRecord class with @Entity annotation (do NOT modify Transaction class)
- TransactionRecord has Many-to-One relationship with User for sender and recipient
- User entity has One-to-Many with its transactions

**Verification**:
- Run TaskThreeTests
- Inspect waldorf user final balance in debugger (rounded down integer)

**What it teaches**: JPA entity modeling, relational data design, business rule enforcement, Kafka + DB pipeline.

---

### Task 4 — External REST API Integration (Incentive API)

**Goal**: Connect Midas Core to an external Incentive API; incorporate incentives into transaction processing.

**API as a Contract**:
- Incentive API is a black box — logic is irrelevant; only the API contract matters
- Two teams work independently as long as the contract is not broken

**Incentive API Details**:
- Run provided JAR from services/ folder locally
- Base URL: http://localhost:8080
- Endpoint: POST /incentive
- Request: JSON-serialized Transaction object
- Response: { "amount": <double> } (amount >= 0)

**Updated Balance Logic**:
- POST transaction to Incentive API after validation
- Record incentive.amount in a new incentive field on TransactionRecord
- Recipient balance += transaction amount + incentive amount
- Sender balance -= transaction amount ONLY (incentive NOT deducted from sender)

**Implementation**:
- Use RestTemplate.postForObject() or postForEntity()
- Let Spring handle serialization — pass Transaction object directly
- Deserialize response into Incentive class with single amount field

**Verification**:
- Run TaskFourTests
- Inspect wilbur user final balance (rounded down integer)

**What it teaches**: Service-to-service communication, RestTemplate, API contract design, microservice thinking.

---

### Task 5 — REST API Controller (Expose Balance Endpoint)

**Goal**: Expose a REST endpoint inside Midas Core for querying user account balances.

**Architectural Decision**:
Option A: Add to Midas Core (chosen)
  - Pros: simpler, lower deployment burden, Spring makes it trivial
  - Cons: slightly muddies Midas Core single-purpose focus

Option B: New microservice
  - Pros: cleaner architecture
  - Cons: more overhead, new deployment unit

Decision: Add to Midas Core now. If balance endpoint grows, extract later.
Principle: Good architecture balances elegance with practicality.

**Implementation Requirements**:
- Create @RestController class in Midas Core
- Expose GET /balance endpoint
- Accept userId as @RequestParam
- Look up user in database
- Return Balance object serialized to JSON
- If user not found: return Balance with amount = 0
- Configure application port: 33400
- Do NOT modify Balance.toString() (used for test verification)

**Integration Note**:
- REST Controller runs ALONGSIDE the Kafka listener — same Spring Boot app
- Configure in application.yml: server.port: 33400

**Verification**:
- Ensure Incentive API JAR is running first
- Run TaskFiveTests
- Submit BEGIN/END output snippet

**What it teaches**: Spring REST controllers, JSON serialization, architectural trade-off reasoning.

---

## 4. 🧩 How the Tasks Connect — Full Development Journey

Task 1: Foundation
    |  Set up project scaffold, dependencies, configuration
    v
Task 2: Kafka Integration
    |  Midas Core can now RECEIVE transaction messages
    v
Task 3: H2 Database
    |  Midas Core can now VALIDATE and PERSIST transactions
    |  + Manage user balances
    v
Task 4: Incentive API
    |  Midas Core can now ENRICH transactions with incentives
    |  + Adjust balances with incentive logic
    v
Task 5: REST Controller
       Midas Core can now SURFACE user balances via HTTP API
       => System is fully functional end-to-end

Each task builds on the previous one. By the end:
- Kafka feeds transactions in
- H2 + JPA validates and persists them
- Incentive API enriches each transaction
- REST Controller lets users query their balance

---

## 5. 🔧 Technology Stack Summary

| Technology               | Purpose                                           |
|--------------------------|---------------------------------------------------|
| Java 17                  | Core language                                     |
| Spring Boot 3.2.5        | Application framework, DI, auto-configuration     |
| Apache Kafka 3.1.4       | Async message queue for transaction events        |
| Spring Data JPA          | ORM layer for database interactions               |
| H2 Database 2.2.224      | In-memory SQL database (dev/test)                 |
| RestTemplate             | HTTP client for Incentive API calls               |
| Maven                    | Build tool and dependency management              |
| Spring Boot Test         | Integration testing framework                     |
| Testcontainers 1.19.1    | Embedded Kafka for test isolation                 |

---

## 6. 📦 Key Domain Classes

| Class              | Type       | Description                                                      |
|--------------------|------------|------------------------------------------------------------------|
| Transaction        | DTO        | Incoming Kafka message: senderId, recipientId, amount            |
| TransactionRecord  | JPA Entity | Persisted transaction with sender/recipient (Many-to-One User)   |
| User               | JPA Entity | User account with ID and balance                                 |
| Incentive          | DTO        | Response from Incentive API — single field: amount               |
| Balance            | DTO        | Response for GET /balance — used for JSON API response           |

---

## 7. 🗂️ Expected Project Structure

forage-midas/
├── src/
│   ├── main/
│   │   ├── java/com/.../midascore/
│   │   │   ├── KafkaListener.java        <- Task 2
│   │   │   ├── TransactionRecord.java    <- Task 3
│   │   │   ├── TransactionService.java   <- Tasks 3, 4
│   │   │   ├── BalanceController.java    <- Task 5
│   │   │   ├── IncentiveClient.java      <- Task 4
│   │   │   └── domain/
│   │   │       ├── Transaction.java      <- Provided scaffold
│   │   │       ├── User.java             <- Provided scaffold
│   │   │       ├── Incentive.java        <- Task 4
│   │   │       └── Balance.java          <- Task 5
│   │   └── resources/
│   │       └── application.yml           <- Task 1 config
│   └── test/
│       ├── TaskOneTests.java
│       ├── TaskTwoTests.java
│       ├── TaskThreeTests.java
│       ├── TaskFourTests.java
│       └── TaskFiveTests.java
├── services/
│   └── incentive-api.jar                 <- Task 4 - run locally
└── pom.xml                               <- Task 1 - dependencies

---

## 8. ⚙️ Configuration Summary (application.yml)

general:
  kafka-topic: trader-updates    # Task 2 - Kafka topic name

server:
  port: 33400                    # Task 5 - REST API port

spring:
  datasource:
    url: jdbc:h2:mem:testdb      # Task 3 - H2 in-memory DB
    driver-class-name: org.h2.Driver
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop

---

## 9. 🏅 Skills Demonstrated (Resume Summary)

From the official JP Morgan resume snippet:

1. Kafka Integration — Consumed high-volume transaction messages using @KafkaListener,
   configurable topic, and embedded Kafka test framework.

2. Database Design & Persistence — Implemented transaction validation, JPA entity modeling,
   and balance updates across relational User records using Spring Data JPA + H2.

3. External REST API Consumption — Connected to Incentive API using RestTemplate,
   processed JSON responses, and incorporated incentive logic into transactional workflows.

4. REST API Exposure — Developed GET /balance endpoint in Spring REST controller,
   returning JSON responses while maintaining clean architectural boundaries.

5. Testing & Verification — Verified system behavior using Maven test suites and
   debugger-driven inspection across all integration layers.

---

## 10. 💡 Key Software Engineering Principles Illustrated

| Principle                        | Where Applied                                                |
|----------------------------------|--------------------------------------------------------------|
| Separation of Concerns           | Kafka, DB, Incentive API, REST API are separate layers       |
| API as Contract                  | Incentive API separates two team domains safely              |
| Avoid Premature Over-Engineering | Balance endpoint added to Midas Core, not a new service      |
| Abstraction via JPA              | DB backend abstracted; production swap requires no code change|
| Async / Resilient Architecture   | Kafka allows burst tolerance and independent scaling         |
| SQL for Financial Data           | Chosen over NoSQL for stronger failure guarantees            |
| Test-Driven Verification         | Each task has automated test suite with BEGIN/END markers    |

---

## 11. 📌 Open Questions / Notes for Implementation Phase

- [ ] Does the provided scaffold include a User entity with @Entity or must it be created?
- [ ] Is a data.sql seed file (for initial user data) provided or must it be authored?
- [ ] What is the exact shape of the Transaction class? (Fields: senderId, recipientId, amount + timestamps?)
- [ ] Is the Incentive API JAR pre-built and included in the cloned repo under services/?
- [ ] Does the scaffold include any partially-implemented service classes to complete?
- [ ] Original repository: https://github.com/vagabond-systems/forage-midas

---

Document created as part of Phase 1 — Documents & Designing
Location: phase-1_Documents&Designing/Project_Understanding_Draft.md
