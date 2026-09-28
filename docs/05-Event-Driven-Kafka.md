# Event-Driven Architecture & Kafka Dictionary

To ensure microservices remain decoupled and highly resilient, Boundless relies heavily on Apache Kafka topics for inter-service communication.

## Event Dictionary

### 1. `auth-approved-topic`
*   **Producer**: Auth Engine (After a successful swipe)
*   **Consumer**: Ledger Service
*   **Action**: Saves the permanent transaction record to PostgreSQL.

### 2. `employee-onboarding-topic`
*   **Producer**: Identity Service (When a founder adds an employee)
*   **Consumer**: Notification Service
*   **Action**: Sends the welcome email with the temporary password and Employee ID.

### 3. `otp-email-topic`
*   **Producer**: Auth Engine (When RBI limits are exceeded)
*   **Consumer**: Notification Service
*   **Action**: Dispatches the 6-digit OTP to the employee's email.

### 4. `receipt-reminder-topic`
*   **Producer**: Ledger Service (When a tx > $50 clears)
*   **Consumer**: Notification Service
*   **Action**: Dispatches the 24-hour SLA warning email. (Uses a synchronous Service-to-Service call to Identity to fetch the target email).

### 5. `card-freeze-topic`
*   **Producer**: Ledger Service (Cron Job detects a missed SLA)
*   **Consumer**: Identity Service (`CardFreezeListener`)
*   **Action**: Sets `status = FROZEN` in Postgres and Redis.

### 6. `card-unfreeze-topic`
*   **Producer**: Ledger Service (Employee uploads receipt)
*   **Consumer**: Identity Service (`CardFreezeListener`)
*   **Action**: Sets `status = ACTIVE` in Postgres and deletes the `FROZEN` lock in Redis.

### 7. `card-closed-topic`
*   **Producer**: Auth Engine (Immediately after a BURNER card is successfully swiped)
*   **Consumer**: Identity Service (`CardFreezeListener`)
*   **Action**: Sets `status = CLOSED` in PostgreSQL permanently.

### 8. `internal-alert-topic`
*   **Producer**: Auth Engine & Identity Service (Any rule violation or fund shortage)
*   **Consumer**: Notification Service (`EmailListener`)
*   **Action**: Routes critical operational alerts to the Treasury, Support, or Compliance teams based on the JSON payload.
