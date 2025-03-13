package it.pagopa.pn.portfat.middleware.db.dao.impl;

import it.pagopa.pn.portfat.config.BaseTest;
import it.pagopa.pn.portfat.middleware.db.dao.PortFatDownloadDAO;
import it.pagopa.pn.portfat.middleware.db.entities.PortFatDownload;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class PortFatDownloadDAOImplTestIT extends BaseTest.WithLocalStack {

    @SpyBean
    private PortFatDownloadDAO portFatDownloadDAO;

    @Test
    void createAndFindByDownloadId() {
        // Crea un oggetto PortFatDownload di test
        PortFatDownload download = new PortFatDownload();
        download.setDownloadId("test-download-id");
        download.setFileVersion("v1.1");

        // Salva nel database
        StepVerifier.create(portFatDownloadDAO.createPortFatDownload(download))
                .expectNext(download)
                .verifyComplete();

        // Recupera l'elemento e verifica che sia corretto
        StepVerifier.create(portFatDownloadDAO.findByDownloadId("test-download-id"))
                .assertNext(found -> {
                    assertEquals("test-download-id", found.getDownloadId());
                    assertEquals("v1.1", found.getFileVersion());
                })
                .verifyComplete();
    }

    @Test
    void updatePortFatDownload() {

        // Crea e salva un oggetto
        PortFatDownload download = new PortFatDownload();
        download.setDownloadId("update-test-id");
        download.setFileVersion("v1.1");

        StepVerifier.create(portFatDownloadDAO.createPortFatDownload(download))
                .expectNext(download)
                .verifyComplete();

        // Modifica il fileName e aggiorna
        download.setFileVersion("v2.2");

        StepVerifier.create(portFatDownloadDAO.updatePortFatDownload(download))
                .expectNext(download)
                .verifyComplete();

        // Recupera e verifica l'aggiornamento
        StepVerifier.create(portFatDownloadDAO.findByDownloadId("update-test-id"))
                .assertNext(found -> assertEquals("v2.2", found.getFileVersion()))
                .verifyComplete();
    }

}