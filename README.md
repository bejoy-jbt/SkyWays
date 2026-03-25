# ✈️ FlightApp — Production-Grade Microservices Flight Booking System

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────┐
│                        React Frontend :3000                         │
└───────────────────────────────┬─────────────────────────────────────┘
                                │ HTTP
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    API Gateway :8080                                 │
│          JWT validation · Role-based routing · CORS                 │
└───┬──────────────┬───────────────┬──────────────┬───────────────────┘
    │              │               │              │
    ▼              ▼               ▼              ▼
Auth :8081   Flight :8082   Booking :8083   Payment :8084
PostgreSQL   PostgreSQL      PostgreSQL      PostgreSQL
                                  │               │
                                  └───────┬───────┘
                                          │ Kafka Events (KRaft)
                                  ┌───────┴───────┐
                                  │  Kafka :9092  │
                                  │  5 topics     │
                                  └───────┬───────┘
                                          │
                                Notification :8085
                               JavaMailSender (Gmail)
```

## Saga Choreography Flow

```
POST /api/bookings
  │
  ├─ 1. Create Booking record (PENDING)
  ├─ 2. Lock seat via flight-service REST (optimistic @Version lock)
  ├─ 3. Publish  →  [payment.requested]
  │
  │                 PaymentService consumes payment.requested
  │                 ├─ Create Stripe PaymentIntent (test mode)
  │                 ├─ Charge card pm_card_visa (auto-confirm)
  │                 ├─ SUCCESS → Publish [payment.success]
  │                 └─ FAILURE → Publish [payment.failed]
  │
  ├─ BookingService consumes payment.success
  │   ├─ Status → CONFIRMED
  │   ├─ Confirm seat permanently in flight-service
  │   └─ Publish [booking.confirmed]
  │
  └─ BookingService consumes payment.failed
      ├─ Status → PAYMENT_FAILED
      ├─ Release seat (compensating transaction)
      └─ Publish [booking.failed]

NotificationService consumes booking.confirmed / booking.failed
  └─ Sends HTML email via Gmail SMTP
```

## Quick Start (Docker Compose)

```bash
# 1. Clone or extract the project
cd flight-system

# 2. Configure environment
cp .env.example .env
# Edit .env:
#   JWT_SECRET    — any 32+ char random string
#   MAIL_USER     — your Gmail address
#   MAIL_PASS     — Gmail App Password (not your login password)
#   STRIPE_SECRET_KEY — from https://dashboard.stripe.com/test/apikeys

# 3. Start all services (first run takes ~5 min to build)
make up
# or: docker compose up --build

# 4. Create admin user
make admin-user

# 5. Open the app
open http://localhost:3000
```

## URLs
| URL                          | Description                        |
|------------------------------|------------------------------------|
| http://localhost:3000        | Frontend (React)                   |
| http://localhost:8080        | API Gateway                        |
| http://localhost:9000        | Kafka UI (browse topics/messages)  |
| http://localhost:9411        | Zipkin (distributed tracing)       |
| http://localhost:8081/actuator/health | Auth health check        |
| http://localhost:8082/actuator/health | Flight health check      |

## Default Credentials
| Email                  | Password   | Role  |
|------------------------|------------|-------|
| admin@flightapp.com    | Admin@123  | ADMIN |
| (register your own)    | (any 8+)   | USER  |

## Gmail SMTP Setup
1. Enable 2-Step Verification on your Google account
2. Go to: https://myaccount.google.com/apppasswords
3. Create an App Password → select "Mail" → "Other" → name it "FlightApp"
4. Copy the 16-character password into `MAIL_PASS` in `.env`

## Stripe Test Mode
| Card Number         | Result             |
|---------------------|--------------------|
| 4242 4242 4242 4242 | ✅ Payment success |
| 4000 0000 0000 0002 | ❌ Card declined   |
| 4000 0000 0000 9995 | ❌ Insufficient funds |

The system uses `pm_card_visa` test payment method for server-side auto-confirm.
For production: integrate Stripe Elements in frontend for real card collection.

## Useful Commands
```bash
make logs          # Tail all service logs
make log-booking   # Tail booking-service only
make log-payment   # Tail payment-service only
make ps            # Show container status
make down          # Stop everything
make clean         # Stop + delete all volumes (fresh start)
make shell-auth-db # PostgreSQL shell for auth DB
```

## Kubernetes Deployment
```bash
# 1. Build and push images
docker build -t your-registry/flightapp/api-gateway:latest ./api-gateway
docker build -t your-registry/flightapp/auth-service:latest ./auth-service
docker build -t your-registry/flightapp/flight-service:latest ./flight-service
docker build -t your-registry/flightapp/booking-service:latest ./booking-service
docker build -t your-registry/flightapp/payment-service:latest ./payment-service
docker build -t your-registry/flightapp/notification-service:latest ./notification-service
docker build -t your-registry/flightapp/frontend:latest ./frontend

# 2. Update image names in k8s/services.yaml

# 3. Edit secrets
nano k8s/secrets.yaml  # Fill in real values

# 4. Apply manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/kafka.yaml
kubectl apply -f k8s/services.yaml
kubectl apply -f k8s/hpa.yaml
kubectl apply -f k8s/ingress.yaml

# 5. Check pods
kubectl get pods -n flightapp
```

## Security Features
- **JWT (HS256)** — issued by auth-service, verified at gateway level
- **BCrypt** — all passwords hashed with bcrypt strength 10
- **Role-based** — ADMIN routes blocked at gateway for non-admin tokens
- **Optimistic locking** — `@Version` on Seat entity prevents double-booking
- **HTTPS-ready** — configure TLS at ingress/load balancer level
- **Sensitive data** — Stripe keys and mail credentials via env vars / K8s Secrets

## Non-Functional Features
| Feature           | Implementation                                          |
|-------------------|---------------------------------------------------------|
| Scalability       | Stateless services + HPA (2–10 replicas)                |
| Low latency       | Redis at gateway for rate-limiting / token cache        |
| Fault tolerance   | `restart: on-failure`, Kafka durability, sagas compensate |
| Observability     | Structured JSON logs, Zipkin tracing, Actuator health   |
| Self-healing      | K8s liveness + readiness probes                        |
| Durability        | Kafka `earliest` offset + PostgreSQL with WAL           |
| Seat safety       | Optimistic lock → conflict → immediate seat release     |
