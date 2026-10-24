package com.tripflow.user_auth_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gestisce il salvataggio delle immagini profilo su disco locale.
 * Il path di upload è configurabile via app.upload.dir (default: ./uploads/avatars).
 * Le immagini vengono servite dal controller come risorsa statica via /api/auth/avatars/{filename}.
 */
@Configuration
@Slf4j
public class AvatarStorageConfig {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final String MARKER = "/api/auth/avatars/";
    //solo i nomi generati da salva(): niente "..", separatori o file estranei alla cartella
    private static final Pattern NOME_AVATAR = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)$");

    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Salva il file sul disco, restituisce il filename generato (UUID + estensione).
     * Lancia IllegalArgumentException se il tipo o la dimensione non sono validi.
     */
    public String salva(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo file non supportato. Usa JPEG, PNG o WebP.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File troppo grande. Dimensione massima: 5 MB.");
        }

        String estensione = estrai(contentType);
        String filename = UUID.randomUUID() + "." + estensione;

        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);

        Path destinazione = dir.resolve(filename);
        Files.copy(file.getInputStream(), destinazione, StandardCopyOption.REPLACE_EXISTING);

        log.info("Avatar salvato: {}", destinazione.toAbsolutePath());
        return filename;
    }

    /**
     * Percorso su disco di un avatar, o null se il nome non è uno di quelli generati da salva()
     * o se risolto finirebbe fuori dalla cartella di upload.
     */
    public Path percorsoAvatar(String filename) {
        if (filename == null || !NOME_AVATAR.matcher(filename).matches()) {
            return null;
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        return file.startsWith(dir) ? file : null;
    }

    /** Elimina il vecchio avatar dal disco se presente e gestito internamente. */
    public void elimina(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            return;
        }
        if (!profileImage.contains(MARKER)) {
            // URL esterno (es. OAuth/social o placeholder), non eliminare
            return;
        }
        String filename = profileImage.substring(profileImage.indexOf(MARKER) + MARKER.length());
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf("?"));
        }
        Path file = percorsoAvatar(filename);
        if (file == null) {
            log.warn("Avatar con nome non valido, eliminazione ignorata: {}", profileImage);
            return;
        }
        try {
            Files.deleteIfExists(file);
            log.info("Vecchio avatar eliminato: {}", file);
        } catch (IOException e) {
            log.warn("Impossibile eliminare il vecchio avatar: {}", file, e);
        }
    }

    private String estrai(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png"  -> "png";
            case "image/webp" -> "webp";
            default           -> "jpg";
        };
    }
}
