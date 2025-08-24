package br.jus.tjm.ism.service;

import java.net.URI;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import br.jus.tjm.ism.dto.ListaProcessosResponse;

@Service
public class DatalakeProcessosApiService {

    @Value("${datalake-processos-api.base-url}")
    private String apiBaseUrl;

    private final KeycloakTokenService keycloakTokenService;

    public DatalakeProcessosApiService(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    public ListaProcessosResponse getListaProcessosAtualizados(
        String tribunal,
        String dataHoraAtualizacaoInicio,
        @Nullable String dataHoraAtualizacaoFim
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(apiBaseUrl)
                .queryParam("instancia", "PRIMEIRO_GRAU")
                .queryParam("tribunal", tribunal)
                .queryParam("dataHoraAtualizacaoInicio", dataHoraAtualizacaoInicio);

        if (StringUtils.hasText(dataHoraAtualizacaoFim)) {
            builder.queryParam("dataHoraAtualizacaoFim", dataHoraAtualizacaoFim);
        }

        // true => preserva valores já codificados (evita double-encoding)
        URI uri = builder.build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(keycloakTokenService.getAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ListaProcessosResponse> resp = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    ListaProcessosResponse.class
            );

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Chamada retornou status " + resp.getStatusCodeValue());
            }

            return Objects.requireNonNull(resp.getBody(), "Resposta sem corpo");
        } catch (RestClientException e) {
            // Ajuste para seu logger/exception handler
            throw new IllegalStateException("Falha ao consultar lista de processos", e);
        }
    }

    public ResponseEntity<byte[]> getListaDocumentos(
        String numProcesso
    ) {
        String url = String.format(
            "%s/%s/documentos", 
            apiBaseUrl, numProcesso
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keycloakTokenService.getAccessToken());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
    }

    public ResponseEntity<byte[]> getDocumentoTexto(
        String numProcesso,
        String idDocumento
    ) {
        String url = String.format(
            "%s/%s/documentos/%s/texto", 
            apiBaseUrl, numProcesso,idDocumento
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keycloakTokenService.getAccessToken());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
    }
}
