# Identity & Multi-Tenancy

The `card-identity-service` acts as the administrative control plane. It handles Authentication, B2B Multi-Tenancy, and Internal Operations.

## 1. Authentication & Security
The service uses Spring Security with JWT (JSON Web Tokens). 
*   Clients hit `POST /api/auth/login` to receive a Bearer Token.
*   The `JwtFilter` intercepts all subsequent requests, validates the cryptography, and injects the user's `ROLE_` into the Spring Security Context, enforcing `@PreAuthorize` restrictions.

## 2. B2B Multi-Tenancy (Row-Level Security)
The platform is designed to host multiple companies (e.g., Acme Corp, Globex) on the same database securely.
*   **KYB Flow**: Companies register publicly via `CompanyRegistrationController`. If approved, the system auto-provisions a `MasterWallet` and a Founder user with `ROLE_COMPANY_ADMIN`.
*   **Data Isolation**: In the `CompanyAdminController`, every single endpoint strictly verifies that the target employee or wallet has the exact same `companyName` as the requesting Admin. An Admin at Acme Corp cannot issue a card to an employee at Globex.

## 3. The Internal Ops Triad
To prevent a single "God Mode" super admin from causing catastrophic damage, platform operations are fractured into three granular roles inside the `InternalOpsController`.

### A. The Treasury Manager (`ROLE_PLATFORM_TREASURY`)
*   **Job**: Manages the massive real-world fiat currency that companies wire to the platform.
*   **Access**: Has the exclusive ability to hit `POST /api/internal-ops/master-wallet/fund` to mint digital currency into a client's Master Wallet.

### B. Customer Success (`ROLE_PLATFORM_SUPPORT`)
*   **Job**: Troubleshoots angry employee phone calls.
*   **Access**: Has read-only access via `GET /api/internal-ops/companies/{name}/overview` to see if a company ran out of money, explaining why a card was declined at Starbucks.

### C. Risk & Compliance (`ROLE_PLATFORM_COMPLIANCE`)
*   **Job**: Anti-Money Laundering (AML) enforcement.
*   **Access**: Has the exclusive ability to hit `POST /api/internal-ops/companies/{name}/freeze` to instantly lock down a company and trigger a full forensic audit if massive fraud is detected.
