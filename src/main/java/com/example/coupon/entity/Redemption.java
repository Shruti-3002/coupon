package com.example.coupon.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "redemptions")
public class Redemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    private String userId;

    private LocalDateTime redeemedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public Coupon getCoupon() { return coupon; }
    public String getUserId() { return userId; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }

    public void setId(Long id) { this.id = id; }
    public void setCoupon(Coupon coupon) { this.coupon = coupon; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
}