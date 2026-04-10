-- Recreate cleanly on each restart (dev only)
DROP TABLE IF EXISTS redemptions;
DROP TABLE IF EXISTS coupons;

-- A coupon with limited quantity
CREATE TABLE coupons (
    id       BIGSERIAL   PRIMARY KEY,
    code     VARCHAR(50) NOT NULL UNIQUE,
    quantity INT         NOT NULL
);

-- One row = one redemption attempt that succeeded
-- Bug is visible: COUNT(*) should never exceed coupons.quantity
CREATE TABLE redemptions (
    id          BIGSERIAL    PRIMARY KEY,
    coupon_id   BIGINT       NOT NULL REFERENCES coupons(id),
    user_id     VARCHAR(100) NOT NULL,
    redeemed_at TIMESTAMP    DEFAULT NOW(),

    -- Fix 2: DB enforces only 1 redemption per coupon (quantity = 1)
    -- Second INSERT throws DataIntegrityViolationException → caught in service
    CONSTRAINT unique_coupon_redemption UNIQUE (coupon_id)
);

-- Seed data: 1 coupon, only 1 person allowed to redeem
INSERT INTO coupons (code, quantity) VALUES ('SAVE100', 1);