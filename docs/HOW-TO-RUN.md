# How to Run NexaBank Platform — Step by Step

There are **two ways** to run this application:

| Method | Best For | What You Need |
|---|---|---|
| **Option A — Docker Compose (Recommended)** | Running the full stack with one command | Docker Desktop |
| **Option B — Run Each Service Manually** | Local development, debugging a specific service | Java 17, Node.js 20, Python 3.11, Docker |

---

## Prerequisites — Install These First

### Required for Option A (Docker only)
- **Docker Desktop** — [https://www.docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop)
  - Mac: allocate at least **6GB RAM** in Docker Desktop → Settings → Resources
  - Verify: `docker --version` and `docker compose version`

### Required for Option B (local development)
- **Java 17** — `java -version` (must show 17.x)
- **Maven 3.9+** — `mvn -version`
- **Node.js 20+** — `node --version`
- **npm** — `npm --version`
- **Python 3.11+** — `python3 --version`
- **Docker Desktop** — still needed for Postgres, Kafka, ActiveMQ

---

## Option A — Full Stack with Docker Compose

This starts every service automatically in the correct order.

### Step 1 — Navigate to the project root

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform
```

### Step 2 — Build all Java services

```bash
mvn package -DskipTests
```

This compiles and packages all 7 Spring Boot services into `.jar` files.
Expected output: `BUILD SUCCESS` for each module. Takes ~2-3 minutes.

### Step 3 — Start everything

```bash
docker-compose up -d
```

Docker will:
1. Pull images (Postgres, Kafka, ActiveMQ, etc.) on first run — takes a few minutes
2. Build the Java and Angular Docker images
3. Start all containers in dependency order

### Step 4 — Wait for services to become healthy

```bash
docker-compose ps
```

Wait until all containers show `healthy` or `running`. This takes about **60–90 seconds**.

You can watch it in real time:
```bash
docker-compose ps --watch
```

Or check logs for a specific service:
```bash
docker-compose logs -f account-service
docker-compose logs -f api-gateway
```

### Step 5 — Verify everything is up

Open these in your browser:

| URL | What You Should See |
|---|---|
| http://localhost:8761 | Eureka dashboard — 4 services registered |
| http://localhost:4200 | NexaBank login page |
| http://localhost:9090 | Kafka UI — topic browser |
| http://localhost:8161 | ActiveMQ admin (login: admin / admin) |

### Step 6 — Create an account and log in

Open http://localhost:4200 and click **Register**. Fill in your name, email, and password.

Or use curl:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@nexabank.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

You'll receive a JWT token in the response.

### Step 7 — Stop everything

```bash
docker-compose down
```

To also remove the database volume (resets all data):
```bash
docker-compose down -v
```

---

## Option B — Run Each Service Manually

Use this when you want to run and debug individual services in your IDE. You still use Docker for the infrastructure (Postgres, Kafka, ActiveMQ).

### Step 1 — Start infrastructure containers only

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform

docker-compose up -d postgres zookeeper kafka activemq kafka-ui
```

Wait ~20 seconds for Kafka to become ready:
```bash
docker-compose ps
# kafka should show "healthy"
```

### Step 2 — Build the Java codebase

```bash
mvn package -DskipTests
```

### Step 3 — Start Eureka Server

```bash
cd infrastructure/eureka-server
mvn spring-boot:run
```

Wait until you see:
```
Started EurekaServerApplication in X.XXX seconds
```

Keep this terminal open. Open a **new terminal** for each following step.

### Step 4 — Start Config Server

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/infrastructure/config-server
mvn spring-boot:run
```

Wait until you see `Started ConfigServerApplication`.

### Step 5 — Create the service databases

The first time only — run this to create the 4 databases:
```bash
docker exec -i nexabank-postgres psql -U nexabank -d nexabank < docs/db-init/01-create-databases.sql
```

### Step 6 — Start API Gateway

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/infrastructure/api-gateway
mvn spring-boot:run
```

Wait until you see `Started ApiGatewayApplication`.

### Step 7 — Start account-service

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/services/account-service
mvn spring-boot:run
```

### Step 8 — Start transaction-service

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/services/transaction-service
mvn spring-boot:run
```

### Step 9 — Start notification-service

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/services/notification-service
mvn spring-boot:run
```

### Step 10 — Start loan-service

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/services/loan-service
mvn spring-boot:run
```

### Step 11 — Start the Python AI layer

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/ai-layer

# Create a virtual environment (first time only)
python3 -m venv venv
source venv/bin/activate

# Install dependencies (first time only)
pip install -r requirements.txt

# Start the server
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

### Step 12 — Start the Angular frontend

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform/frontend/nexabank-ui

# Install dependencies (first time only)
npm install

# Start the dev server
npm start
```

The Angular dev server runs on http://localhost:4200 and proxies `/api` calls to the API Gateway at `localhost:8080`.

---

## Running Tests

### Unit tests for all Java services

```bash
cd /Users/anthonyjoyner/Documents/Projects/nexabank-platform
mvn test
```

### Unit tests for a single service

```bash
cd services/account-service
mvn test
```

### Angular tests

```bash
cd frontend/nexabank-ui
npm test
```

---

## Verify the Full Flow Works

Do these checks in order after starting:

```
1. http://localhost:8761 → Eureka shows: ACCOUNT-SERVICE, TRANSACTION-SERVICE,
                           NOTIFICATION-SERVICE, LOAN-SERVICE all registered

2. POST http://localhost:8080/api/auth/register
   → Returns { "token": "eyJ..." }

3. POST http://localhost:8080/api/transactions/transfer (with Bearer token)
   → Returns transaction response with referenceNumber

4. http://localhost:9090 → Kafka UI → Topic "nexabank.transaction.completed"
   → Message visible

5. http://localhost:8161 → ActiveMQ → Queue "nexabank.transaction.events"
   → Message visible (may have been consumed already)

6. GET http://localhost:8080/api/notifications/{customerId} (with Bearer token)
   → Returns notification created by the Kafka consumer

7. POST http://localhost:8080/api/loans/apply (with Bearer token)
   → Returns loan with aiDecision (APPROVED/REJECTED/AI_REVIEW) and aiReason

8. http://localhost:4200 → Full login → dashboard → transfer → notifications flow
```

---

## Swagger API Documentation

Every Java service has an interactive API explorer:

| Service | Swagger UI |
|---|---|
| Account Service | http://localhost:8081/swagger-ui.html |
| Transaction Service | http://localhost:8082/swagger-ui.html |
| Notification Service | http://localhost:8083/swagger-ui.html |
| Loan Service | http://localhost:8084/swagger-ui.html |
| AI Layer | http://localhost:8000/docs |

---

## Common Problems and Fixes

### "Port already in use"
```bash
# Find what's using the port (e.g., 8080)
lsof -i :8080
# Kill it
kill -9 <PID>
```

### Services not appearing in Eureka
The service hasn't finished starting up yet. Wait 30 seconds and refresh http://localhost:8761. Services register asynchronously.

### Kafka errors on startup
Kafka takes longer than other services. If a Spring service fails to connect to Kafka on startup, restart it after Kafka is healthy:
```bash
docker-compose restart account-service
```

### Database tables don't exist (Flyway error)
Flyway runs migrations automatically on service startup. If you see `relation does not exist`, the database wasn't created. Run:
```bash
docker exec -i nexabank-postgres psql -U nexabank -d nexabank < docs/db-init/01-create-databases.sql
```
Then restart the failing service.

### Angular "Cannot GET /api/..."
Make sure the API Gateway is running on port 8080. The Angular proxy config in `proxy.conf.json` forwards `/api` to `http://localhost:8080`.

### Docker out of memory
Open Docker Desktop → Settings → Resources → increase Memory to at least 6GB.

### Start fresh (wipe all data)
```bash
docker-compose down -v
docker-compose up -d
```

---

## Service Port Quick Reference

| Service | Port | URL |
|---|---|---|
| Angular UI | 4200 | http://localhost:4200 |
| API Gateway | 8080 | http://localhost:8080 |
| Account Service | 8081 | http://localhost:8081 |
| Transaction Service | 8082 | http://localhost:8082 |
| Notification Service | 8083 | http://localhost:8083 |
| Loan Service | 8084 | http://localhost:8084 |
| Python AI Layer | 8000 | http://localhost:8000 |
| Eureka Dashboard | 8761 | http://localhost:8761 |
| Config Server | 8888 | http://localhost:8888 |
| Kafka UI | 9090 | http://localhost:9090 |
| ActiveMQ Admin | 8161 | http://localhost:8161 |
| PostgreSQL | 5432 | localhost:5432 (user: nexabank / nexabank123) |
| Kafka Broker | 9092 | localhost:9092 |
| ActiveMQ | 61616 | localhost:61616 |
