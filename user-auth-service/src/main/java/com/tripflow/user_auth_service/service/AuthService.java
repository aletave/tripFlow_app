package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.config.KeycloakAdminClient;
import com.tripflow.user_auth_service.dto.request.ChangePasswordRequest;
import com.tripflow.user_auth_service.dto.request.RegisterRequest;
import com.tripflow.user_auth_service.dto.request.UpdateProfileRequest;
import com.tripflow.user_auth_service.dto.response.PublicUserResponse;
import com.tripflow.user_auth_service.dto.response.RegisterResponse;
import com.tripflow.user_auth_service.dto.response.UserResponse;
import com.tripflow.user_auth_service.exception.EmailAlreadyExistsException;
import com.tripflow.user_auth_service.exception.ResourceNotFoundException;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

//Service di autenticazione e gestione profilo.
//Le credenziali vivono solo su Keycloak: il DB locale contiene solo i dati del profilo
//con il riferimento keycloak_id. Login/refresh/logout NON esistono qui:
//il client li fa direttamente su Keycloak.
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    //Registrazione con scrittura doppia (dual-write) in ordine DB-check -> Keycloak -> DB-save.
    //Se il salvataggio su DB fallisce, compensa cancellando l'utente da Keycloak (con 1 retry).
    public RegisterResponse register(RegisterRequest request) {
        //email normalizzata: Keycloak è case-insensitive, il DB no — evita doppioni con maiuscole
        String email = request.email().toLowerCase(Locale.ROOT);

        //1. controllo su DB: email già registrata -> 409, STOP
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        //2. creazione su Keycloak (+ assegnazione ruolo); su 409 il client adotta l'account
        //orfano solo se non esiste già nel DB locale (predicate). Si ricontrolla anche l'email:
        //nel frattempo un'altra registrazione concorrente potrebbe aver salvato proprio quell'utente
        String keycloakId = keycloakAdminClient.createUser(
                email,
                request.password(),
                request.firstName(),
                request.lastName(),
                request.role().name(),
                id -> !userRepository.existsByKeycloakId(id) && !userRepository.existsByEmail(email));

        //3. salvataggio su DB, con compensazione in caso di errore
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(email);
        user.setRole(request.role());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhoneNumber(request.phoneNumber());
        user.setProfileImage(request.profileImage());

        try {
            userRepository.save(user);
        } catch (RuntimeException e) {
            compensaUtenteOrfano(keycloakId);
            throw e;
        }

        log.info("Utente registrato: id={}, email={}", user.getId(), user.getEmail());
        return new RegisterResponse(user.getId(), user.getEmail(), true);
    }

    //Profilo dell'utente autenticato: lookup per keycloak_id (il "sub" del JWT Keycloak).
    public UserResponse getMe(String keycloakId) {
        return toUserResponse(loadByKeycloakId(keycloakId));
    }

    //Aggiornamento del profilo (solo campi anagrafici: le credenziali sono su Keycloak).
    public UserResponse updateProfile(String keycloakId, UpdateProfileRequest request) {
        User user = loadByKeycloakId(keycloakId);
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPhoneNumber(request.phoneNumber());
        user.setProfileImage(request.profileImage());

        User saved = userRepository.save(user);
        log.info("Profilo aggiornato per keycloak_id={}", keycloakId);
        return toUserResponse(saved);
    }

    //Cambio password: aggiorna solo Keycloak, nessuna modifica sul DB locale.
    public void changePassword(String keycloakId, ChangePasswordRequest request) {
        User user = loadByKeycloakId(keycloakId);
        keycloakAdminClient.changePassword(user.getKeycloakId(), request.newPassword());
    }

    //Eliminazione account: prima il DB locale, poi Keycloak.
    //Se la cancellazione su Keycloak fallisce, l'account resta lì come orfano:
    //verrà riadottato (e sovrascritto) alla prossima registrazione con la stessa email.
    public void deleteMe(String keycloakId) {
        User user = loadByKeycloakId(keycloakId);
        userRepository.delete(user);
        try {
            keycloakAdminClient.deleteUser(user.getKeycloakId());
        } catch (RuntimeException e) {
            log.warn("Account locale eliminato, ma la cancellazione su Keycloak è fallita per {}", keycloakId, e);
        }
        log.info("Account eliminato: keycloak_id={}", keycloakId);
    }

    //Recupero del profilo pubblico di un utente per keycloak_id (il "sub" del JWT Keycloak).
    //Usato dagli altri servizi per risolvere l'utente di una prenotazione/recensione:
    //salvano viaggiatore_id = sub, quindi il lookup per id DB locale non li copre.
    public PublicUserResponse getPublicUserByKeycloakId(String keycloakId) {
        return toPublicUserResponse(loadByKeycloakId(keycloakId));
    }

    //Recupero del profilo pubblico di un utente per id DB.
    public PublicUserResponse getPublicUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato: " + id));
        return toPublicUserResponse(user);
    }

    //Recupero dei profili pubblici per lista di email: usato dagli altri servizi/app
    //per risolvere i nomi degli utenti (es. autore di una recensione).
    public List<PublicUserResponse> lookupByEmails(List<String> emails) {
        //le email arrivano non normalizzate da chi chiama: si filtrano i valori vuoti
        //e si abbassa il case, altrimenti le maiuscole non troverebbero nulla
        List<String> emailNormalizzate = emails.stream()
                .filter(email -> email != null && !email.isBlank())
                .map(email -> email.toLowerCase(Locale.ROOT))
                .toList();
        if (emailNormalizzate.isEmpty()) {
            return List.of();
        }
        return userRepository.findByEmailIn(emailNormalizzate)
                .stream()
                .map(this::toPublicUserResponse)
                .toList();
    }

    private User loadByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utente non trovato con keycloak_id: " + keycloakId));
    }

    //Se il salvataggio su DB fallisce dopo la creazione su Keycloak, elimina l'utente
    //dall'IdP per non lasciare orfani: retry 1x, poi log dell'eventuale residuo.
    private void compensaUtenteOrfano(String keycloakId) {
        //guard: se nel frattempo un'altra richiesta ha salvato la riga per questo account,
        //non è più un orfano — cancellarlo da Keycloak distruggerebbe un utente legittimo
        if (userRepository.existsByKeycloakId(keycloakId)) {
            log.warn("Compensazione saltata: l'utente {} esiste già nel DB locale", keycloakId);
            return;
        }
        try {
            keycloakAdminClient.deleteUser(keycloakId);
            log.info("Compensazione riuscita: utente {} rimosso da Keycloak", keycloakId);
        } catch (RuntimeException e) {
            log.error("Compensazione fallita per {}, retry...", keycloakId, e);
            try {
                keycloakAdminClient.deleteUser(keycloakId);
            } catch (RuntimeException e2) {
                log.error("Utente orfano su Keycloak: {}", keycloakId, e2);
            }
        }
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getDateOfBirth(),
                user.getPhoneNumber(),
                user.getProfileImage());
    }

    private PublicUserResponse toPublicUserResponse(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getProfileImage());
    }
}