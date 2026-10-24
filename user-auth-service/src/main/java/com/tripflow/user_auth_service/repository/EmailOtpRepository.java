package com.tripflow.user_auth_service.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tripflow.user_auth_service.model.EmailOtp;
import com.tripflow.user_auth_service.model.OtpPurpose;

public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    //Ultimo codice emesso (non ancora consumato) per quell'utente e scopo.
    //Lock in scrittura: verifiche parallele dello stesso codice si mettono in coda, così il
    //contatore dei tentativi non può essere aggirato con richieste concorrenti.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailOtp> findFirstByKeycloakIdAndPurposeAndConsumedFalseOrderByCreatedAtDesc(
            String keycloakId, OtpPurpose purpose);

    //Invalida i codici ancora attivi: chiamato prima di emetterne uno nuovo,
    //altrimenti un codice vecchio tornerebbe valido dopo l'uso di quello recente.
    @Modifying
    @Query("UPDATE EmailOtp o SET o.consumed = true "
            + "WHERE o.keycloakId = :keycloakId AND o.purpose = :purpose AND o.consumed = false")
    int invalidateActive(@Param("keycloakId") String keycloakId, @Param("purpose") OtpPurpose purpose);

    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.keycloakId = :keycloakId")
    int deleteByKeycloakId(@Param("keycloakId") String keycloakId);

    //Pulizia: elimina i codici ormai inutili (scaduti o già consumati).
    @Modifying
    @Query("DELETE FROM EmailOtp o WHERE o.expiresAt < :now OR o.consumed = true")
    int deleteExpiredOrConsumed(@Param("now") LocalDateTime now);
}
