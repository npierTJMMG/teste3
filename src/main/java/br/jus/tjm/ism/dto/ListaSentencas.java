package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ListaSentencas(
        Documentos sentencaPublica,
        Documentos sentencaSigilosa
) {
    public static ListaSentencas vazia() {
        return new ListaSentencas(new Documentos(), new Documentos());
    }
}
