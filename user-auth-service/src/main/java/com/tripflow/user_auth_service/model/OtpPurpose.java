package com.tripflow.user_auth_service.model;

//Scopo di un codice OTP inviato via email. Determina quale azione viene sbloccata
//una volta che il codice è stato verificato con successo.
public enum OtpPurpose {
    //Verifica dell'indirizzo email in fase di registrazione.
    EMAIL_VERIFICATION,
    //Reset della password dimenticata.
    PASSWORD_RESET
}
