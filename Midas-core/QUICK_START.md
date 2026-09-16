# ⚡ **QUICK START GUIDE — Midas Core**

Get Midas Core running in minutes.

---

## 🚀 **5-Minute Setup**

### **Option 1: Local Development (Fastest)**

```bash
# Clone and setup
git clone <repo-url>
cd forage-midas

# Build
mvn clean install

# Run
mvn spring-boot:run

# Test
curl -H "X-API-Key: midas-dev-key-2026" http://localhost:33400/actuator/health
```

**Time**: 2-3 minutes  
**Database**: H2 (in-memory)  
**Port**: 33400

---

### **Option 2: Docker Compose (Production-like)**

```bash
cd forage-midas

# Start
docker-compose up -d

# Check status
docker-compose ps

# Test
curl -H "X-API-Key: midas-dev-key-2026" http://localhost:33400/actuator/health

# View logs
docker-compose logs -f midas-core

# Stop
docker-compose down
```

**Time**: 1-2 minutes  
**Database**: PostgreSQL (persistent)  
**Includes**: Multi-service setup

---

## 🧪 **Run Tests**

```bash
# All tests (80/80)
mvn test

# Specific test
mvn test -Dtest=UpgradeFourTests

# With coverage
mvn test jacoco:report
open target/site/jacoco/index.html
```

---

## 📊 **Check System Status**

```bash
# Health check
curl -H "X-API-Key: midas-dev-key-2026" \
  http://localhost:33400/actuator/health

# Metrics
curl -H "X-API-Key: midas-dev-key-2026" \
  http://localhost:33400/actuator/metrics

# Transactions received
curl -H "X-API-Key: midas-dev-key-2026" \
  http://localhost:33400/actuator/metrics/midas.transactions.received
```

---

## 📚 **Key Files**

| File | Purpose |
|------|---------|
| `README.md` | Full project documentation |
| `pom.xml` | Maven dependencies |
| `Dockerfile` | Container image |
| `docker-compose.yml` | Multi-service setup |
| `application.yml` | Dev configuration |
| `application-prod.yml` | Prod configuration |
| `.github/workflows/ci.yml` | CI/CD pipeline |

---

## 🔧 **Key Commands**

```bash
# Build
mvn clean install
mvn clean compile
mvn clean package -DskipTests

# Test
mvn test
mvn test -Dtest=TaskOneTests
mvn test -Dtest=Upgrade*

# Run
mvn spring-boot:run
java -jar target/forage-midas-0.0.1-SNAPSHOT.jar

# Docker
docker build -t midas-core:latest .
docker-compose up -d
docker-compose logs -f
docker-compose down

# Database
psql -h localhost -U midas_user -d midas
```

---

## 📈 **Tech Stack Overview**

```
Java 17 + Spring Boot 3.2.5
├── Web: Spring MVC
├── Data: Spring Data JPA
├── Kafka: spring-kafka
├── Resilience: Resilience4j
├── Security: Spring Security
├── Monitoring: Spring Boot Actuator
├── Database: PostgreSQL (prod) / H2 (dev)
└── Container: Docker + docker-compose
```

---

## 🎯 **What This System Does**

1. **Receives** financial transactions from Kafka
2. **Validates** using 4 business rules
3. **Calls** external Incentive API (with circuit breaker)
4. **Updates** user balances (with BigDecimal precision)
5. **Persists** transactions to PostgreSQL
6. **Tracks** metrics (received, valid, rejected)
7. **Exposes** REST API for balance queries
8. **Authenticates** via API key header

---

## ✅ **Verification Checklist**

After setup:

- [ ] `docker-compose ps` shows 2 running containers
- [ ] Health endpoint returns `{"status":"UP"}`
- [ ] Metrics endpoint returns counter data
- [ ] Logs show no ERROR messages
- [ ] All 80 tests passing
- [ ] API requires X-API-Key header

---

## 🚨 **Troubleshooting**

| Problem | Fix |
|---------|-----|
| Port 33400 in use | `lsof -i :33400 \| kill -9` |
| Docker won't start | `docker-compose down -v && docker-compose up` |
| Tests failing | `mvn clean install` |
| DB connection error | Check `docker-compose logs postgres` |
| API returns 401 | Add `-H "X-API-Key: midas-dev-key-2026"` |

---

## 📖 **Full Documentation**

See `README.md` for:
- Complete architecture design
- Detailed implementation guide
- API documentation
- Deployment procedures
- Development guide

---

## 🏆 **System Status**

✅ Production-Ready  
✅ 80/80 Tests Passing  
✅ FFWP Certified  
✅ Ready to Deploy

---

**Get started now!** 🚀
