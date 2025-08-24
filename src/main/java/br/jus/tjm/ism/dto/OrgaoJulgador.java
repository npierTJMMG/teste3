package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrgaoJulgador(
        Long id,
        Long idLocal,
        String nome
) {}
