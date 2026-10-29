package com.tripflow.itinerary_service.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.tripflow.itinerary_service.model.Visibility;

import java.io.IOException;
import java.util.Locale;

/**
 * Lettura tollerante del campo JSON {@code visibility}.
 *
 * <p>Accetta:
 * <ul>
 *   <li>l'enum come stringa: {@code "PUBLIC"}, {@code "PRIVATE"}, {@code "SHARED"}
 *       (anche in minuscolo);</li>
 *   <li>un booleano o le stringhe {@code "true"}/{@code "false"}
 *       ({@code true} = PUBLIC, {@code false} = PRIVATE).</li>
 * </ul>
 * Qualsiasi altro valore produce un 400.
 */
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
