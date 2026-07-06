package com.tripflow.booking.data.dto.requests;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrenotazioneRequest {


    @NotNull(message = "L'ID del viaggio è obbligatorio")
    private UUID viaggioId;

    @NotNull(message = "Specificare il numero di partecipanti")
    @Min(value = 1, message = "Deve esserci almeno un partecipante")
    //per ora 50 come placeholder, da rivedere
    @Max(value = 50, message = "Massimo 50 partecipanti per singola prenotazione")
    private Integer numeroPartecipanti;

    @Size(max = 1000, message = "Le note non possono superare i 1000 caratteri")
    private String note;

    //Lista di ID delle attività aggiuntive
    private List<UUID> attivitaIds;
}
