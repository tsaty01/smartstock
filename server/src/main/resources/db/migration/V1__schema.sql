CREATE TABLE users (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(16)  NOT NULL CHECK (role IN ('ADMIN', 'MANAGER', 'CASHIER', 'CUSTOMER')),
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(10)  CHECK (phone ~ '^[0-9]{10}$'),
    email         VARCHAR(254),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX users_username_key ON users (lower(username));
CREATE INDEX users_role_idx ON users (role);

CREATE TABLE categories (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX categories_name_key ON categories (lower(name));

CREATE TABLE products (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id   BIGINT        NOT NULL REFERENCES categories (id),
    name          VARCHAR(150)  NOT NULL,
    description   TEXT,
    barcode       VARCHAR(50)   UNIQUE,
    price         NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    stock         INTEGER       NOT NULL DEFAULT 0 CHECK (stock >= 0),
    reorder_level INTEGER       NOT NULL DEFAULT 10 CHECK (reorder_level >= 0),
    expiry_date   DATE,
    warning_label VARCHAR(100),
    image_name    VARCHAR(100),
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX products_category_idx ON products (category_id);
CREATE INDEX products_expiry_idx ON products (expiry_date);

CREATE TABLE discount_rules (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    min_quantity INTEGER       NOT NULL DEFAULT 1 CHECK (min_quantity >= 1),
    min_amount   NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (min_amount >= 0),
    percent      NUMERIC(5, 2) NOT NULL CHECK (percent > 0 AND percent <= 100),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    valid_from   DATE,
    valid_to     DATE,
    CHECK (valid_to IS NULL OR valid_from IS NULL OR valid_to >= valid_from)
);

CREATE TABLE orders (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id  BIGINT        NOT NULL REFERENCES users (id),
    cashier_id   BIGINT        NOT NULL REFERENCES users (id),
    payment_mode VARCHAR(8)    NOT NULL CHECK (payment_mode IN ('CASH', 'CARD', 'UPI', 'OTHER')),
    subtotal     NUMERIC(12, 2) NOT NULL CHECK (subtotal >= 0),
    discount     NUMERIC(12, 2) NOT NULL CHECK (discount >= 0 AND discount <= subtotal),
    tax_rate     NUMERIC(5, 2) NOT NULL CHECK (tax_rate >= 0),
    tax          NUMERIC(12, 2) NOT NULL CHECK (tax >= 0),
    total        NUMERIC(12, 2) NOT NULL CHECK (total >= 0),
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX orders_customer_idx ON orders (customer_id);
CREATE INDEX orders_cashier_idx ON orders (cashier_id);
CREATE INDEX orders_created_idx ON orders (created_at);

CREATE TABLE order_items (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id     BIGINT        NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   BIGINT        NOT NULL REFERENCES products (id),
    product_name VARCHAR(150)  NOT NULL,
    quantity     INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price   NUMERIC(12, 2) NOT NULL CHECK (unit_price >= 0),
    line_total   NUMERIC(12, 2) NOT NULL CHECK (line_total >= 0),
    UNIQUE (order_id, product_id)
);
CREATE INDEX order_items_product_idx ON order_items (product_id);

CREATE TABLE stock_movements (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id   BIGINT      NOT NULL REFERENCES products (id),
    change       INTEGER     NOT NULL CHECK (change <> 0),
    reason       VARCHAR(16) NOT NULL CHECK (reason IN ('INITIAL', 'RESTOCK', 'SALE', 'ADJUSTMENT', 'EXPIRED_REMOVAL')),
    order_id     BIGINT      REFERENCES orders (id),
    performed_by BIGINT      NOT NULL REFERENCES users (id),
    notes        VARCHAR(255),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((reason = 'SALE') = (order_id IS NOT NULL))
);
CREATE INDEX stock_movements_product_idx ON stock_movements (product_id, created_at DESC);

CREATE TABLE notifications (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type       VARCHAR(16)  NOT NULL CHECK (type IN ('LOW_STOCK', 'EXPIRY')),
    message    VARCHAR(255) NOT NULL,
    product_id BIGINT       REFERENCES products (id),
    read       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX notifications_unread_idx ON notifications (created_at DESC) WHERE NOT read;
