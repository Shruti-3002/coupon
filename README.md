# Coupon Redemption System — Race Condition Demo

## What We Built

A Spring Boot REST API that lets users redeem a coupon.

**Endpoint:**
```
POST /api/redeem-coupon?couponId=1&userId=user1
```

**Stack:**
- Java 22
- Spring Boot 3.2.4
- PostgreSQL 14
- JPA / Hibernate

---

## The Problem — Race Condition

### Setup
- 1 coupon `SAVE100` with `quantity = 1`
- Only **1 user** should be able to redeem it

### What the code does (broken)
```
1. SELECT COUNT(*) FROM redemptions WHERE coupon_id = 1
2. If count < quantity → INSERT into redemptions
3. Return SUCCESS
```

Steps 1 and 2 are two separate DB calls.
The gap between them is where threads collide.

### What happens under load

```
Thread A → reads count = 0 → passes check → inserts ✓
Thread B → reads count = 0 → passes check → inserts ✓  ← BUG
Thread C → reads count = 0 → passes check → inserts ✓  ← BUG
```

All threads read `count = 0` before any of them finish writing.
So multiple threads pass the check and all insert successfully.

---

## Load Test Result

Fired **100 concurrent requests** using `load-test.sh`:

```bash
for i in $(seq 1 100); do
    curl -s -X POST "http://localhost:8080/api/redeem-coupon?couponId=1&userId=user$i" &
done
wait
```

### Result in DB

```
 id | coupon_id | user_id |        redeemed_at
----+-----------+---------+----------------------------
  1 |         1 | user29  | 2026-04-10 13:33:42.812901
  2 |         1 | user67  | 2026-04-10 13:33:42.812348
  3 |         1 | user94  | 2026-04-10 13:33:42.812372
  4 |         1 | user17  | 2026-04-10 13:33:42.813272
  5 |         1 | user76  | 2026-04-10 13:33:42.812960
  6 |         1 | user59  | 2026-04-10 13:33:42.813790
  7 |         1 | user22  | 2026-04-10 13:33:42.812348
  8 |         1 | user5   | 2026-04-10 13:33:42.812563
  9 |         1 | user6   | 2026-04-10 13:33:42.813090
 10 |         1 | user10  | 2026-04-10 13:33:42.813372
(10 rows)
```

| Expected winners | Actual winners |
|-----------------|----------------|
| 1               | 10 ← BUG       |

All 10 timestamps are within the same millisecond — proof that threads collided simultaneously.

---

## Fix 1 — `synchronized` method ✅

**Endpoint:**
```
POST /api/redeem-coupon/synchronized?couponId=1&userId=user1
```

**How it works:**

The `synchronized` keyword puts a JVM-level lock on the method.
Only 1 thread can enter at a time. All others wait.

```
Without synchronized:               With synchronized:
Thread A → enters                   Thread A → enters
Thread B → enters  (parallel)       Thread B → WAITS
Thread C → enters  (parallel)       Thread C → WAITS
                                    Thread A → exits
                                    Thread B → enters → exits
                                    Thread C → enters → exits
```

**Result with 100 concurrent requests:**

```
 id | coupon_id | user_id |        redeemed_at
----+-----------+---------+----------------------------
  1 |         1 | user56  | 2026-04-10 14:14:36.361113
(1 row)
```

| Expected winners | Actual winners |
|-----------------|----------------|
| 1               | **1 ✅**        |

**Limitation:**

Only works on a single JVM (single server).
In production with multiple instances:

```
Server 1 (JVM 1) — has its own lock in memory
Server 2 (JVM 2) — has its own lock in memory
```

These locks don't know about each other. Race condition comes back.
That's why Fix 2 and Fix 3 use the DB — shared by all servers.

---

## Coming Next

| Fix | Technique | Where protection lives | Works on multiple servers? |
|-----|-----------|----------------------|---------------------------|
| 1 | `synchronized` method ✅ | JVM memory | No |
| 2 | DB unique constraint + catch `DataIntegrityViolationException` | Database | Yes |
| 3 | `SELECT FOR UPDATE` (pessimistic lock) | Database row lock | Yes |

---

## How to Run

### Prerequisites
- Java 17+
- Maven
- PostgreSQL running on port 5432

### Setup
```bash
# Create database
psql -c "CREATE DATABASE coupon_db;"

# Run the app (schema.sql auto-creates tables and seeds data)
mvn spring-boot:run
```

### Reproduce the bug
```bash
bash load-test.sh
```

Then check the DB:
```sql
SELECT COUNT(*) FROM redemptions;  -- should be 1, will be more
```