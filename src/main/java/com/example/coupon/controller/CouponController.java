package com.example.coupon.controller;

import com.example.coupon.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // v1 — BROKEN: race condition (no protection)
    // POST /api/v1/redeem-coupon?couponId=1&userId=user1
    @PostMapping("/api/v1/redeem-coupon")
    public ResponseEntity<String> redeemV1(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        return ResponseEntity.ok(couponService.redeemNaive(couponId, userId));
    }

    // v2 — FIX 1: synchronized (JVM-level lock)
    // POST /api/v2/redeem-coupon?couponId=1&userId=user1
    @PostMapping("/api/v2/redeem-coupon")
    public ResponseEntity<String> redeemV2(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        return ResponseEntity.ok(couponService.redeemSynchronized(couponId, userId));
    }

    // v3 — FIX 2: unique constraint + catch DataIntegrityViolationException
    // POST /api/v3/redeem-coupon?couponId=1&userId=user1
    @PostMapping("/api/v3/redeem-coupon")
    public ResponseEntity<String> redeemV3(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        return ResponseEntity.ok(couponService.redeemUniqueConstraint(couponId, userId));
    }
}
