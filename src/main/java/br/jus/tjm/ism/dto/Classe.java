package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Classe(
        Integer codigo,
        String descricao,
        String hierarquia
) {}
