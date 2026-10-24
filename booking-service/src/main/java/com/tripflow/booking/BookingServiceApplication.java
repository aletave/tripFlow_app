package com.tripflow.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.tripflow.booking.config.PrenotazioneProperties;
import com.tripflow.booking.config.ScontoProperties;


@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@EnableConfigurationProperties({PrenotazioneProperties.class, ScontoProperties.class})

public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
