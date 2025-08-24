package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListaProcessosResponse(
        int total,
        int numberOfElements,
        int maxElementsSize,
        List<Object> searchAfter,
        List<ProcessoResumo> content
) {}
