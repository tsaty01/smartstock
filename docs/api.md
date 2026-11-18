# REST API

Base path `/api`. JSON in and out; errors are [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) problem details (`status`, `detail`). Money is a decimal with two places; dates are ISO-8601.

## Authentication

`POST /api/auth/login` with `{"username", "password"}` returns `{"token", "expiresAt", "user"}`. Send the token as `Authorization: Bearer <token>`. Tokens are HS256 JWTs signed with `TOKEN_SECRET` and last `smartstock.security.token-ttl` (12 h). Every request reloads the account, so disabling a user or changing their role applies immediately. Logging out means discarding the token.

| Status | Meaning |
| --- | --- |
| 400 | Invalid input |
| 401 | Missing, invalid or expired token; wrong credentials; account disabled after the token was issued |
| 403 | Role not allowed; login to a disabled account |
| 404 | Not found, including other customers' orders |
| 409 | Duplicate (username, barcode, category) or concurrent edit |
| 422 | Business rule: insufficient stock, expired or inactive product, discount above subtotal |

## Endpoints

Roles: **A** admin, **M** manager, **C** cashier, **U** customer.

| Method | Path | Roles | Notes |
| --- | --- | --- | --- |
| POST | `/auth/login` | public | |
| POST | `/auth/register` | public | Customer self-registration; returns a token |
| GET | `/auth/me` | any | |
| GET | `/users?role=` | A | |
| POST | `/users` | A | Any role |
| PUT | `/users/{id}` | A | Role, profile, `active`, optional new `password`; cannot disable or demote yourself |
| GET | `/customers?q=` | A M C | Active customers by name, username or phone (20 max) |
| POST | `/customers` | A M C | Register a customer at the till |
| GET | `/categories` | any | |
| POST, PUT | `/categories`, `/categories/{id}` | A M | |
| GET | `/products?categoryId=&q=&sellable=&includeInactive=` | any | `q` matches name or exact barcode; `sellable` = active, in stock, not expired; `includeInactive` for A M |
| GET | `/products/{id}` | any | Includes `daysToExpiry`, `lowStock`, `expired`, `imageUrl` |
| POST | `/products` | A M | Full product plus optional `initialStock` |
| PUT | `/products/{id}` | A M | Full replacement; stock is not editable here |
| POST | `/products/{id}/restock` | A M | `{"quantity", "notes"}` |
| POST | `/products/{id}/adjustments` | A M | `{"change", "reason": "ADJUSTMENT" or "EXPIRED_REMOVAL", "notes"}` |
| PUT | `/products/{id}/image` | A M | Multipart `file`: PNG, JPEG or WebP, 2 MB max |
| GET | `/products/{id}/image` | any | |
| GET | `/stock-movements?productId=&page=&size=` | A M | Newest first |
| GET | `/notifications?unread=` | A M | Low-stock and expiry alerts |
| POST | `/notifications/{id}/read` | A M | |
| POST | `/orders/quote` | A M C | Prices a cart without reserving stock |
| POST | `/orders` | A M C | Checkout, returns the receipt |
| GET | `/orders?customerId=&page=&size=` | any | Customers always get their own orders |
| GET | `/orders/{id}` | any | Receipt; customers only their own |
| GET | `/discount-rules` | A M C | |
| POST, PUT | `/discount-rules`, `/discount-rules/{id}` | A M | |
| GET | `/reports/dashboard` | A M | Today, 6-month sales, top products (30 days), low stock, expiring, expired, unread alerts |
| GET | `/reports/products?from=&to=` | A M | Sales per product; default last 30 days |
| GET | `/reports/categories?from=&to=` | A M | Sales per category |
| GET | `/reports/clearance?days=30` | A M | In-stock products with no recent sale |

## Checkout rules

Request: `{"customerId", "paymentMode": "CASH|CARD|UPI|OTHER", "items": [{"productId", "quantity"}], "discount"}`.

1. Repeated lines for a product are combined.
2. Product rows are locked in id order; each must be active, not expired (store time zone `smartstock.zone`) and in stock.
3. `subtotal` = sum of `price × quantity`.
4. `discount`: a manual amount (admin and manager only, at most the subtotal), otherwise the highest active discount rule matching total quantity and subtotal.
5. `tax` = `(subtotal − discount) × smartstock.billing.tax-rate` (18 %), stored per order; `total` = `subtotal − discount + tax`. Amounts round half-up to two places.
6. The order, item snapshots (name, unit price), stock decrements, `SALE` movements and low-stock alerts commit together or not at all.
