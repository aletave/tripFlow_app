package com.tripflow.user_auth_service.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageConfigTest {

    private static final String NOME_VALIDO = "123e4567-e89b-12d3-a456-426614174000.jpg";

    @TempDir
    Path radice;

    private Path cartellaAvatar;
    private AvatarStorageConfig storage;

    @BeforeEach
    void setUp() throws IOException {
        cartellaAvatar = Files.createDirectories(radice.resolve("avatars"));
        storage = new AvatarStorageConfig();
        ReflectionTestUtils.setField(storage, "uploadDir", cartellaAvatar.toString());
    }

    @Test
    void elimina_avatarGenerato_vieneCancellato() throws IOException {
        Path avatar = Files.createFile(cartellaAvatar.resolve(NOME_VALIDO));

        storage.elimina("/user-auth-service/api/auth/avatars/" + NOME_VALIDO);

        assertFalse(Files.exists(avatar));
    }

    @Test
    void elimina_pathTraversal_nonCancellaFileFuoriDallaCartella() throws IOException {
        Path segreto = Files.createFile(radice.resolve("segreto.txt"));

        storage.elimina("/api/auth/avatars/../segreto.txt");

        assertTrue(Files.exists(segreto));
    }

    @Test
    void percorsoAvatar_rifiutaNomiNonGenerati() {
        assertNull(storage.percorsoAvatar(".."));
        assertNull(storage.percorsoAvatar("../segreto.txt"));
        assertNull(storage.percorsoAvatar("avatar.exe"));
        assertEquals(cartellaAvatar.toAbsolutePath().normalize().resolve(NOME_VALIDO),
                storage.percorsoAvatar(NOME_VALIDO));
    }
}
