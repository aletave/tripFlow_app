package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.config.RabbitmqConfig;
import com.tripflow.user_auth_service.dto.events.EventDTO;
import com.tripflow.user_auth_service.dto.events.UserDeletedMessageDTO;
import com.tripflow.user_auth_service.dto.events.UserProfileUpdatedMessageDTO;
import com.tripflow.user_auth_service.dto.events.UserRegisteredMessageDTO;
import com.tripflow.user_auth_service.model.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserRegistered(UUID userId, String keycloakId, String email, Role role, String firstName, String lastName) {
        UserRegisteredMessageDTO payload = new UserRegisteredMessageDTO(userId, keycloakId, email, role, firstName, lastName);
        send("USER_REGISTERED", "user.registered", payload);
    }

    public void publishUserDeleted(UUID userId, String keycloakId, String email) {
        UserDeletedMessageDTO payload = new UserDeletedMessageDTO(userId, keycloakId, email);
        send("USER_DELETED", "user.deleted", payload);
    }

    public void publishUserProfileUpdated(UUID userId, String keycloakId, String firstName, String lastName, String profileImage) {
        UserProfileUpdatedMessageDTO payload = new UserProfileUpdatedMessageDTO(userId, keycloakId, firstName, lastName, profileImage);
        send("USER_UPDATED", "user.updated", payload);
    }

    private void send(String eventType, String routingKey, Object payload) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitmqConfig.USER_EXCHANGE,
                    routingKey,
                    new EventDTO<>(eventType, payload)
            );
            log.info("[RABBITMQ PRODUCER] Evento {} inviato con routingKey={}", eventType, routingKey);
        } catch (AmqpException e) {
            log.error("[RABBITMQ PRODUCER] Invio evento {} FALLITO (RabbitMQ non raggiungibile?): {}",
                    eventType, e.getMessage());
        }
    }
}
