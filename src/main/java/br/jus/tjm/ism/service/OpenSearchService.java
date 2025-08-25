package br.jus.tjm.ism.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import br.jus.tjm.ism.dto.SentencaIndex;

@Service
public class OpenSearchService {

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Value("${opensearch.index}")
    private String indexName;

    public OpenSearchService(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public String indexString(String conteudo) {
        try {
            Map<String, Object> doc = Map.of(
                "conteudo", conteudo,
                "timestamp", System.currentTimeMillis()
            );
            String docId = UUID.randomUUID().toString();

            IndexRequest request = new IndexRequest(indexName).id(docId).source(doc);
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            return response.getId();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao indexar no OpenSearch", e);
        }
    }

    public String indexarSentenca(SentencaIndex sentenca) {
        try {
            String docId = sentenca.id_sentenca();

            String json = objectMapper.writeValueAsString(sentenca);

            IndexRequest request = new IndexRequest(indexName)
                    .id(docId)
                    .source(json, XContentType.JSON);

            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            return response.getId();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao indexar SentencaIndex no OpenSearch", e);
        }
    }
}
