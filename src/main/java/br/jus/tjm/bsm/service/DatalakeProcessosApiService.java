package br.jus.tjm.bsm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DatalakeProcessosApiService {

    @Value("${datalake-processos-api.base-url}")
    private String apiBaseUrl;

    private final KeycloakTokenService keycloakTokenService;

    public DatalakeProcessosApiService(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    public ResponseEntity<byte[]> getListaProcessos(
        String tribunal,
        String dataHoraAtualizacaoInicio,
        String dataHoraAtualizacaoFim
    ) {
        String url = String.format(
            "%s?instancia=PRIMEIRO_GRAU&tribunal=%s&dataHoraAtualizacaoInicio=%s&dataHoraAtualizacaoFim=%s", 
            apiBaseUrl, tribunal, dataHoraAtualizacaoInicio, dataHoraAtualizacaoFim
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + keycloakTokenService.getAccessToken());
        
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(url, HttpMethod.GET, entity, byte[].class);
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
