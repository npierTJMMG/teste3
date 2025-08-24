package br.jus.tjm.ism.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProcessoResumo(
        String id,
        String numeroProcesso,
        int nivelSigilo,
        List<Tramitacao> tramitacoes,
        OrgaoJulgador orgaoJulgador
) {}
