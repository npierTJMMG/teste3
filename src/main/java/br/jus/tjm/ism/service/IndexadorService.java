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
import org.springframework.stereotype.Service;

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

    public void indexarTribunaisMilitaresDiario(String tribunal){
        LocalDate ontem = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Concatena o sufixo para setar o horário
        String ontemStr = ontem.format(formatter) + "T01%3A17%3A00.000";

    }
}
