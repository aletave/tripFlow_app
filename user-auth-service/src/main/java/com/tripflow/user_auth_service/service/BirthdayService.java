package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.dto.response.BirthdayCouponResponse;
import com.tripflow.user_auth_service.model.User;
import com.tripflow.user_auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

//Job schedulato che, una volta al giorno, invia un augurio di compleanno agli utenti che
//compiono gli anni. Il codice sconto è incluso solo se l'utente ne ha diritto secondo
//BirthdayCouponService (es. non a chi ha inserito la data di nascita da pochi giorni).
@Service
@RequiredArgsConstructor
@Slf4j
public class BirthdayService {

    private final UserRepository userRepository;
    private final MailService mailService;
    private final BirthdayCouponService birthdayCouponService;

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
                BirthdayCouponResponse coupon = birthdayCouponService.verifica(utente);
                mailService.sendBirthdayGreeting(utente.getEmail(), utente.getFirstName(),
                        coupon.eligible() ? coupon.code() : null, coupon.validUntil());
            } catch (RuntimeException e) {
                //un invio fallito non deve bloccare gli altri auguri
                log.error("Invio auguri fallito per utente id={}", utente.getId(), e);
            }
        }
    }
}
