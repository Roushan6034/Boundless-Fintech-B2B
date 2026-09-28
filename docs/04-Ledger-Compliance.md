# The Ledger & SLA Compliance

The `ledger-service` is the permanent financial record of the platform. It is decoupled from the fast-path (Auth Engine) via Apache Kafka.

## 1. Asynchronous Settlement
When a transaction is approved in the Auth Engine, it publishes an `auth-approved-topic` event to Kafka. The Ledger Service consumes this event at its own pace and saves the transaction into the PostgreSQL `transaction_ledger` table.

## 2. The 24-Hour Receipt SLA
Corporate expense policies require receipts for large purchases.
*   **The Rule**: Any transaction over $50.00 requires a photo receipt uploaded within 24 hours.
*   **The Email**: The moment the transaction clears, the Ledger Service publishes a `receipt-reminder-topic` event, which emails the employee warning them of the deadline.

## 3. The Enforcer (Cron Job)
The Ledger Service runs a `@Scheduled` Cron Job every 5 minutes.
*   It scans PostgreSQL for any transaction where `amount > 50` AND `receiptUploaded == false` AND `receiptDeadline < NOW()`.
*   If found, it publishes a `card-freeze-topic` event to Kafka.
*   The Identity Service hears this and immediately pushes `card:{pan}:status = FROZEN` into Redis, instantly blocking the employee from buying anything else.

## 4. S3 Object Storage Uploads
To unfreeze their card, the employee must upload a receipt image via `POST /api/transactions/{id}/receipt`.
*   The Ledger Service streams the `MultipartFile` directly into a local **MinIO S3-Compatible** bucket (`boundless-receipts`).
*   It saves the resulting S3 URL into the Postgres `receiptUrl` column.
*   It sets `receiptUploaded = true`.
*   Finally, it publishes a `card-unfreeze-topic` event to Kafka, which tells the Identity Service to delete the `FROZEN` lock in Redis, instantly reactivating the card!
