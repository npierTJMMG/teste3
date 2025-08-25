package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListaSentencas(
        List<Documentos> sentencasPublicas,
        List<Documentos> sentencasSigilosas
) {
    public static ListaSentencas vazia() {
        return new ListaSentencas(new ArrayList<>(), new ArrayList<>());
    }
}
