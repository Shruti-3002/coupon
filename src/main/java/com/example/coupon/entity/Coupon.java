package com.example.coupon.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String code;

    private int quantity;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public int getQuantity() { return quantity; }

    public void setId(Long id) { this.id = id; }
    public void setCode(String code) { this.code = code; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
