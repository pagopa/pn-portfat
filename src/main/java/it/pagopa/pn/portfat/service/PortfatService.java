package it.pagopa.pn.portfat.service;

import org.springframework.http.HttpMethod;

public interface PortfatService {

    void processZipFile(String url, HttpMethod method, String outputDir, String fileName);
}
