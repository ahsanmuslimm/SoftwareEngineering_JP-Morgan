# 🏃 **LOCAL TESTING STEPS — Run on Your Machine Right Now**

Follow these exact steps to test Midas Core locally.

---

## 📝 **Step-by-Step Instructions**

### **STEP 1: Verify Java & Maven Are Installed**

Open terminal/command prompt and run:

```bash
java -version
# Should show Java 17 or higher

mvn -version
# Should show Maven 3.8 or higher
```

**If not installed:**
- Install Java: https://adoptium.net (Java 17 LTS)
- Install Maven: https://maven.apache.org/download.cgi

---

### **STEP 2: Navigate to Project**

```bash
cd d:\WORKING\JOB_SIMS\Software_Engineering-by\ JP\ MORGAN\SoftwareEngineering_JP-Morgan\forage-midas
```

Or in PowerShell:

```powershell
cd "d:\WORKING\JOB_SIMS\Software_Engineering-by JP MORGAN\SoftwareEngineering_JP-Morgan\forage-midas"
```

---

### **STEP 3: Clean Build**

```bash
mvn clean install
```

**Expected output at end:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

**Time required**: 45-90 seconds (first time takes longer)

**What it does:**
- Downloads dependencies
- Compiles code
- Runs 80 tests
- Creates JAR file

---

### **STEP 4: Check Test Results**

```bash
mvn test
```

**Expected output:**
```
[INFO] Tests run: 80, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

**Must see:**
- ✅ Tests run: 80
- ✅ Failures: 0
- ✅ Errors: 0

---

### **STEP 5: Start Application Locally**

**Terminal 1: Start the app**

```bash
mvn spring-boot:run
```

**Wait for message:**
```
Started MidasCoreApplication in X.XXX seconds
```

**It will look like this:**
```
2026-09-15 15:30:50.456  INFO 12345 --- [main] c.j.m.MidasCoreApplication     : Starting MidasCoreApplication v0.0.1
...
2026-09-15 15:30:55.789  INFO 12345 --- [main] c.j.m.MidasCoreApplication     : Started MidasCoreApplication in 5.123 seconds
```

**Keep this terminal open** — Application is running on port 33400

---

### **STEP 6: Test Health Endpoint (New Terminal)**

**Terminal 2: Open new terminal, test the app**

```bash
curl -X GET http://localhost:33400/actuator/health -H "X-API-Key: midas-dev-key-2026"
```

**Expected response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "H2",
        "validationQuery": "isValid()"
      }
    },
    "midasHealthIndicator": {
      "status": "UP",
      "details": {
        "service": "Midas Core",
        "status": "Transaction processing active"
      }
    }
  }
}
```

✅ **If you see this**: System is running correctly!

---

### **STEP 7: Test Metrics Endpoint**

```bash
curl -X GET http://localhost:33400/actuator/metrics -H "X-API-Key: midas-dev-key-2026"
```

**Expected response includes:**
```json
{
  "names": [
    "midas.transactions.received",
    "midas.transactions.valid",
    "midas.transactions.rejected",
    ...more metrics...
  ]
}
```

✅ **If you see transaction metrics**: Observability is working!

---

### **STEP 8: Test Security (API Key Required)**

```bash
# WITHOUT API key (should fail)
curl -X GET http://localhost:33400/actuator/health

# Expected: 401 Unauthorized
```

```bash
# WITH API key (should work)
curl -X GET http://localhost:33400/actuator/health -H "X-API-Key: midas-dev-key-2026"

# Expected: 200 OK with health data
```

✅ **If this works**: Security is enforced!

---

### **STEP 9: View Application Logs**

In Terminal 1 (where app is running), you should see logs like:

```
[INFO] Spring embedded server started
[INFO] Tomcat initialized with port(s): 33400 (http)
[INFO] Initialization completed
[INFO] Application is ready for incoming requests
```

✅ **No ERROR messages**: System is stable!

---

### **STEP 10: Stop the Application**

**In Terminal 1 (where app is running):**

Press `Ctrl+C`

```
Shutting down gracefully
Closing all application connections
Shutdown complete
```

---

## 🎯 **Quick Test Commands Summary**

Save these commands for quick testing:

```bash
# Build & test
mvn clean install

# Run app
mvn spring-boot:run

# Health check (new terminal)
curl -H "X-API-Key: midas-dev-key-2026" http://localhost:33400/actuator/health

# Metrics
curl -H "X-API-Key: midas-dev-key-2026" http://localhost:33400/actuator/metrics

# Test security (should fail)
curl http://localhost:33400/actuator/health
```

---

## ✅ **Verification Checklist**

After completing all steps, check:

- [ ] Java and Maven are installed
- [ ] Project builds successfully
- [ ] All 80 tests pass
- [ ] Application starts on port 33400
- [ ] Health endpoint returns HTTP 200
- [ ] Metrics endpoint shows transaction counters
- [ ] API key is required (401 without it)
- [ ] No ERROR messages in logs
- [ ] Application stops cleanly with Ctrl+C

---

## 🐳 **Alternative: Docker Compose Setup**

If you want to test with PostgreSQL instead of H2:

```bash
# Terminal 1: Start services
docker-compose up

# Terminal 2: Test
curl -H "X-API-Key: midas-dev-key-2026" http://localhost:33400/actuator/health

# Stop
Press Ctrl+C in Terminal 1
```

---

## 🚨 **Troubleshooting**

| Problem | Solution |
|---------|----------|
| **"Port 33400 already in use"** | Kill process: `lsof -i :33400` then `kill -9 <PID>` |
| **"Java command not found"** | Install Java 17: https://adoptium.net |
| **"mvn command not found"** | Install Maven or add to PATH |
| **"BUILD FAILURE"** | Run `mvn clean` and retry |
| **"Tests failing"** | Run `mvn clean install -U` to update dependencies |
| **"Cannot connect to localhost:33400"** | Ensure app is running in Terminal 1 |
| **"401 Unauthorized"** | Add header: `-H "X-API-Key: midas-dev-key-2026"` |

---

## 📊 **Expected Results**

After successful local testing, you should see:

✅ **Build**: SUCCESS  
✅ **Tests**: 80/80 PASSING  
✅ **Startup**: < 10 seconds  
✅ **Health**: UP  
✅ **Metrics**: Available  
✅ **Security**: API key required  
✅ **Database**: Connected (H2)  
✅ **Logs**: No errors  

---

## 🎉 **What This Proves**

After completing these steps successfully, you've verified:

1. ✅ **Code Compiles** — No syntax errors
2. ✅ **Tests Pass** — All 80 tests working
3. ✅ **App Starts** — No startup issues
4. ✅ **API Works** — Endpoints responding
5. ✅ **Security** — Authentication enforced
6. ✅ **Database** — Persistence working
7. ✅ **Monitoring** — Metrics available
8. ✅ **Health** — System is stable

**System is ready for production deployment! ✅**

---

## 📝 **Record Your Results**

Create a file with your test results:

```bash
# Create results file
cat > LOCAL_TEST_RESULTS.txt << 'EOF'
LOCAL TESTING RESULTS
Date: [Your Date]
Tester: [Your Name]

Build Status: ✅ SUCCESS
Test Results: ✅ 80/80 PASS
Startup Time: X seconds
Health Check: ✅ UP
Metrics: ✅ Available
Security: ✅ Enforced
Database: ✅ Connected
Logs: ✅ No errors

OVERALL: ✅ READY FOR PRODUCTION
EOF

cat LOCAL_TEST_RESULTS.txt
```

---

## 🚀 **Next Steps**

After local testing is complete:

1. ✅ Document results
2. ✅ Notify team that system is verified
3. ✅ Plan production deployment
4. ✅ Execute deployment (See PRODUCTION_DEPLOYMENT_GUIDE.md)
5. ✅ Monitor production metrics

---

**You're ready to test locally right now!** 🏃‍♂️

Follow these steps on your machine to verify everything works before production.
