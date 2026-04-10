package com.example.coupon.repository;

import com.example.coupon.entity.Coupon;
import com.example.coupon.entity.Redemption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    // JPA generates: SELECT COUNT(*) FROM redemptions WHERE coupon_id = ?
    long countByCoupon(Coupon coupon);
}