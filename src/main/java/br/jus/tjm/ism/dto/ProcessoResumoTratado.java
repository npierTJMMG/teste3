package br.jus.tjm.ism.dto;
import java.util.List;

public record ProcessoResumoTratado(
    String des_tribunal,
    String num_processo,
    Integer id_classe,
    String des_classe,
    Integer id_assunto_principal,
    String des_assunto_principal,
    List<Integer> ids_assuntos,
    List<String> des_assuntos,
    Long id_orgao,
    String des_orgao
) {}
