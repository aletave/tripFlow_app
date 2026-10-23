package com.tripflow.user_auth_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

//Invio delle email applicative tramite il server SMTP configurato (Mailpit in sviluppo).
//A differenza delle mail gestite da Keycloak, qui controlliamo noi contenuto e template:
//serve per i codici OTP (verifica/reset) e per l'augurio di compleanno con codice sconto.
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@tripflow.com}")
    private String from;

    @Value("${app.mail.from-name:TripFlow}")
    private String fromName;

    //Codice OTP per verificare l'indirizzo email in fase di registrazione.
    public void sendVerificationOtp(String to, String firstName, String code) {
        String corpo = baseLayout(
                "Benvenuto su TripFlow! 🧳",
                saluto(firstName) +
                        "<p>Per completare la registrazione inserisci questo codice di verifica nell'app:</p>" +
                        boxCodice(code) +
                        "<p>Il codice scade tra pochi minuti. Se non hai creato un account TripFlow, ignora questa email.</p>"
        );
        invia(to, "Il tuo codice di verifica TripFlow", corpo);
    }

    //Codice OTP per reimpostare la password dimenticata.
    public void sendPasswordResetOtp(String to, String firstName, String code) {
        String corpo = baseLayout(
                "Reimposta la tua password",
                saluto(firstName) +
                        "<p>Abbiamo ricevuto una richiesta di reset della password. Inserisci questo codice nell'app per procedere:</p>" +
                        boxCodice(code) +
                        "<p>Il codice scade tra pochi minuti. Se non hai richiesto il reset, puoi ignorare questa email: la tua password resta invariata.</p>"
        );
        invia(to, "Codice per reimpostare la password TripFlow", corpo);
    }

    //Augurio di compleanno con codice sconto del 15% (valido 2 settimane).
    public void sendBirthdayGreeting(String to, String firstName, String discountCode) {
        String corpo = baseLayout(
                "Tanti auguri di buon compleanno! 🎉",
                saluto(firstName) +
                        "<p>Tutto il team di TripFlow ti augura una splendida giornata.</p>" +
                        "<p>Per festeggiare, ecco un <strong>codice sconto del 15%</strong> sul tuo prossimo viaggio:</p>" +
                        boxCodice(discountCode) +
                        "<p>È valido <strong>2 settimane</strong>: il mondo ti aspetta. ✈️</p>"
        );
        invia(to, "🎂 Un regalo di compleanno da TripFlow", corpo);
    }

    private void invia(String to, String oggetto, String corpoHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(from, fromName);
            helper.setTo(to);
            helper.setSubject(oggetto);
            helper.setText(corpoHtml, true);
            mailSender.send(message);
            log.info("Email inviata a {}: {}", to, oggetto);
        } catch (MailException | jakarta.mail.MessagingException | java.io.UnsupportedEncodingException e) {
            //non rilanciamo: l'invio email non deve far fallire l'operazione applicativa
            //(registrazione, reset, job compleanni). Il chiamante decide se compensare.
            log.error("Invio email fallito verso {} ({})", to, oggetto, e);
            throw new MailDeliveryException("Invio email non riuscito", e);
        }
    }

    private String saluto(String firstName) {
        String nome = (firstName != null && !firstName.isBlank()) ? firstName : "viaggiatore";
        return "<p>Ciao <strong>" + escape(nome) + "</strong>,</p>";
    }

    private String boxCodice(String code) {
        return "<div style=\"margin:24px 0;text-align:center;\">" +
                "<span style=\"display:inline-block;font-size:28px;font-weight:bold;letter-spacing:6px;" +
                "padding:16px 28px;background:#f2f4f7;border-radius:12px;color:#101828;\">" +
                escape(code) + "</span></div>";
    }

    private String baseLayout(String titolo, String contenuto) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:0 auto;color:#344054;\">" +
                "<h2 style=\"color:#101828;\">" + escape(titolo) + "</h2>" +
                contenuto +
                "<hr style=\"border:none;border-top:1px solid #eaecf0;margin:24px 0;\"/>" +
                "<p style=\"font-size:12px;color:#98a2b3;\">TripFlow — questa è un'email automatica, non rispondere.</p>" +
                "</div>";
    }

    //Escape minimale per i valori interpolati nell'HTML (nome utente).
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    //Eccezione interna per segnalare un invio fallito a chi vuole gestirlo (es. endpoint sincrono di reinvio).
    public static class MailDeliveryException extends RuntimeException {
        public MailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
