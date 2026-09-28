# Database & Caching Schema

## PostgreSQL (The Permanent Record)

### 1. `users` Table
*   `id` (UUID): Primary Key
*   `email` (String): Unique login
*   `password` (String): BCrypt hashed
*   `role` (String): e.g., `ROLE_COMPANY_ADMIN`, `ROLE_EMPLOYEE`, `ROLE_PLATFORM_COMPLIANCE`
*   `companyName` (String): Used for B2B Row-Level Security (Tenant Isolation)

### 2. `companies` Table
*   `id` (UUID): Primary Key
*   `companyName` (String): Unique Name
*   `einNumber` (String): Tax ID
*   `kybStatus` (String): `PENDING`, `APPROVED`, `FROZEN_FOR_AML_INVESTIGATION`

### 3. `master_wallets` Table
*   `id` (UUID): Primary Key
*   `companyName` (String): The Tenant
*   `totalBalance` (BigDecimal): The massive pool of real-world money wired by the client.

### 4. `wallets` Table
*   `id` (UUID): Primary Key
*   `employee_id` (FK): Owner of the wallet
*   `balance` (BigDecimal): The specific budget allocated to this employee

### 5. `virtual_cards` Table
*   `id` (UUID): Primary Key
*   `wallet_id` (FK): The wallet this card draws money from
*   `cardNumber` (String): 16-digit PAN
*   `status` (String): `ACTIVE`, `FROZEN`, `CLOSED`
*   `cardType` (String): `STANDARD` or `BURNER`

### 6. `transaction_ledger` Table
*   `id` (UUID): Primary Key
*   `cardNumber` (String)
*   `amount` (BigDecimal)
*   `merchantName` (String)
*   `receiptUploaded` (Boolean)
*   `receiptUrl` (String): S3 Object URL

---

## Redis (The Auth Engine's O(1) Data Grid)
To hit sub-200ms authorization speeds, the Auth Engine relies entirely on Redis Key-Value pairs.

### Core Mappings
*   **Key**: `card:{cardNumber}:wallet` | **Value**: `{walletId}` (UUID)
    *   *Purpose*: Tells the Auth Engine which wallet to deduct funds from.
*   **Key**: `card:{cardNumber}:status` | **Value**: `FROZEN`
    *   *Purpose*: If this key exists, the card is instantly declined.
*   **Key**: `card:{cardNumber}:type` | **Value**: `BURNER` or `STANDARD`
    *   *Purpose*: Determines if the card should be permanently destroyed after one swipe.
*   **Key**: `wallet:{walletId}:balance` | **Value**: `150.00`
    *   *Purpose*: The current spendable balance. Evaluated using Redis `DECRBYFLOAT` for atomic transactions.

### Fraud Mappings (TTL Based)
*   **Key**: `velocity:{cardNumber}:{YYYY-MM-DD}` | **Value**: `14` (Integer count)
    *   *Purpose*: Tracks how many times a card was swiped today. Expires at midnight.
*   **Key**: `otp:{cardNumber}` | **Value**: `123456`
    *   *Purpose*: RBI Mandate 6-digit code. Expires in 5 minutes (`TTL`).
