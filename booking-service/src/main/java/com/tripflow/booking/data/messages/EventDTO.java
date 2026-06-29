package com.tripflow.booking.data.messages;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventDTO<T> {

    private String eventType;
    private T payload;
}
