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

    /** Elimina il vecchio avatar dal disco se presente e gestito internamente. */
    public void elimina(String profileImage) {
        if (profileImage == null || profileImage.isBlank()) {
            return;
        }
        String marker = "/api/auth/avatars/";
        if (!profileImage.contains(marker)) {
            // URL esterno (es. OAuth/social o placeholder), non eliminare
            return;
        }
        String filename = profileImage.substring(profileImage.indexOf(marker) + marker.length());
        if (filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf("?"));
        }
        Path file = Paths.get(uploadDir).resolve(filename);
        try {
            Files.deleteIfExists(file);
            log.info("Vecchio avatar eliminato: {}", file.toAbsolutePath());
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
