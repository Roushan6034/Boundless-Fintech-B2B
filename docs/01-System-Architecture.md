# Boundless Corporate FinTech: System Architecture

## Overview
Boundless is a B2B corporate card and spend management platform. It allows client companies (like Acme Corp) to onboard, fund a Master Wallet, and issue physical or virtual corporate cards (Standard or Single-Use Burner) to their employees. 

To achieve massive scale and sub-200ms transaction authorization (required by Visa/Mastercard), the system is heavily distributed across 5 Spring Boot Microservices and utilizes event-driven architecture (Apache Kafka) and in-memory data grids (Redis).

## Infrastructure Stack
1. **PostgreSQL**: The permanent source of truth for Users, Companies, Master Wallets, and Ledger Transactions.
2. **Redis**: The ultra-fast caching layer used exclusively by the Auth Engine to evaluate swipes in O(1) time without hitting a disk.
3. **Apache Kafka**: The asynchronous nervous system connecting the Microservices.
4. **MinIO (S3-Compatible)**: Object storage for physical receipt image uploads.
5. **Zipkin**: Distributed tracing for observability across microservice boundaries.

## Microservices Layout

### 1. API Gateway (`api-gateway-service` | Port 8080)
Acts as the single entry point for all frontend requests. It handles initial routing and could be expanded to handle rate-limiting. (JWT Validation happens at the Identity layer in this implementation).

### 2. Card Identity Service (`card-identity-service` | Port 8081)
The administrative brain of the platform.
*   **Responsibilities**: KYB Onboarding, User Authentication (JWT), Master Wallet funding, Virtual Card Issuance (generating PAN/CVV).
*   **Data Flow**: When a card is issued, this service pushes the `card:wallet` mapping and the `wallet:balance` directly into Redis.

### 3. Auth Engine (`auth-engine-service` | Port 8082)
The critical, high-availability service that responds to the Visa/Mastercard webhook (`POST /api/network/swipe`).
*   **Responsibilities**: Must return a 200 OK or 400 Bad Request in under 200ms. Evaluates complex fraud rules (Velocity, MCC limits, Brute Force) and deducts wallet balances.
*   **Data Flow**: Reads *exclusively* from Redis. Never touches PostgreSQL. After approval, it drops an `auth-approved-topic` event into Kafka and returns 200 OK to Visa.

### 4. Ledger Service (`ledger-service` | Port 8083)
The permanent financial record.
*   **Responsibilities**: Consumes the Kafka approval events and saves them to PostgreSQL for auditing. Enforces the 24-hour receipt upload SLA.
*   **Data Flow**: Runs a Cron Job to freeze cards missing receipts, and exposes endpoints to upload images to MinIO (S3) to unfreeze them.

### 5. Notification Service (`notification-service` | Port 8084)
The communications dispatcher.
*   **Responsibilities**: Listens to various Kafka topics and sends SMTP emails.
*   **Data Flow**: Handles Welcome Emails, RBI Mandate OTPs, Receipt Reminders, and Proactive Alerts to Internal Ops teams.
