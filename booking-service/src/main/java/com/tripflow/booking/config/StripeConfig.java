package com.tripflow.booking.config;

import com.stripe.StripeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(@Value("${stripe.api.secret-key}") String secretKey) {
        return new StripeClient(secretKey);
    }
}