package it.pagopa.pn.portfat.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileReadyModel {

    private String downloadUrl;
    private String fileVersion;
    private String filePath;
}
