package com.tripflow.itinerary_service.migration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// Guardia sulle migrazioni Flyway: il vincolo chk_stop_type deve ammettere 'CUSTOM'
// (le tappe manuali inviate dall'app) e chk_stop_reference deve accettare una tappa
// CUSTOM senza riferimenti a viaggio/attivita'. Senza un DB reale nei test, verifichiamo
// che gli script di migrazione contengano queste regole.
class StopTypeMigrationTest {

    @Test
    void scriptDiMigrazione_includonoCustom() {
        String sql = leggiTutteLeMigrazioni();

        assertThat(sql)
                .as("il vincolo chk_stop_type deve ammettere CUSTOM")
                .contains("chk_stop_type")
                .contains("'CUSTOM'");

        assertThat(sql)
                .as("chk_stop_reference deve accettare una tappa CUSTOM senza riferimenti")
                .contains("chk_stop_reference")
                .contains("stop_type = 'CUSTOM' AND viaggio_id IS NULL AND attivita_id IS NULL");
    }

    @Test
    void scriptDiMigrazione_includonoIndiciSuiRiferimentiDelleTappe() {
        String sql = leggiTutteLeMigrazioni();

        assertThat(sql)
                .contains("idx_itinerary_stops_viaggio_id")
                .contains("ON itinerary_stops (viaggio_id)")
                .contains("idx_itinerary_stops_attivita_id")
                .contains("ON itinerary_stops (attivita_id)");
    }

    private String leggiTutteLeMigrazioni() {
        try {
            Path cartella = Paths.get(getClass().getClassLoader()
                    .getResource("db/migration")
                    .toURI());

            try (Stream<Path> file = Files.list(cartella)) {
                List<Path> migrazioni = file
                        .filter(p -> p.getFileName().toString().endsWith(".sql"))
                        .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                        .toList();

                StringBuilder sql = new StringBuilder();
                for (Path migrazione : migrazioni) {
                    sql.append(Files.readString(migrazione));
                    sql.append(System.lineSeparator());
                }
                return sql.toString();
            }
        } catch (Exception e) {
            throw new UncheckedIOException(new IOException("Impossibile leggere le migrazioni", e));
        }
    }
}
