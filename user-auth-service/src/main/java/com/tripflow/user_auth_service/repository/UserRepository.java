package com.tripflow.user_auth_service.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.tripflow.user_auth_service.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    //Utenti che compiono gli anni in un dato giorno (ignorando l'anno di nascita).
    //Usato dal job degli auguri di compleanno.
    @Query(value = "SELECT * FROM users WHERE date_of_birth IS NOT NULL "
            + "AND EXTRACT(MONTH FROM date_of_birth) = :month "
            + "AND EXTRACT(DAY FROM date_of_birth) = :day", nativeQuery = true)
    List<User> findByBirthday(@Param("month") int month, @Param("day") int day);

    Optional<User> findByKeycloakId(String keycloakId);

    boolean existsByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    List<User> findByEmailIn(Collection<String> emails);

    List<User> findByKeycloakIdIn(Collection<String> keycloakIds);
}
