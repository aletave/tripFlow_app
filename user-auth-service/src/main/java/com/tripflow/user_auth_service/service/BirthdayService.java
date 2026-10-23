package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Job schedulato che, una volta al giorno, invia un augurio di compleanno con un codice
//sconto del 10% agli utenti che compiono gli anni. Nella versione attuale il codice è
//puramente dimostrativo: non viene salvato né è riscattabile al checkout (l'integrazione
//con booking-service/Stripe sarebbe un passo successivo).
@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayService {

    private final UserRepository userRepository;
    private final MailService mailService;

    private static final char[] ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private final SecureRandom secureRandom = new SecureRandom();

    //Orologio iniettabile: in produzione è quello di sistema, nei test viene fissato
    //per verificare la gestione del 29 febbraio.
    private Clock clock = Clock.systemDefaultZone();

    //Cron configurabile: default ogni giorno alle 08:00 (ora del server).
    @Scheduled(cron = "${app.birthday.cron:0 0 8 * * *}", zone = "${app.birthday.zone:Europe/Rome}")
    public void inviaAuguriDiCompleanno() {
        LocalDate oggi = LocalDate.now(clock);
        List<User> festeggiati = new ArrayList<>(
                userRepository.findByBirthday(oggi.getMonthValue(), oggi.getDayOfMonth()));

        //chi è nato il 29 febbraio festeggia il 28 negli anni non bisestili
        if (oggi.getMonthValue() == 2 && oggi.getDayOfMonth() == 28 && !oggi.isLeapYear()) {
            festeggiati.addAll(userRepository.findByBirthday(2, 29));
        }

        if (festeggiati.isEmpty()) {
            log.debug("Nessun compleanno oggi ({}/{})", oggi.getDayOfMonth(), oggi.getMonthValue());
            return;
        }

        log.info("Invio auguri di compleanno a {} utente/i", festeggiati.size());
        for (User utente : festeggiati) {
            try {
                mailService.sendBirthdayGreeting(utente.getEmail(), utente.getFirstName(), generaCodiceSconto());
            } catch (RuntimeException e) {
                //un invio fallito non deve bloccare gli altri auguri
                log.error("Invio auguri fallito per utente id={}", utente.getId(), e);
            }
        }
    }

    //Codice sconto leggibile del tipo BDAY15-XXXXXX (dimostrativo, non persistito).
    //Verrà sostituito dal Promotion Code reale di Stripe una volta deciso il flusso con booking.
    private String generaCodiceSconto() {
        StringBuilder sb = new StringBuilder("BDAY15-");
        for (int i = 0; i < 6; i++) {
            sb.append(ALFABETO[secureRandom.nextInt(ALFABETO.length)]);
        }
        return sb.toString();
    }
}
