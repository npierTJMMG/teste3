package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Documentos(
        String id,
        String nivelSigilo,
        String dataHoraJuntada,
        TipoDocumento tipo
) {}
