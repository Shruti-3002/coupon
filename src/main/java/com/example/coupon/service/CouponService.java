package com.example.coupon.service;

import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.Redemption;
import com.example.coupon.repository.CouponRepository;
import com.example.coupon.repository.RedemptionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CouponService {

    private final CouponRepository couponRepository;
    private final RedemptionRepository redemptionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public CouponService(CouponRepository couponRepository,
                         RedemptionRepository redemptionRepository) {
        this.couponRepository = couponRepository;
        this.redemptionRepository = redemptionRepository;
    }

    // ---------------------------------------------------------------
    // BROKEN: classic check-then-act race condition
    //
    // Thread A: reads count = 0  (ok, quantity = 1, so allowed)
    // Thread B: reads count = 0  (also ok — A hasn't written yet!)
    // Thread A: inserts redemption
    // Thread B: inserts redemption  ← BUG: 2 rows for 1 coupon
    // ---------------------------------------------------------------
    @Transactional
    public String redeemNaive(Long couponId, String userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        long alreadyRedeemed = redemptionRepository.countByCoupon(coupon);

        // ← DANGER ZONE: another thread can pass this check simultaneously
        if (alreadyRedeemed >= coupon.getQuantity()) {
            return "FAILED - coupon fully redeemed";
        }

        // ← Both threads reach here and both insert
        Redemption redemption = new Redemption();
        redemption.setCoupon(coupon);
        redemption.setUserId(userId);
        redemptionRepository.save(redemption);

        return "SUCCESS - redeemed by " + userId;
    }

    // ---------------------------------------------------------------
    // FIX 1: synchronized
    //
    // Only 1 thread can enter this method at a time.
    // The JVM puts a lock on the object — every other thread waits.
    // Now the read → check → write happens without interruption.
    //
    // LIMITATION: only works on a single JVM (single server).
    // If you run 2 instances of this app, each has its own lock
    // and the race condition comes back.
    // ---------------------------------------------------------------
    @Transactional
    public synchronized String redeemSynchronized(Long couponId, String userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        long alreadyRedeemed = redemptionRepository.countByCoupon(coupon);

        if (alreadyRedeemed >= coupon.getQuantity()) {
            return "FAILED - coupon fully redeemed";
        }

        Redemption redemption = new Redemption();
        redemption.setCoupon(coupon);
        redemption.setUserId(userId);
        redemptionRepository.save(redemption);

        return "SUCCESS - redeemed by " + userId;
    }

    // ---------------------------------------------------------------
    // FIX 2: unique constraint + catch DataIntegrityViolationException
    //
    // No check at all. Just try to INSERT directly.
    // The DB has UNIQUE(coupon_id) on redemptions table.
    // First thread  → INSERT succeeds ✓
    // Every other   → INSERT rejected by DB → exception → FAILED
    //
    // WHY better than synchronized:
    // The DB is shared across all servers.
    // Works even with 10 instances of this app running.
    // ---------------------------------------------------------------
    @Transactional
    public String redeemUniqueConstraint(Long couponId, String userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new RuntimeException("Coupon not found"));

        try {
            Redemption redemption = new Redemption();
            redemption.setCoupon(coupon);
            redemption.setUserId(userId);
            redemptionRepository.save(redemption);
            entityManager.flush(); // force INSERT now so exception is caught here

            return "SUCCESS - redeemed by " + userId;

        } catch (DataIntegrityViolationException e) {
            return "FAILED - coupon fully redeemed";
        }
    }
}
