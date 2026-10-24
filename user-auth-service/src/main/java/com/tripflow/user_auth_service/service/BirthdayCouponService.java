package com.tripflow.user_auth_service.service;

import com.tripflow.user_auth_service.dto.response.BirthdayCouponResponse;
import com.tripflow.user_auth_service.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;

//Regole del codice sconto di compleanno. Il codice è uguale per tutti, quindi il diritto
//a usarlo dipende dall'utente:
// - vale dal giorno del compleanno ai `validity-days` giorni successivi;
// - la data di nascita deve essere stata inserita almeno `min-days-before-birthday` giorni
//   prima del compleanno: chi crea un account dichiarando di essere nato oggi non ottiene
//   nulla fino al compleanno dell'anno successivo.
//La data di nascita non è modificabile (vedi AuthService), quindi le regole non si aggirano
//cambiandola. Il limite di un utilizzo per compleanno lo applica booking-service, che conosce
//i pagamenti, usando validFrom/validUntil della risposta.
@Service
public class BirthdayCouponService {

    public static final String NO_BIRTH_DATE = "NO_BIRTH_DATE";
    public static final String OUTSIDE_BIRTHDAY_WINDOW = "OUTSIDE_BIRTHDAY_WINDOW";
    public static final String BIRTH_DATE_TOO_RECENT = "BIRTH_DATE_TOO_RECENT";

    @Value("${app.birthday.promo-code:BDAY15}")
    private String codice = "BDAY15";

    @Value("${app.birthday.validity-days:7}")
    private int giorniValidita = 7;

    @Value("${app.birthday.min-days-before-birthday:30}")
    private int giorniMinimiPrima = 30;

    //Stesso fuso del job degli auguri: nei container il fuso di sistema è UTC, e tra mezzanotte
    //e le 2 italiane "oggi" sarebbe ancora il giorno prima.
    @Value("${app.birthday.zone:Europe/Rome}")
    private String fuso = "Europe/Rome";

    //Orologio iniettabile: nei test viene fissato per simulare i diversi giorni.
    private Clock clock = Clock.systemUTC();

    public BirthdayCouponResponse verifica(User user) {
        LocalDate oggi = LocalDate.now(clock.withZone(ZoneId.of(fuso)));
        if (user.getDateOfBirth() == null || user.getDateOfBirthSetAt() == null) {
            return rifiuto(null, null, NO_BIRTH_DATE,
                    "Aggiungi la data di nascita al profilo per ricevere il codice di compleanno");
        }

        LocalDate compleanno = compleannoPiuRecente(user.getDateOfBirth(), oggi);
        LocalDate scadenza = compleanno.plusDays(giorniValidita);
        if (oggi.isAfter(scadenza)) {
            LocalDate prossimo = compleannoNellAnno(user.getDateOfBirth(), compleanno.getYear() + 1);
            return rifiuto(prossimo, prossimo.plusDays(giorniValidita), OUTSIDE_BIRTHDAY_WINDOW,
                    "Il codice di compleanno vale solo nei " + giorniValidita + " giorni dopo il tuo compleanno");
        }

        LocalDate impostataIl = user.getDateOfBirthSetAt().toLocalDate();
        if (impostataIl.isAfter(compleanno.minusDays(giorniMinimiPrima))) {
            LocalDate prossimo = compleannoNellAnno(user.getDateOfBirth(), compleanno.getYear() + 1);
            return rifiuto(prossimo, prossimo.plusDays(giorniValidita), BIRTH_DATE_TOO_RECENT,
                    "Il codice di compleanno sarà disponibile dal tuo prossimo compleanno");
        }

        return new BirthdayCouponResponse(true, codice, compleanno, scadenza, null,
                "Buon compleanno! Il codice è valido fino al " + scadenza);
    }

    //Compleanno più recente non successivo a oggi. MonthDay.atYear porta il 29 febbraio
    //al 28 negli anni non bisestili.
    static LocalDate compleannoPiuRecente(LocalDate nascita, LocalDate oggi) {
        LocalDate questAnno = compleannoNellAnno(nascita, oggi.getYear());
        return questAnno.isAfter(oggi) ? compleannoNellAnno(nascita, oggi.getYear() - 1) : questAnno;
    }

    static LocalDate compleannoNellAnno(LocalDate nascita, int anno) {
        return MonthDay.from(nascita).atYear(anno);
    }

    private BirthdayCouponResponse rifiuto(LocalDate dal, LocalDate al, String motivo, String messaggio) {
        return new BirthdayCouponResponse(false, null, dal, al, motivo, messaggio);
    }
}
