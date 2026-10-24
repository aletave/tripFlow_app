package com.tripflow.booking.client;

import com.tripflow.booking.client.dto.BirthdayCouponDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "user-auth-service")
public interface UserAuthClient {

    @GetMapping("/api/auth/me/birthday-coupon")
    BirthdayCouponDTO getBirthdayCoupon();
}
