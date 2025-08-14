package br.jus.tjm.bsm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IndexadorRequest {

    @JsonProperty("tribunal")
    private String tribunal;

    @JsonProperty("dataHoraAtualizacaoInicio")
    private String dataHoraAtualizacaoInicio;

    @JsonProperty("dataHoraAtualizacaoFim")
    private String dataHoraAtualizacaoFim;
    

    // Getters e Setters
    public String getTribunal() { return tribunal; }
    public void setTribunal(String tribunal) { this.tribunal = tribunal; }

    public String getDataHoraAtualizacaoInicio() { return dataHoraAtualizacaoInicio; }
    public void setDataHoraAtualizacaoInicio(String dataHoraAtualizacaoInicio) { this.dataHoraAtualizacaoInicio = dataHoraAtualizacaoInicio; }

    public String getDataHoraAtualizacaoFim() { return dataHoraAtualizacaoFim; }
    public void setDataHoraAtualizacaoFim(String dataHoraAtualizacaoFim) { this.dataHoraAtualizacaoFim = dataHoraAtualizacaoFim; }
}
