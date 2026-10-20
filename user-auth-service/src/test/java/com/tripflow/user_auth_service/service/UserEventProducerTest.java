package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.config.RabbitmqConfig;
import com.tripflow.user_auth_service.dto.events.EventDTO;
import com.tripflow.user_auth_service.dto.events.UserDeletedMessageDTO;
import com.tripflow.user_auth_service.dto.events.UserProfileUpdatedMessageDTO;
import com.tripflow.user_auth_service.dto.events.UserRegisteredMessageDTO;
import com.tripflow.user_auth_service.model.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserEventProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private UserEventProducer producer;

    @Test
    @SuppressWarnings("unchecked")
    void publishUserRegistered_inviaMessaggioSuRabbit() {
        UUID userId = UUID.randomUUID();
        producer.publishUserRegistered(userId, "kc-123", "mario@example.com", Role.TRAVELER, "Mario", "Rossi");

        ArgumentCaptor<EventDTO> captor = ArgumentCaptor.forClass(EventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitmqConfig.USER_EXCHANGE),
                eq("user.registered"),
                captor.capture()
        );

        EventDTO evento = captor.getValue();
        assertEquals("USER_REGISTERED", evento.getEventType());
        UserRegisteredMessageDTO payload = (UserRegisteredMessageDTO) evento.getPayload();
        assertEquals(userId, payload.userId());
        assertEquals("kc-123", payload.keycloakId());
        assertEquals("mario@example.com", payload.email());
        assertEquals(Role.TRAVELER, payload.role());
        assertEquals("Mario", payload.firstName());
        assertEquals("Rossi", payload.lastName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishUserDeleted_inviaMessaggioSuRabbit() {
        UUID userId = UUID.randomUUID();
        producer.publishUserDeleted(userId, "kc-123", "mario@example.com");

        ArgumentCaptor<EventDTO> captor = ArgumentCaptor.forClass(EventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitmqConfig.USER_EXCHANGE),
                eq("user.deleted"),
                captor.capture()
        );

        EventDTO evento = captor.getValue();
        assertEquals("USER_DELETED", evento.getEventType());
        UserDeletedMessageDTO payload = (UserDeletedMessageDTO) evento.getPayload();
        assertEquals(userId, payload.userId());
        assertEquals("kc-123", payload.keycloakId());
        assertEquals("mario@example.com", payload.email());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishUserProfileUpdated_inviaMessaggioSuRabbit() {
        UUID userId = UUID.randomUUID();
        producer.publishUserProfileUpdated(userId, "kc-123", "Mario", "Rossi", "avatar.png");

        ArgumentCaptor<EventDTO> captor = ArgumentCaptor.forClass(EventDTO.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitmqConfig.USER_EXCHANGE),
                eq("user.updated"),
                captor.capture()
        );

        EventDTO evento = captor.getValue();
        assertEquals("USER_UPDATED", evento.getEventType());
        UserProfileUpdatedMessageDTO payload = (UserProfileUpdatedMessageDTO) evento.getPayload();
        assertEquals(userId, payload.userId());
        assertEquals("Mario", payload.firstName());
        assertEquals("Rossi", payload.lastName());
        assertEquals("avatar.png", payload.profileImage());
    }
}
