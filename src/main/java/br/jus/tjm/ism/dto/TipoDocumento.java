package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TipoDocumento(
        int codigo,
        String nome,
        long idCodex,
        String idOrigem
) {}
