package it.pagopa.pn.portfat.utils;

import it.pagopa.pn.portfat.exception.PnGenericException;
import it.pagopa.pn.portfat.model.PortaleFatturazioneModel;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UtilityTest {

    // Test per il metodo jsonToByteArray
    @Test
    void testJsonToByteArray() {
        // Creiamo un oggetto di esempio
        TestObject testObject = new TestObject();
        testObject.setName("test");
        testObject.setValue(123);
        // Convertilo in byte[]
        byte[] result = Utility.jsonToByteArray(testObject);
        // Verifica che il risultato non sia null e che la lunghezza del byte array sia maggiore di 0
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void testComputeSHA256() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");
        Files.write(tempFile, "test content".getBytes());
        String sha256 = Utility.computeSHA256(tempFile);

        assertNotNull(sha256);
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testComputeSHA256WithException() {
        try {
            Path invalidFilePath = Path.of("invalidPath");
            assertThrows(PnGenericException.class, () -> Utility.computeSHA256(invalidFilePath));
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void testConvertToObjectWithValidJson() {
        String json = "{\"name\":\"test\", \"value\":123}";

        // Converti in oggetto
        TestObject result = Utility.convertToObject(json, TestObject.class);

        assertNotNull(result);
        assertEquals("test", result.getName());
        assertEquals(123, result.getValue());
    }

    @Test
    void testConvertToObjectWithValidFile() throws URISyntaxException {
        File existingFile = new File(Objects.requireNonNull(getClass().getClassLoader().getResource("029dcc19-1d59-481e-9a77-9d67aac31bc0.json"))
                .toURI());
        assertTrue(existingFile.exists(), "File should exist");
        PortaleFatturazioneModel result = Utility.convertToObject(existingFile, PortaleFatturazioneModel.class);

        assertNotNull(result);
        assertEquals("038b356f-eb4f-421b-9aa7-ae5b1cc261a4", result.getFkIdEnte());
        assertEquals("01-2025", result.getPeriodoRiferimento());
    }


    @Test
    void testConvertToObjectWithInvalidJson() {
        String invalidJson = "{\"name\":\"test\", \"value\":\"invalid\"}";

        PnGenericException exception = Assertions.assertThrows(PnGenericException.class,
                () -> Utility.convertToObject(invalidJson, TestObject.class));
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("Failed to map the requested object"));
    }

    @Test
    void testCreateDirectories() throws IOException {
        Path tempDir = Files.createTempDirectory("testDir");
        Mono<Void> result = Utility.createDirectories(tempDir);
        result.block();
        assertTrue(Files.exists(tempDir));
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testDeleteDirectories() throws IOException {
        Path tempDir = Files.createTempDirectory("testDir");
        assertTrue(Files.exists(tempDir));
        Mono<Void> result = Utility.deleteFileOrDirectory(tempDir.toFile());
        result.block();
        assertFalse(Files.exists(tempDir));
    }

    // Classe di supporto per i test
    @Getter
    @Setter
    static class TestObject {
        private String name;
        private int value;
    }
}