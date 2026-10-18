package com.tripflow.itinerary_service.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripflow.itinerary_service.model.Visibility;

import java.io.IOException;
import java.util.Locale;

// Deserializzatore tollerante per il campo "visibility".
// Accetta indifferentemente:
//  - il booleano di comodo ("visibility": true/false)
//  - l'enum come stringa ("PUBLIC"/"PRIVATE"/"SHARED", anche in minuscolo)
//  - le stringhe "true"/"false"
// In questo modo i client Android che inviano la visibilita' in formati diversi
// non ricevono piu' un 400.
public class VisibilityDeserializer extends JsonDeserializer<Visibility> {

    @Override
    public Visibility deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.asBoolean() ? Visibility.PUBLIC : Visibility.PRIVATE;
        }

        String raw = node.asText().trim();
        if (raw.isEmpty()) {
            return null;
        }

        return switch (raw.toUpperCase(Locale.ROOT)) {
            case "PUBLIC", "TRUE" -> Visibility.PUBLIC;
            case "PRIVATE", "FALSE" -> Visibility.PRIVATE;
            case "SHARED" -> Visibility.SHARED;
            default -> throw context.weirdStringException(raw, Visibility.class,
                    "Valore di visibilita' non valido: attesi PUBLIC, PRIVATE, SHARED o un booleano");
        };
    }
}
