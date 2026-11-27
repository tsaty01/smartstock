# SmartStock — Project Goals and Implementation Blueprint

> **Project:** SmartStock — Enterprise Inventory & Billing Management System  
> **Course:** Enterprise Web Application — UCS553 (2026–27 Odd Semester)  
> **Institution:** Thapar Institute of Engineering & Technology (TIET), Patiala  
> **Instructor:** Dr. Kanupriya  
> **Status:** Agreed technical direction; implementation pending  
> **Document updated:** 18 November 2026

## 1. Vision

Build **SmartStock**, a new, standalone inventory and billing system: a native Android app backed by **Java Spring Boot + PostgreSQL**. The server provides a REST API, not a website.

**One-sentence success criterion:** A user can perform an authorized inventory or billing operation in the Android app, refresh on another device, and see the correct persisted result, with secure access and correct transactions.

An earlier Node.js/Express + MySQL inventory app is used only as a feature reference (see `docs/reference-audit.md`); the product, code and branding are independent of it.

### Project team

| Member | Roll number |
| --- | --- |
| Ishant Mehndiratta | 1024030525 |
| Satyam Tiwari | 1024030088 |
| Anshaj | 1024030494 |
| Aayush Bindal | 1024030498 |

SmartStock is an independent **UCS553** project under **Dr. Kanupriya**.

## 2. Product goals (in priority order)

1. **Android-first:** Deliver a polished, genuinely native Android app using Kotlin, Jetpack Compose, and Material 3 Expressive. It must be the primary live demonstration.
2. **Java-first core:** Implement the HTTP API, security, business rules, transactions, persistence, and web server in **Java 21 + Spring Boot**. Use Kotlin primarily where Compose and Android-specific UI state benefit from it.
3. **Android-only frontend:** Keep all user workflows in Compose; the backend serves JSON APIs only.
4. **Single source of truth:** Android devices use one authenticated backend and one PostgreSQL database. Never duplicate inventory or billing logic in the app.
5. **Core scope first:** Roles, inventory management, checkout, discounts, alerts, order history, and analytics before enhancements.
6. **Reliable demonstration and deployment:** Run locally with Docker Compose and, when practical, deploy to the existing homelab. Maintain a laptop/local-network fallback that does not require public internet connectivity.
7. **Cohesive design:** Use **Inter Variable**, **Source Serif 4 Variable**, Material 3 Expressive principles, and semantic light/dark color tokens in Android.

## 3. Final architecture

```mermaid
flowchart TD
    A["Android app<br/>Kotlin + Jetpack Compose + M3 Expressive"]
    API["Java 21 Spring Boot<br/>REST Controllers"]
    S["Java service layer<br/>Auth · Inventory · Billing · Reports"]
    P["Spring Data JPA / JDBC<br/>Flyway migrations"]
    DB[(PostgreSQL)]
    F[("Product images / exports<br/>Persistent file volume")]

    A -->|HTTPS JSON API| API
    API --> S
    S --> P
    P --> DB
    S --> F
```

### Guiding architectural decisions

- **Modular monolith**, not microservices. Prefer clearly separated domain packages within a single Spring Boot deployment.
- **One PostgreSQL database** for all business data. Android devices never connect to PostgreSQL directly.
- **One shared Java service layer** for checkout, stock changes, discounts, reports, authorization-sensitive operations, and validation.
- **One presentation surface:** the native Compose Android app. Spring Boot serves only the API.
- **No Appwrite dependency.** Existing Appwrite can coexist on the homelab but is not required for authentication, database access, storage, or startup.
- **No Kotlin Multiplatform.** Cross-platform reuse is achieved via HTTP APIs and shared design semantics, not shared UI binaries.
- **No Node.js runtime** in the application.

## 4. Technology stack

| Area | Selected approach | Notes |
| --- | --- | --- |
| Backend language | **Java 21** | Main UCS553 language |
| Backend framework | **Spring Boot** | REST API only |
| Android UI | **Kotlin + Jetpack Compose** | Prefer M3 Expressive components |
| Android architecture | **ViewModel + unidirectional UI state** | Kotlin for Compose/state; Java for reusable Android data/domain code where pragmatic |
| Android networking | **Retrofit + OkHttp** | HTTPS API access; safe credential handling |
| Database | **PostgreSQL** | Standalone instance; not Appwrite's internal DB |
| Persistence | **Spring Data JPA + Spring JDBC** | JPA for typical entities, JDBC for SQL-centric cases |
| Schema migrations | **Flyway** | Versioned, reproducible SQL |
| Authentication and authorization | **Spring Security** | Roles enforced on the server |
| Build | **Gradle** | Separate Android and backend builds within one repo |
| Backend tests | **JUnit 5 + Mockito + integration tests** | Integration tests run against a real embedded PostgreSQL, no Docker needed |
| Deployment | **Docker Compose** | Spring Boot + PostgreSQL + persistent storage |
| Typography | **Inter Variable + Source Serif 4 Variable** | Bundled in Android |
| Theme | **Material 3 Expressive** | Native light and dark themes |

Use currently supported, compatible stable versions when implementation begins; avoid locking library versions in this goal document before dependency verification.

## 5. Functional scope

### Roles and authorization

Four roles: **Admin/Owner, Manager, Cashier, Customer**.

| Capability | Admin | Manager | Cashier | Customer |
| --- | :---: | :---: | :---: | :---: |
| Operational dashboard and analytics | Yes | Yes | No | No |
| Create/edit products and categories | Yes | Yes | No | No |
| Restock and adjust inventory | Yes | Yes | No | No |
| Checkout / point of sale | Yes | Yes | Yes | No |
| Operational order list | Yes | Yes | Yes | No |
| Manage users and roles | Yes | No | No | No |
| Inventory alerts | Yes | Yes | No | No |
| Own purchase history and receipts | As relevant | As relevant | As relevant | Yes |

**Security note:** Apply authorization to every backend endpoint and object, not merely by hiding buttons. Customers must never be able to read other customers' orders.

### Must-have modules — core scope

- **Authentication:** Sign in, sign out, customer registration, current-user profile, role-aware navigation, disabled-account handling.
- **Product catalog:** Categories, product search/filter, barcode field, pricing, status, reorder levels, expiry dates, warnings, and product images.
- **Inventory management:** Create/edit products, restock, stock adjustment, stock movement history, low-stock and expiry alerts.
- **Billing/POS:** Select a customer, add products/quantities, calculate subtotal, apply permitted discounts, calculate tax, select payment mode, finalize a sale, create a receipt.
- **Orders:** View order details and history according to role; store item prices at transaction time.
- **Analytics:** Daily sales, revenue, product/category performance, high-selling items, low-stock and expiry summaries.
- **Customer experience:** View own purchases/receipts and any appropriate catalog information.

### Enhancements — after core scope

- Barcode scanning with the Android camera (prefer a well-supported scanner API).
- CSV export and printable/downloadable receipts (PDF if feasible).
- Improved product images and reusable upload components.
- Refresh-on-change or lightweight real-time update where it meaningfully improves inventory coordination.
- Optional Android low-stock notifications and polished dashboard charts.

Treat these as **stretch goals**, not prerequisites for a passing core demonstration. Do not introduce a remote push-notification dependency just to implement basic in-app alerts.

## 6. Data and business rules

Scope reference: `docs/reference-audit.md` (domain model, behaviors worth matching, defects to avoid).

1. Model categories, users, products, orders, order items, stock movements, discount rules, and notifications.
2. Define the schema as versioned Flyway migrations with referential integrity, check constraints, and useful query indexes.
3. Use identity keys, `BOOLEAN`, constrained text for enumerations, and `timestamptz` for instants.
4. Decide ownership of each business rule **once**: service methods for workflow/transaction logic; database constraints for invariant protection; database views for useful read projections. Avoid competing trigger and service side effects.
5. Implement checkout as a single **Java `@Transactional` service operation** with appropriate row locking/concurrency control. No negative stock, partial order, duplicate stock movement, or checkout of ineligible products.
6. Implement restocking and stock logging in **one transaction**.
7. Use `BigDecimal` / PostgreSQL `NUMERIC` for money and explicit rounding rules. Make tax and discount policies explicit and configurable.
8. Supply safe development seed data with **hashed passwords only**; do not copy plaintext demo credentials into production.
9. Test key behaviors: checkout, discounts, expiry rules, alerts, inventory logging, and reports.

**Important:** Do not point the application at Appwrite's internal PostgreSQL database. Use a dedicated PostgreSQL database/container with separate credentials, storage, and backups.

## 7. Android app goals

### Android experience

- App written as a **native Android application**, not WebView/PWA packaging.
- **Jetpack Compose** UI with **Material 3 Expressive** navigation, inputs, cards, chips, sheets, dialogs, and progress states where suitable.
- Screen structure by role: login/registration; dashboard; inventory/products; product detail/edit; restock; POS/cart/checkout; orders/receipts; alerts; profile/settings.
- Responsive/adaptive layouts as appropriate for mobile phones; tablet layouts are a bonus, not core scope.
- Good loading/empty/error states, clear validation, accessibility labels, and sensible handling of API/network failures.
- No direct database credentials in the APK. Store user session/token material with Android-appropriate secure handling; never embed privileged API secrets.
- Inventory and order data must be consistent across Android devices through the backend.

### Language boundary

- **Kotlin:** Composable UI, Compose navigation, UI ViewModels/state, and Android glue where idiomatic.
- **Java:** Primary business logic on the Spring Boot server; Android networking/data classes can also be Java when practical, without creating unnecessary interop complexity.
- Do not rewrite Java service logic in Kotlin on the device. The backend remains authoritative.

## 8. Backend interface

- Serve JSON under `/api/**` with stateless bearer authentication, role and object checks.
- Keep checkout, inventory rules, pricing and reports in Java services; never duplicate them in Compose.
- Serve no browser pages or session login.

## 9. Visual design system

### Typography — locked

- **Inter Variable** is the primary functional UI typeface for navigation, labels, buttons, forms, inventory tables, prices, metrics, and dense operational text.
- **Source Serif 4 Variable** is a selective display typeface for branded headlines, key page/screen titles, onboarding/empty-state headlines, and editorial/report accents.
- Favor Inter for data-heavy interfaces; do not sacrifice readability by using serif text throughout tables or POS interactions.
- Bundle font files in the Android app. Prefer variable fonts with appropriate weight and optical-size support where applicable.
- Use tabular numerals for amounts, stock counts, and aligned financial columns.

### Components and motion — locked direction

- **Android:** Material 3 Expressive components and tokens through Compose Material 3.
- Rounded surfaces, clear hierarchy, tasteful motion, visible focus/pressed states, helpful icons, and an expressive primary action.
- Keep dashboards, table-heavy screens, and checkout flows **functional rather than overly decorative**.
- Respect reduced-motion preferences where practical.

### Color palette — forest ink, cream paper, brass

- **Forest `#1A3A2A`**, **paper `#F5EDDA`**, **brass `#E9AE62`**. The forest tone matches the reused Express favicon, now the Android brand icon.
- Light theme: a textured cream bill-paper ground, forest-green primary actions and ink, brass for charging a sale. Dark theme: deep forest surfaces, cream text and pale green primary actions, with the same warm brass accent.
- Stock-status colours (ok, warning, danger) are separate semantic tokens so they keep their meaning in both themes.
- Support **light and dark themes** with accessible contrast. Optional Android dynamic color must not destroy SmartStock's brand/status semantics.
- Store theme values centrally in the Compose theme, not duplicated as arbitrary colors throughout screens.

## 10. Security and reliability requirements

- Spring Security with secure password hashing (e.g. bcrypt or Argon2), no plaintext-password compatibility, no hard-coded fallback signing key, no credentials committed to Git.
- Android: authenticated HTTPS API calls; explicit token/session lifecycle, secure storage and logout; avoid broad CORS exposure.
- Validate all untrusted inputs on the server; use DTOs, bean validation, parameterized SQL and safe error responses.
- Enforce ownership checks for purchase history, receipts and uploaded content.
- Use transactions and concurrency tests for checkout, cancellations/stock adjustments if introduced, and restocking.
- Maintain audit entries for material inventory changes and applicable administrative actions.
- Avoid leaking sensitive values in logs; keep test fixtures separate from real users and secrets.
- PostgreSQL and file storage need persistent volumes plus a documented backup/restore path.
- Show graceful error states on Android when the backend is unavailable.

## 11. Repository structure (target)

```text
SmartStock/
├── GOAL.md
├── README.md
├── android/                          # Android Gradle project
│   └── app/src/main/
│       ├── java/.../                # Kotlin Compose UI + optional Java data code
│       └── res/font/                # Variable font assets
├── server/                           # Spring Boot Gradle project
│   └── src/
│       ├── main/java/.../
│       │   ├── config/
│       │   ├── security/
│       │   ├── auth/
│       │   ├── inventory/
│       │   ├── billing/
│       │   ├── reporting/
│       │   └── common/
│       ├── main/resources/
│       │   ├── db/migration/         # Flyway PostgreSQL SQL
│       └── test/
├── docs/                              # API, diagrams, design notes, demo, tests
├── compose.yaml
└── .github/workflows/                 # Build/test CI
```

`src/main/java` may contain Kotlin `.kt` sources in a mixed Android project, although a dedicated Kotlin source directory may be chosen during implementation.

## 12. Development phases and gates

### Phase 0 — Scope and foundation

- [x] Capture reference features, role rules, endpoints, schema, and screen flows.
- [x] Create repository layout, build conventions, CI, and release automation.
- [x] Establish Docker Compose with Spring Boot and PostgreSQL.
- [x] Create an initial architecture diagram and checklist.

**Gate:** One-command local backend/database startup, documented environment configuration, and health check.

### Phase 1 — Java backend and PostgreSQL

- [x] Implement Flyway schema and safe test fixtures.
- [x] Implement Spring Security and role-aware APIs.
- [x] Implement products, categories, inventory, POS/checkout, orders, alerts, and reporting.
- [x] Verify transaction atomicity, stock concurrency, expiry/discount logic, and authorization.

**Gate:** Tested APIs deliver the essential SmartStock operations.

### Phase 2 — Retired website

- [x] Remove the abandoned browser UI, session authentication, templates, assets and web-only tests.
- [x] Preserve the REST API and domain services used by Android.

**Gate:** Spring Boot remains an Android API backend, with no browser UI.

### Phase 3 — Native Android app

- [x] Implement theme, fonts, navigation, authentication, and networking.
- [x] Implement role-aware Compose screens for the essential workflows.
- [x] Handle loading/empty/error cases and mobile accessibility.
- [ ] Validate Android actions against the database and a second Android session.

**Gate:** Entire primary demo can be executed in the Android app.

### Phase 4 — Refinement, tests and deployment

- [x] Finalize selected color palette and light/dark themes.
- [ ] Add targeted enhancements only after the core scope is complete.
- [ ] Run unit, integration, Android UI, and cross-device acceptance tests.
- [ ] Produce production-like Docker deployment, backups, documentation, and a local fallback.

**Gate:** Reproducible demo and passing critical test suite.

### Phase 5 — UCS553 submission and demonstration

- [ ] Produce architecture, class, component, and database diagrams.
- [ ] Document Java/OOP concepts, packages, interfaces, exception handling, data access, concurrency/transactions, and testing.
- [ ] Explain the architecture, design decisions, and contribution of each team member.
- [ ] Prepare a concise Android demonstration with the Java API and database as evidence.

**Gate:** The instructor can inspect the Java code, build the application, test meaningful workflows, and understand the project's academic contribution.

## 13. Definition of done / acceptance criteria

A release is considered **course-demo ready** only when all the following are true:

1. A fresh clone can start Spring Boot + PostgreSQL with documented configuration, migrations, and safe demo data.
2. A native Android APK can be built and used for the primary workflows.
3. Logging in as Admin, Manager, Cashier, and Customer produces the expected role-specific access rules in Android and the API.
4. Adding or restocking an item on one Android device makes the updated item/quantity visible after refresh on another.
5. A completed sale updates order records, item snapshots, stock counts, movement logs, and analytics correctly and atomically.
6. Simultaneous attempts to purchase the final available unit do **not** oversell, and failed transactions leave no partial changes.
7. Invalid/expired/out-of-stock products cannot be sold; unauthorized access and cross-customer order access are rejected.
8. PostgreSQL starts from Flyway migrations.
9. Inter Variable and Source Serif 4 Variable are integrated; the selected brand palette works in both Android themes.
10. The deployment can run on the homelab **without Appwrite**, and a portable local demonstration fallback is documented.
11. Build, test, architecture, team contribution, and demo instructions are committed to the repository.

## 14. Deliberate non-goals

- No Kotlin Multiplatform shared UI and no mandatory iOS or desktop app.
- No replacement of Spring Boot business logic with Appwrite, Supabase, or another BaaS.
- No microservice decomposition, Kubernetes, message broker, or distributed event system for the initial course submission.
- No full offline-first Android synchronization engine unless the core system is completed early.
- No mandatory real-world payment-gateway processing; record a payment **mode**, not card details.
- No UI polish that delays core features or transactional correctness.

## 15. Open decisions

| Decision | Current status |
| --- | --- |
| Overall architecture: Android app + Spring Boot REST API + PostgreSQL; no web UI | **Chosen** |
| Android UI: Kotlin Compose + M3 Expressive | **Chosen** |
| Appwrite dependency | **Excluded** |
| KMP | **Excluded** |
| Core fonts: Inter Variable + Source Serif 4 Variable | **Chosen** |
| Brand colors and icon | Forest `#1A3A2A`, paper `#F5EDDA`, brass `#E9AE62`; reuse the reference Express favicon |
| Exact Spring Boot / Compose / dependency versions | Choose compatible stable versions during bootstrap |
| API authentication for Android | **Chosen:** HS256 JWT bearer tokens via Spring Security resource server; account reloaded per request |
| Optional enhancements (barcode scan, notifications, advanced exports) | After core scope |
| Exact homelab hostname/infrastructure topology | Deployment configuration; not a product dependency |

## 16. References

- Feature reference: `docs/reference-audit.md`
- Palette-selection reference: https://www.instagram.com/reel/DfuhfgpRQxa/

---

**Working principle:** Build a maintainable, secure, testable, Java-centric enterprise product. Prioritize **correctness → core scope → native Android quality → visual polish → optional enhancements**.
