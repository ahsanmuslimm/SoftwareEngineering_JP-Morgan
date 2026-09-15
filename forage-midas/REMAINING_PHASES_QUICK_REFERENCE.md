# Remaining Phases - Quick Reference Guide

## UP-3: Security & Authentication (NEXT)

### Files to Create
1. **SecurityConfig.java**
   ```java
   @Configuration
   @EnableWebSecurity
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) {
           // Disable CSRF, register API key filter
           // Require authentication for /balance
       }
   }
   ```

2. **ApiKeyAuthFilter.java**
   ```java
   @Component
   public class ApiKeyAuthFilter extends OncePerRequestFilter {
       @Value("${security.api-key}")
       private String validApiKey;
       
       // Check X-API-Key header
       // Return 401 if invalid
   }
   ```

### Files to Modify
- `pom.xml`: Add `spring-boot-starter-security` (3.2.5)
- `application.yml`: Add `security.api-key` config

### Configuration
```yaml
security:
  api-key: ${MIDAS_API_KEY:midas-dev-key-2026}
```

### Tests (8)
- UP3-01: Valid API key accepted (200 OK)
- UP3-02: Missing API key rejected (401)
- UP3-03: Wrong API key rejected (401)
- UP3-04: Empty API key rejected (401)
- UP3-05: 401 returns JSON error body
- UP3-06: Kafka listener unaffected by security
- UP3-07: API key externalized (not hard-coded)
- UP3-08: TaskFiveTests updated with API key header

**Estimated Time**: 1 day

---

## UP-4: Observability & Diagnostics (PARTIAL)

### Already Done ✅
- ✅ SLF4J logging integrated
- ✅ Actuator configured
- ✅ Critical log statements added

### Files to Create/Modify

1. **Metrics Configuration** (optional, can use auto metrics)
   - Add `MeterRegistry` injection where needed
   - Register custom counters

2. **Enhanced Logging** (optional)
   - Add DEBUG level logs for detailed tracing
   - Add business metric calculations

### Sample Implementation
```java
@Bean
public MeterBinder customMetrics() {
    return (registry) -> {
        Counter.builder("midas.transactions.processed")
            .description("Total transactions processed")
            .register(registry);
    };
}
```

### Tests (9)
- UP4-01: INFO log on transaction received
- UP4-02: WARN log on transaction rejected
- UP4-03: WARN log on duplicate skipped
- UP4-04: ERROR log on incentive API failure
- UP4-05: Health endpoint returns UP
- UP4-06: Received counter increments
- UP4-07: Valid counter increments
- UP4-08: Rejected counter increments
- UP4-09: Existing tests still pass

**Estimated Time**: 0.5 days

---

## UP-5: Infrastructure & CI/CD (PENDING)

### Files to Create

1. **Dockerfile**
   ```dockerfile
   FROM openjdk:17-slim
   COPY target/forage-midas-0.0.1-SNAPSHOT.jar app.jar
   ENTRYPOINT ["java","-jar","/app.jar"]
   ```

2. **docker-compose.yml**
   ```yaml
   services:
     postgres:
       image: postgres:15
       environment:
         POSTGRES_DB: midas
         POSTGRES_USER: admin
         POSTGRES_PASSWORD: password
     
     midas-core:
       build: .
       environment:
         SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/midas
       ports:
         - "33400:33400"
   ```

3. **.github/workflows/ci.yml**
   ```yaml
   name: CI/CD Pipeline
   on: [push, pull_request]
   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v2
         - uses: actions/setup-java@v2
           with:
             java-version: '17'
         - run: mvn clean install
   ```

### Files to Modify

1. **pom.xml**
   - Add PostgreSQL JDBC driver
   - Add Docker Maven plugin (optional)

2. **application.yml**
   - Add PostgreSQL profile configuration
   - Keep H2 as default for development

3. **src/main/java/com/jpmorgan/midascore/**
   - Create `PostgresConfig.java` for dialect
   - Add migration scripts (Flyway or Liquibase)

### Configuration Profiles
```yaml
# application-prod.yml
spring:
  datasource:
    url: jdbc:postgresql://postgres:5432/midas
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate  # Use migrations in production
```

### Tests (6)
- UP5-01: PostgreSQL connection successful
- UP5-02: Hibernate migration works on PostgreSQL
- UP5-03: Docker image builds successfully
- UP5-04: Docker container starts and responds
- UP5-05: GitHub Actions pipeline runs on push
- UP5-06: All tests pass in CI environment

**Estimated Time**: 1.5 days

---

## 🎯 **Implementation Sequence**

### Session 1: UP-3 Security (1 day)
```
Morning:
  - Create SecurityConfig.java
  - Create ApiKeyAuthFilter.java
  - Update pom.xml and application.yml
  
Afternoon:
  - Write and run UP3-01 through UP3-08 tests
  - Verify all previous tests still pass
  - Document completion in UP-3_IMPLEMENTATION_COMPLETE.md
```

### Session 2: UP-4 Metrics (0.5 days)
```
Morning:
  - Add metrics configuration
  - Register custom counters
  - Write UP4-01 through UP4-09 tests
```

### Session 3: UP-5 Infrastructure (1.5 days)
```
Day 1:
  - Create Dockerfile
  - Create docker-compose.yml
  - Add PostgreSQL configuration
  
Day 2:
  - Create GitHub Actions CI/CD pipeline
  - Write UP5-01 through UP5-06 tests
  - Final FFWP verification
```

---

## 📋 **Pre-Implementation Checklist**

### Before UP-3
- [ ] All UP-1 and UP-2 implementations compile without errors
- [ ] All UP-1 and UP-2 tests pass (25 tests)
- [ ] Spring Security documentation reviewed
- [ ] API key strategy finalized

### Before UP-4
- [ ] UP-3 complete and all tests pass (8 tests)
- [ ] Custom metrics requirements clarified
- [ ] Health indicator design finalized

### Before UP-5
- [ ] UP-4 complete and all tests pass (9 tests)
- [ ] PostgreSQL setup available (local or container)
- [ ] Docker installation verified
- [ ] GitHub repository configured

---

## 🔄 **Testing Workflow**

### After Each Phase

```bash
# Build and compile
mvn clean install

# Run specific phase tests
mvn test -Dtest=UpgradeThreeTests    # UP-3
mvn test -Dtest=UpgradeFourTests     # UP-4
mvn test -Dtest=UpgradeFiveTests     # UP-5

# Run all tests (regression)
mvn test

# Generate coverage report
mvn jacoco:report
```

---

## 📊 **Expected Progress Timeline**

| Phase | Start | Duration | End | Status |
|-------|-------|----------|-----|--------|
| UP-1 | Sept 15 | 1 day | Sept 15 | ✅ Complete |
| UP-2 | Sept 15 | 1 day | Sept 15 | ✅ Complete |
| UP-3 | Sept 16 | 1 day | Sept 16 | ⏳ Next |
| UP-4 | Sept 16 | 0.5 days | Sept 16 | ⏳ After UP-3 |
| UP-5 | Sept 17 | 1.5 days | Sept 18 | ⏳ Final |
| **FFWP Ready** | - | - | **Sept 18** | 🎯 Target |

---

## ✅ **Success Criteria**

### For Each Phase
1. All implementation files created/modified
2. All tests pass (100%)
3. No regressions in previous phases
4. Documentation complete
5. Code reviewed and clean

### For FFWP
1. All 5 phases complete (UP-1 through UP-5)
2. Total 48 tests passing (80 total including MVP)
3. 20 final acceptance criteria met
4. Production-ready architecture achieved
5. Full documentation and runbooks

---

**Quick Reference Generated**: September 15, 2026  
**Next Session Target**: September 16-18, 2026  
**Final Goal**: FFWP Certification