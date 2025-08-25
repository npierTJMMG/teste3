package br.jus.tjm.ism.service;

import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import br.jus.tjm.ism.dto.ListaProcessosResponse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.io.IOException;
import java.util.*;

@Service
public class IndexadorService {

    private final RestHighLevelClient client;
    private final DatalakeProcessosApiService datalakeProcessosApiService;

    public IndexadorService(
        RestHighLevelClient client,
        DatalakeProcessosApiService datalakeProcessosApiService
    ) {
        this.client = client;
        this.datalakeProcessosApiService = datalakeProcessosApiService;
    }

    public ResponseEntity<ListaProcessosResponse> indexarTribunaisMilitaresDiario(String tribunal){
        try {
            LocalDate ontem = LocalDate.now().minusDays(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            String inicio = ontem.format(fmt) + "T01%3A17%3A00.000";

            ListaProcessosResponse resposta = datalakeProcessosApiService.indexarSentencasProcessosAtualizados(
                tribunal, 
                "2020-08-23T01%3A17%3A00.000",
                 null
            );

            return ResponseEntity.ok(resposta); // HTTP 200 + body
        } catch (Exception e) {
            // Logar exceção se quiser
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
