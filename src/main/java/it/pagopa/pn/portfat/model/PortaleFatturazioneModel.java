package it.pagopa.pn.portfat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortaleFatturazioneModel {

    @JsonProperty("idEnte")
    private String fkIdEnte;

    @JsonProperty("periodo_riferimento")
    private String periodoRiferimento;


}
