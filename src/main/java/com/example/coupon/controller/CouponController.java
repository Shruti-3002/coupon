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
}