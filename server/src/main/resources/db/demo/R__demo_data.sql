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
        ('Dairy',         'Paneer 200 g',            '8900000000141',  90.00, 12, 10,   6, 'Refrigerate After Opening')
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
