#!/bin/bash

echo "Firing 100 concurrent requests..."

for i in $(seq 1 100); do
    curl -s -X POST "http://localhost:8080/api/v1/redeem-coupon?couponId=1&userId=user$i" &
done

wait

echo ""
echo "Done. Check DB for redemption count."