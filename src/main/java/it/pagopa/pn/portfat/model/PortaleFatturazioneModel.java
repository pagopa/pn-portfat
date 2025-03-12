package it.pagopa.pn.portfat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortaleFatturazioneModel {

    @JsonProperty("FKIdEnte")
    private String fkIdEnte;
    @JsonProperty("AnnoValidita")
    private Integer annoValidita;
    @JsonProperty("MeseValidita")
    private Integer meseValidita;
    @JsonProperty("TotaleAnalogico")
    private BigDecimal totaleAnalogico;
    @JsonProperty("TotaleDigitale")
    private BigDecimal totaleDigitale;
    @JsonProperty("TotaleNotificheAnalogico")
    private Integer totaleNotificheAnalogico;
    @JsonProperty("TotaleNotificheDigitale")
    private Integer totaleNotificheDigitale;
    @JsonProperty("Totale")
    private BigDecimal totale;
    @JsonProperty("TotaleNotifiche")
    private Integer totaleNotifiche;
    @JsonProperty("IdTipoContratto")
    private Integer idTipoContratto;
    @JsonProperty("PercentualeCategoriaA")
    private Integer percentualeCategoriaA;
    @JsonProperty("PercentualeCategoriaD")
    private Integer percentualeCategoriaD;
    @JsonProperty("FkIdStato")
    private String fkIdStato;
    @JsonProperty("Fatturabile")
    private Boolean fatturabile;
    @JsonProperty("RagioneSociale")
    private String ragioneSociale;
    @JsonProperty("CodiceFiscale")
    private String codiceFiscale;
    @JsonProperty("TipoContratto")
    private String tipoContratto;
    @JsonProperty("Asseverazione")
    private Boolean asseverazione;
    @JsonProperty("DataUscitaAsseverazione")
    private String dataUscitaAsseverazione;

}
