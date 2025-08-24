package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Assunto(
        Integer codigo,
        String descricao,
        List<Assunto> outrosAssuntos
) {}
