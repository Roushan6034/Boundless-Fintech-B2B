# Internal Operations & Proactive Alerting

Boundless FinTech shifts Internal Operations from a reactive state (staring at dashboards) to a highly proactive state using an automated alerting matrix over Kafka.

## The Routing Matrix
Instead of hardcoding error responses, the Auth Engine acts as a Universal Router. When *any* transaction fails for *any* reason, it evaluates the error string and routes a highly structured JSON alert to the `internal-alert-topic`.

### 1. High-Risk Security Class
*   **Triggers**: `BRUTE_FORCE`, `BLOCKED_MCC` (Casinos), `VELOCITY_EXCEEDED`
*   **Routing**: The Notification Service sends an urgent SMTP email to `compliance@boundless.com`.
*   **Outcome**: The Risk Officer is notified of the exact card and merchant, allowing them to instantly hit the `/freeze` endpoint to halt the AML threat.

### 2. User Friction Class
*   **Triggers**: `INSUFFICIENT_FUNDS`, `FROZEN`, `INACTIVE`
*   **Routing**: The Notification Service sends an email to `support@boundless.com`.
*   **Outcome**: Customer Success knows that an employee at Starbucks was just declined for having an empty wallet. The support rep can pull up their account *before* the angry employee even dials the 1-800 number.

### 3. Corporate Liquidity Class (In Identity Service)
*   **Triggers**: A Company Admin tries to allocate a budget to an employee, but their Master Wallet is empty.
*   **Routing**: The Identity Service drops a Kafka event targeting `treasury@boundless.com`.
*   **Outcome**: The Treasury Manager receives an email stating that Acme Corp is out of funds. The Treasury Manager can proactively email the founder of Acme Corp requesting a new wire transfer, maintaining cash flow.
