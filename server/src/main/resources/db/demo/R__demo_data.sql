-- Demo data for the "demo" profile. Every account's password is "smartstock".
-- Idempotent: rows that already exist are skipped.

INSERT INTO users (username, password_hash, role, full_name, phone, email) VALUES
    ('admin',    '{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'ADMIN',    'Asha Admin',      '9000000001', 'admin@example.com'),
    ('manager',  '{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'MANAGER',  'Manav Manager',   '9000000002', 'manager@example.com'),
    ('cashier',  '{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'CASHIER',  'Chetan Cashier',  '9000000003', 'cashier@example.com'),
    ('cashier2', '{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'CASHIER',  'Kiran Cashier',   '9000000004', 'cashier2@example.com'),
    ('customer', '{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'CUSTOMER', 'Chitra Customer', '9000000005', 'customer@example.com'),
    ('customer2','{bcrypt}$2y$10$pq1T2Cc7t2021hAiRPKfNelv9ASNm6mpnZqnHX3tRF13DAak8rpy2', 'CUSTOMER', 'Dev Customer',    '9000000006', 'customer2@example.com')
ON CONFLICT DO NOTHING;

INSERT INTO categories (name, description) VALUES
    ('Groceries',     'Staples and everyday food'),
    ('Beverages',     'Soft drinks, juices, tea and coffee'),
    ('Medicines',     'Over-the-counter and prescription products'),
    ('Personal Care', 'Hygiene and beauty products'),
    ('Snacks',        'Biscuits, chips and packaged snacks'),
    ('Dairy',         'Milk, curd, butter and cheese')
ON CONFLICT DO NOTHING;

WITH seed (category, name, barcode, price, stock, reorder_level, expiry_days, warning_label) AS (
    VALUES
        ('Groceries',     'Basmati Rice 5 kg',       '8900000000011', 549.00, 40, 10, NULL::INTEGER, NULL),
        ('Groceries',     'Whole Wheat Atta 5 kg',   '8900000000028', 289.00, 35, 10, 120, NULL),
        ('Groceries',     'Toor Dal 1 kg',           '8900000000035', 165.00,  8, 10, 200, NULL),
        ('Beverages',     'Masala Tea 250 g',        '8900000000042', 140.00, 50, 15, 300, NULL),
        ('Beverages',     'Orange Juice 1 L',        '8900000000059', 110.00, 24, 12,  12, 'Refrigerate After Opening'),
        ('Beverages',     'Mineral Water 1 L',       '8900000000066',  20.00, 120, 30, 365, NULL),
        ('Medicines',     'Paracetamol 500 mg (10)', '8900000000073',  25.00, 60, 20, 400, NULL),
        ('Medicines',     'Cough Syrup 100 ml',      '8900000000080',  95.00, 15, 10,  -5, 'Prescription Required'),
        ('Personal Care', 'Herbal Shampoo 340 ml',   '8900000000097', 210.00, 30, 10, NULL, NULL),
        ('Personal Care', 'Toothpaste 150 g',        '8900000000103',  98.00,  5, 10, NULL, NULL),
        ('Snacks',        'Salted Chips 90 g',       '8900000000110',  30.00, 80, 20,  45, NULL),
        ('Snacks',        'Peanut Cookies 200 g',    '8900000000127',  55.00, 40, 15,  25, 'Allergen: Contains Nuts'),
        ('Dairy',         'Toned Milk 1 L',          '8900000000134',  56.00, 30, 20,   3, 'Refrigerate After Opening'),
        ('Dairy',         'Paneer 200 g',            '8900000000141',  90.00, 12, 10,   6, 'Refrigerate After Opening'),
        ('Groceries',     'Rolled Oats 1 kg',         '8900000000158', 240.00, 200, 20, 180, NULL),
        ('Beverages',     'Cold Brew Coffee 250 ml', '8900000000165', 160.00, 200, 20,  45, NULL),
        ('Snacks',        'Dark Chocolate 100 g',    '8900000000172', 120.00, 200, 20, 120, 'Allergen: Contains Milk'),
        ('Personal Care', 'Hand Wash 500 ml',        '8900000000189', 180.00, 200, 20, 300, NULL)
),
inserted AS (
    INSERT INTO products (category_id, name, barcode, price, stock, reorder_level, expiry_date, warning_label)
    SELECT c.id, s.name, s.barcode, s.price, s.stock, s.reorder_level,
           CASE WHEN s.expiry_days IS NULL THEN NULL ELSE current_date + s.expiry_days END,
           s.warning_label
    FROM seed s JOIN categories c ON c.name = s.category
    ON CONFLICT DO NOTHING
    RETURNING id, stock
)
INSERT INTO stock_movements (product_id, change, reason, performed_by, notes)
SELECT i.id, i.stock, 'INITIAL', u.id, 'Demo data'
FROM inserted i, users u
WHERE u.username = 'admin' AND i.stock > 0;

INSERT INTO discount_rules (name, min_quantity, min_amount, percent)
SELECT r.name, r.min_quantity, r.min_amount, r.percent
FROM (VALUES
    ('Bulk buy (10+ items)',   10,    0,  5.00),
    ('Big basket (500+)',       1,  500, 10.00),
    ('Mega basket (1000+)',     1, 1000, 15.00)
) AS r (name, min_quantity, min_amount, percent)
WHERE NOT EXISTS (SELECT 1 FROM discount_rules d WHERE d.name = r.name);

-- Named fixtures keep repeatable migrations from adding the same receipts twice.
CREATE TABLE IF NOT EXISTS demo_seed_orders (
    seed_key VARCHAR(32) PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders (id)
);

CREATE TEMP TABLE demo_sale_lines (
    seed_key VARCHAR(32) NOT NULL,
    barcode VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_sale_lines (seed_key, barcode, quantity) VALUES
    ('demo-01', '8900000000158', 2), ('demo-01', '8900000000165', 2),
    ('demo-02', '8900000000189', 2), ('demo-02', '8900000000172', 2),
    ('demo-03', '8900000000158', 1), ('demo-03', '8900000000172', 2),
    ('demo-04', '8900000000165', 1), ('demo-04', '8900000000172', 1),
    ('demo-05', '8900000000189', 1), ('demo-05', '8900000000158', 1),
    ('demo-06', '8900000000165', 3), ('demo-06', '8900000000172', 2),
    ('demo-07', '8900000000158', 3), ('demo-07', '8900000000189', 2),
    ('demo-08', '8900000000172', 3), ('demo-08', '8900000000165', 1),
    ('demo-09', '8900000000158', 2), ('demo-09', '8900000000189', 1),
    ('demo-10', '8900000000189', 2), ('demo-10', '8900000000172', 1),
    ('demo-11', '8900000000165', 2), ('demo-11', '8900000000158', 1);

DO $$
DECLARE
    sale RECORD;
    sale_id BIGINT;
    subtotal NUMERIC(12, 2);
    discount NUMERIC(12, 2);
    tax NUMERIC(12, 2);
    units INTEGER;
BEGIN
    FOR sale IN
        SELECT * FROM (VALUES
            ('demo-01',   0,  1, 'customer',  'cashier',  'UPI'),
            ('demo-02',   0,  2, 'customer2', 'cashier2', 'CARD'),
            ('demo-03',   0,  3, 'customer',  'cashier',  'CASH'),
            ('demo-04',   1,  4, 'customer2', 'cashier',  'CASH'),
            ('demo-05',   7,  5, 'customer',  'cashier2', 'UPI'),
            ('demo-06',  20,  6, 'customer2', 'cashier',  'CARD'),
            ('demo-07',  40,  7, 'customer',  'cashier2', 'CASH'),
            ('demo-08',  70,  8, 'customer2', 'cashier',  'UPI'),
            ('demo-09', 100,  9, 'customer',  'cashier2', 'CARD'),
            ('demo-10', 130, 10, 'customer2', 'cashier',  'CASH'),
            ('demo-11', 160, 11, 'customer',  'cashier2', 'UPI')
        ) AS s(seed_key, age_days, ordinal, customer, cashier, payment)
    LOOP
        IF EXISTS (SELECT 1 FROM demo_seed_orders WHERE seed_key = sale.seed_key) THEN
            CONTINUE;
        END IF;

        IF EXISTS (
            SELECT 1 FROM demo_sale_lines l
            LEFT JOIN products p ON p.barcode = l.barcode
            WHERE l.seed_key = sale.seed_key AND (p.id IS NULL OR p.stock < l.quantity)
        ) THEN
            RAISE EXCEPTION 'Demo products unavailable for %', sale.seed_key;
        END IF;

        SELECT sum(p.price * l.quantity), sum(l.quantity)
        INTO subtotal, units
        FROM demo_sale_lines l JOIN products p ON p.barcode = l.barcode
        WHERE l.seed_key = sale.seed_key;
        IF subtotal IS NULL THEN
            RAISE EXCEPTION 'Missing demo sale lines for %', sale.seed_key;
        END IF;

        discount := round(subtotal * CASE
            WHEN subtotal >= 1000 THEN 0.15
            WHEN subtotal >= 500 THEN 0.10
            WHEN units >= 10 THEN 0.05
            ELSE 0
        END, 2);
        tax := round((subtotal - discount) * 0.18, 2);

        INSERT INTO orders (customer_id, cashier_id, payment_mode, subtotal, discount, tax_rate, tax, total, created_at)
        VALUES (
            (SELECT id FROM users WHERE username = sale.customer),
            (SELECT id FROM users WHERE username = sale.cashier),
            sale.payment, subtotal, discount, 18.00, tax, subtotal - discount + tax,
            now() - make_interval(days => sale.age_days, secs => sale.ordinal)
        ) RETURNING id INTO sale_id;

        INSERT INTO demo_seed_orders (seed_key, order_id) VALUES (sale.seed_key, sale_id);

        INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, line_total)
        SELECT sale_id, p.id, p.name, l.quantity, p.price, p.price * l.quantity
        FROM demo_sale_lines l JOIN products p ON p.barcode = l.barcode
        WHERE l.seed_key = sale.seed_key;

        UPDATE products p SET stock = p.stock - i.quantity, version = p.version + 1, updated_at = now()
        FROM (SELECT product_id, sum(quantity) AS quantity FROM order_items
              WHERE order_id = sale_id GROUP BY product_id) i
        WHERE p.id = i.product_id;

        INSERT INTO stock_movements (product_id, change, reason, order_id, performed_by, notes, created_at)
        SELECT i.product_id, -i.quantity, 'SALE', sale_id,
               (SELECT id FROM users WHERE username = sale.cashier),
               'Demo sale', o.created_at
        FROM order_items i JOIN orders o ON o.id = i.order_id
        WHERE i.order_id = sale_id;
    END LOOP;
END $$;

INSERT INTO notifications (type, message, product_id)
SELECT 'LOW_STOCK', 'Low stock: ' || p.name || ' (' || p.stock || ' remaining)', p.id
FROM products p
WHERE p.barcode IN ('8900000000035', '8900000000103') AND p.stock <= p.reorder_level
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.type = 'LOW_STOCK' AND n.product_id = p.id);

INSERT INTO notifications (type, message, product_id)
SELECT 'EXPIRY', 'Check expiry: ' || p.name, p.id
FROM products p
WHERE p.barcode IN ('8900000000080', '8900000000134', '8900000000141')
  AND p.expiry_date <= current_date + 30
  AND NOT EXISTS (SELECT 1 FROM notifications n WHERE n.type = 'EXPIRY' AND n.product_id = p.id);
