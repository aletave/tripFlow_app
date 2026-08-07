package com.tripflow.catalog_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
public class EventDTO<T> {

    private String eventType;
    private T payload;
    private LocalDateTime timestamp;

    public EventDTO(String e, T p) {
        this.eventType=e;
        this.payload=p;
        this.timestamp=LocalDateTime.now();
    }
}
