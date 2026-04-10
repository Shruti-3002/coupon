package com.example.coupon.controller;

import com.example.coupon.service.CouponService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CouponController {

    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    // BROKEN — race condition lives here
    // POST /api/redeem-coupon?couponId=1&userId=user1
    @PostMapping("/redeem-coupon")
    public ResponseEntity<String> redeem(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        String result = couponService.redeemNaive(couponId, userId);
        return ResponseEntity.ok(result);
    }

    // FIX 1 — synchronized method
    // POST /api/redeem-coupon/synchronized?couponId=1&userId=user1
    @PostMapping("/redeem-coupon/synchronized")
    public ResponseEntity<String> redeemSynchronized(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        String result = couponService.redeemSynchronized(couponId, userId);
        return ResponseEntity.ok(result);
    }

    // v3 — FIX 3: SELECT FOR UPDATE (pessimistic lock)
    // POST /api/v3/redeem-coupon?couponId=1&userId=user1
    @PostMapping("/v3/redeem-coupon")
    public ResponseEntity<String> redeemV3(
            @RequestParam Long couponId,
            @RequestParam String userId) {

        return ResponseEntity.ok(couponService.redeemPessimisticLock(couponId, userId));
    }
}