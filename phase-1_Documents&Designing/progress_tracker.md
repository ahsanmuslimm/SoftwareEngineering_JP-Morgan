# Midas Core — Progress Tracker

## Journey Map (from Design Docs)

```
Doc 1: Project Understanding     ✅ DONE (analyzed)
Doc 2: Module Decomposition      ✅ DONE (M1–M5 all built)
Doc 3: MVP Integration           ✅ DONE (all modules integrated)
Doc 4: Post-MVP Upgrade          ⬜ NOT STARTED
Doc 5: Commercial Ready          ⬜ NOT STARTED
```

---

## ✅ BUILT — MVP (Doc 2 + Doc 3)

| Module | What | Status |
|--------|------|--------|
| M1 | Project Foundation — `pom.xml`, `application.yml`, Spring Boot entry | ✅ Built |
| M2 | Kafka Listener — `@KafkaListener` on `trader-updates` topic | ✅ Built |
| M3 | Validation & Persistence — 3 rules, JPA entities, `@Transactional` | ✅ Built |
| M4 | Incentive Client — `RestTemplate` → `POST /incentive` | ✅ Built |
| M5 | Balance REST API — `GET /balance?userId=...` on port 33400 | ✅ Built |

**Files created**: 15 source + 5 tests = **20 files**  
**Test coverage**: 5 task test suites (TaskOneTests → TaskFiveTests)  
**MVP test target**: 32 integration tests across 6 suites (IT-1 → IT-6)

---

## ⬜ REMAINING — Post-MVP Upgrade → FFWP (Doc 4)

| Phase | What | Why |
|-------|------|-----|
| UP-1 | **BigDecimal migration** — replace all `double` with `BigDecimal` | `double` has floating-point precision errors (fatal for finance) |
| UP-2 | **Idempotency** — add unique transaction ID + dedup check | Kafka redelivery currently causes double-processing |
| UP-3 | **Circuit breaker** — Resilience4j `@CircuitBreaker` on Incentive API | If Incentive API goes down, entire pipeline crashes |
| UP-4 | **Structured logging** — SLF4J + Micrometer metrics | Zero observability currently |
| UP-5 | **Dead-letter queue** — handle malformed/failed Kafka messages | Bad messages currently vanish silently |

**Test target**: +44 tests, +8 acceptance criteria  
**Output**: Full Fledged Working Prototype (FFWP)

---

## ⬜ REMAINING — Commercial Ready Product (Doc 5)

| Phase | What | Why |
|-------|------|-----|
| CR-1 | **Security** — OAuth2/JWT, RBAC, TLS, secrets vault | Currently zero auth on `GET /balance` |
| CR-2 | **Data integrity** — audit trail, encryption at rest, PCI-DSS | No compliance controls |
| CR-3 | **Advanced testing** — load, stress, chaos, penetration tests | Only functional tests exist |
| CR-4 | **Compliance** — GDPR-aware, PCI-DSS Level controls | Not addressed |
| CR-5 | **Operations** — Kubernetes, blue-green deploy, SLA monitoring, runbooks | Currently local-only, no CI/CD |

**Test target**: +52 tests, +15 acceptance criteria  
**Output**: Commercial Ready Product (CRP)

---

## Totals at a Glance

| Phase | Tests | Acceptance Criteria | Status |
|-------|-------|--------------------|--------|
| MVP (Docs 2+3) | 32 | 12 | ✅ **BUILT** |
| Post-MVP → FFWP (Doc 4) | +44 | +8 | ⬜ Remaining |
| CRP (Doc 5) | +52 | +15 | ⬜ Remaining |
| **TOTAL** | **148** | **35** | **34% complete** |
