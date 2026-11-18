# Reference audit

Feature reference: an earlier inventory and billing app (commit `e668b9f`), reviewed for scope only. Node.js + Express 4 + mysql2 backend in a single `server.js`; vanilla JS SPA (`index.html`, `js/*.js`, `css/style.css`); MySQL schema, views, routines, triggers and seed data in `schema.sql`.

This document records behavior worth matching, and defects to avoid.

## 1. Roles and navigation

| Role | Home screen | Sidebar |
| --- | --- | --- |
| admin | dashboard | Dashboard, Products, Orders, Users, Analytics, Alerts |
| manager | dashboard | Dashboard, Products, Orders, Analytics, Alerts |
| cashier | billing | Billing, My Bills (actually all orders) |
| customer | customer-home | My Account, My Purchases |

Auth state is a 12 h JWT stored in `localStorage` (`ss_token`, `ss_user`); logout only clears local storage.

## 2. API inventory

All responses are `{ success, ...data }` or `{ success: false, error }`.

| Method | Path | Roles | Behavior |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | public | Active user by username; bcrypt **or plaintext** match; returns token + profile |
| POST | `/api/auth/register` | public | Customer self-registration; bcrypt(10); returns token |
| GET | `/api/auth/me` | any | Current profile |
| GET | `/api/categories` | **public** | All categories by name |
| POST | `/api/categories` | admin, manager | Create category |
| GET | `/api/products` | any | Active products + `days_to_expiry`; filters `category`, `search` (name/barcode LIKE), `cashier=1` (stock > 0 and not expired) |
| GET | `/api/products/:id` | any | Single product (active or not) |
| POST | `/api/products` | admin, manager | Create; defaults stock 0, reorder 10 |
| PUT | `/api/products/:id` | admin, manager | Full overwrite of editable fields |
| POST | `/api/products/:id/restock` | admin, manager | `stock += qty`, then insert `restock` stock log |
| POST | `/api/orders` | cashier, admin, manager | Checkout (see §4) |
| GET | `/api/orders/:id` | **any** | Order header + items (receipt) |
| GET | `/api/orders` | any | Customer: own orders; staff: all orders, or `customer_id` filter; latest 100 |
| GET | `/api/analytics/dashboard` | admin, manager | Today summary, top 5 ticket items, 10 expiry alerts, 20 unread notifications, 6-month monthly sales |
| GET | `/api/analytics/products` | admin, manager | Per-product sales summary by revenue |
| GET | `/api/analytics/clearance` | admin, manager | In-stock active products unsold for 30 days, by expiry |
| GET | `/api/analytics/revenue-by-category` | admin, manager | Units and revenue per category (completed orders) |
| GET | `/api/users` | admin | All users, optional `role` filter |
| POST | `/api/users` | admin | Create user of any role |
| GET | `/api/users/customers` | cashier, admin, manager | Active customer search (name/phone/username), 20 max |
| GET | `/api/notifications` | admin, manager | Latest 50 with product name |
| PUT | `/api/notifications/:id/read` | admin, manager | Mark read |
| GET | `/api/stock-log` | admin, manager | Latest 100 movements, optional `product_id` (no UI uses it) |

## 3. Schema

Tables: `CATEGORY`, `USER`, `PRODUCT`, `ORDERS`, `ORDER_ITEMS`, `STOCK_LOG`, `DISCOUNT_RULES`, `NOTIFICATION`.

| Table | Notable columns and constraints |
| --- | --- |
| USER | unique username; role enum admin/manager/cashier/customer; phone `^[0-9]{10}$`; `is_active` |
| PRODUCT | unique barcode; `price DECIMAL(10,2) >= 0`; `stock_qty >= 0`; `reorder_level` 10; `expiry_date`; `warning_label`; `image_url`; `is_active`; FK category |
| ORDERS | customer and cashier FKs to USER; subtotal, discount, `tax_rate` (default 18, never written), tax_amount, total_bill; payment_mode cash/card/upi/other; status pending/completed/cancelled |
| ORDER_ITEMS | `qty_sold > 0`; `unit_price` snapshot; `discount_pct` (never written); `line_total`; cascade on order delete |
| STOCK_LOG | signed `change_qty`; reason sale/restock/adjustment/expired_removal; `reference_id` (order id); `performed_by` (no FK) |
| DISCOUNT_RULES | `min_qty`, `min_amount`, `discount_pct`, `is_active`, `valid_from`/`valid_to` |
| NOTIFICATION | type low_stock/expiry_warning/expired/info; `is_read`; optional product FK |

Indexes: product(category, expiry), orders(customer, cashier, date), order_items(order, product), stock_log(product).

Views used by the API: `vw_daily_summary`, `vw_highest_ticket_items`, `vw_expiry_alerts` (0–30 days, in stock), `vw_product_sales_summary`. Unused: `vw_cashier_products`, `vw_customer_history`.

Routines: only `fn_get_discount(qty, amount)` (max active, date-valid `discount_pct` whose thresholds are met) is called. `sp_checkout`, `sp_restock_product`, `sp_register_customer`, `sp_analytics_report` are dead code.

Triggers (all active):

| Trigger | Effect |
| --- | --- |
| `trg_block_expired_product` | BEFORE INSERT order item: reject expired product or insufficient stock |
| `trg_low_stock_alert` | AFTER UPDATE product: `low_stock` notification when stock first crosses to `<= reorder_level` |
| `trg_product_create_log` | AFTER INSERT product: `restock` stock log of initial stock, no performer |
| `trg_expiry_warning` | AFTER INSERT product: `expiry_warning` notification if expiry within 0–30 days |

Seed data: 6 categories, 4 users per role with **plaintext** passwords and real-looking personal contact details, 3 discount rules (10+ items 5 %, ₹500+ 10 %, ₹1000+ 15 %). No products or orders.

## 4. Behavior to match

Checkout (`POST /api/orders`, one DB transaction):

1. Require `customer_id` (must be a `customer`) and non-empty `items[{product_id, qty}]`.
2. For each item: `SELECT … FOR UPDATE`; reject missing, expired (`expiry_date < now`), or `stock_qty < qty`.
3. `subtotal = Σ price × qty`, item price snapshotted.
4. Discount: if the client sends none/0, apply `fn_get_discount(total_qty, subtotal)` % of subtotal, rounded to 2 dp; otherwise use the client amount.
5. `tax = round((subtotal − discount) × 0.18, 2)`; `total = subtotal − discount + tax`.
6. Insert completed order, items, decrement stock, insert `sale` stock logs (`reference_id` = order).

Other observed rules:

- Low-stock notification on threshold crossing; expiry warning on product creation; initial stock logged on creation.
- Product table UI flags: stock `<= 0` red, `<= reorder_level` yellow; expiry `<= 7` days red, `<= 30` amber; tabs All / Low stock / Expiring / Clearance.
- Cashier product grid shows only sellable products; cart computes totals client-side with the same 18 % rate (preview only).
- Customer search needs ≥ 2 characters; receipt shows customer, cashier, payment, items, subtotal, discount, GST 18 %, total, and a printable layout.
- Warning labels are a fixed list: Prescription Required, Harmful to Kids, Age 18+ Only, Allergen: Contains Nuts, Refrigerate After Opening, Restricted Drug.

## 5. Defects to avoid

Security:

1. Plaintext-password login fallback; plaintext seed passwords; demo credentials printed on startup and shown on the login page.
2. Hard-coded JWT secret fallback; `cors()` open to all origins; token in `localStorage`; no server-side logout.
3. **IDOR:** any authenticated user (including customers) can read any order via `GET /api/orders/:id`.
4. Raw DB error messages returned to clients with status 500.
5. No input validation on categories, products or user creation (role value unchecked, password may be missing).
6. Pervasive XSS: server data interpolated into `innerHTML` and inline `onclick` handlers.
7. Real personal data and a hosted DB endpoint embedded in `schema.sql`.

Correctness:

8. Restock runs two statements without a transaction and does not verify the product exists or is active.
9. Checkout does not reject inactive products, non-positive quantities, or duplicate lines for the same product (each line is stock-checked independently).
10. Manual discount is unvalidated (negative or above subtotal accepted); any staff role can set it.
11. Money computed with JS floating point; `tax_rate` and `discount_pct` columns never populated.
12. `PUT /api/products/:id` overwrites every column, and the edit form omits description, barcode and image, so **editing a product erases them**.
13. Expiry warnings fire only on insert, never on edit or as dates approach; “today” depends on the DB server clock and time zone.
14. “Register new customer” from billing navigates to public registration, which replaces the cashier's session with the new customer's.
15. Checkout logic is duplicated across `server.js`, `sp_checkout` and a trigger, with competing side effects.
16. Clearance tab fabricates `reorder_level`, `is_active` and other fields client-side.

## 6. Rule ownership in SmartStock

| Rule | New owner |
| --- | --- |
| Checkout, restock, adjustments, stock logging, discount selection, tax | Java `@Transactional` services |
| Low-stock and expiry notifications | Java services (no triggers) |
| Non-negative stock and price, quantity > 0, enums, uniqueness, FKs | PostgreSQL constraints |
| Dashboard and report read models | SQL queries or views |
| Tax rate, discount policy | Configuration and `DISCOUNT_RULES`; rate stored per order |
