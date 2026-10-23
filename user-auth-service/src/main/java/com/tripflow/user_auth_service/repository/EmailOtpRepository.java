package com.tripflow.user_auth_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tripflow.user_auth_service.model.EmailOtp;
import com.tripflow.user_auth_service.model.OtpPurpose;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    //Ultimo codice emesso (non ancora consumato) per quell'utente e scopo:
    //se l'utente ne ha richiesti più d'uno (es. "reinvia"), vale solo il più recente.
    Optional<EmailOtp> findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String keycloakId, OtpPurpose purpose);

    //Pulizia: elimina i codici ormai inutili (scaduti o già consumati).
    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now OR o.consumed = true")
    int deleteExpiredOrConsumed(@Param("now") LocalDateTime now);
}
