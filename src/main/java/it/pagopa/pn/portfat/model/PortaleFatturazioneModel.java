package it.pagopa.pn.portfat.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortaleFatturazioneModel {

    @JsonProperty("idEnte")
    private String idEnte;

    @JsonProperty("contractId")
    private String contractId;

    @JsonProperty("periodo_riferimento")
    private String periodoRiferimento;

    @JsonProperty("last_update")
    private String lastUpdate;

    @JsonProperty("prodotti")
    private List<Prodotto> prodotti;


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Prodotto {

        @JsonProperty("id")
        private String id;

        @JsonProperty("nome")
        private String nome;

        @JsonProperty("valore_totale")
        private double valoreTotale;

        @JsonProperty("varianti")
        private List<Variante> varianti;


        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Variante {

            @JsonProperty("codice")
            private String codice;

            @JsonProperty("nome")
            private String nome;

            @JsonProperty("valore_totale")
            private double valoreTotale;

            @JsonProperty("distribuzione")
            private Distribuzione distribuzione;

        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Distribuzione {

            @JsonProperty("regionale")
            private List<Object> regionale;

        }
    }
}

