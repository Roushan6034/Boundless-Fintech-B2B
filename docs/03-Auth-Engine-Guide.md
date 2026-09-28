# The Auth Engine (Fraud & Authorization)

The `auth-engine-service` is the most critical component. It handles the `POST /api/network/swipe` webhook sent directly from Visa or Mastercard. It must respond with 200 OK or 400 Bad Request in under 200ms.

## 1. The Chain of Responsibility
When a swipe arrives, it passes through a gauntlet of Java rules (The Fraud Engine). If any rule fails, it throws a `RuntimeException` and the card is instantly declined.

### Rule 1: `VelocityRule`
*   **Logic**: Increments a Redis key `velocity:{pan}:{date}`. If the value exceeds 10 swipes in a single day, the transaction is rejected.
*   **Why**: Prevents hackers from rapidly testing stolen card numbers or draining small amounts.

### Rule 2: `MccRule`
*   **Logic**: Checks the `merchantName` against a hardcoded blocked list (e.g., "Las Vegas Casino", "Local Bar").
*   **Why**: Corporate funds cannot be used for gambling or alcohol.

### Rule 3: `BruteForceRule`
*   **Logic**: If the CVV is wrong, it increments `cvv_failures:{pan}` in Redis. If it hits 3 failures, the card is locked for 24 hours.
*   **Why**: Stops brute-force scripts from guessing the CVV.

### Rule 4: `BalanceRule`
*   **Logic**: Atomically deducts the requested amount from the Redis key `wallet:{walletId}:balance`.
*   **Why**: Ensures the employee actually has enough budget allocated by their founder.

## 2. RBI Mandate (OTP Enforcement)
If the transaction amount exceeds ₹5,000, the Auth Engine halts the transaction and returns a `202 PENDING_OTP` response. 
*   It generates a 6-digit OTP, saves it to Redis with a 5-minute TTL, and drops a Kafka event to send an email to the employee. 
*   The transaction will only clear if the employee subsequently verifies the code.

## 3. Single-Use Burner Cards
If the Redis key `card:{pan}:type` equals `BURNER`, the Auth Engine triggers a destruction sequence immediately after approval:
1. Deletes `card:{pan}:wallet` from Redis.
2. Deletes `card:{pan}:status` from Redis.
3. Deletes `card:{pan}:type` from Redis.
4. Publishes a `card-closed-topic` event to Kafka so the Identity service updates Postgres to `CLOSED`.

## 4. Universal Proactive Alerting
If any transaction fails (for any reason), the Auth Engine catches the exception, classifies the error, and publishes to the `internal-alert-topic`:
*   **High Risk (Brute Force, MCC, Velocity)** ➔ Routes to `ROLE_PLATFORM_COMPLIANCE`
*   **Friction (Insufficient Funds, Inactive)** ➔ Routes to `ROLE_PLATFORM_SUPPORT`
