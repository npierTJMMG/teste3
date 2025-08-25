package br.jus.tjm.ism.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import br.jus.tjm.ism.dto.Assunto;
import br.jus.tjm.ism.dto.Documentos;
import br.jus.tjm.ism.dto.ListaDocumentosResponse;
import br.jus.tjm.ism.dto.ListaProcessosResponse;
import br.jus.tjm.ism.dto.ListaSentencas;
import br.jus.tjm.ism.dto.ProcessoResumo;
import br.jus.tjm.ism.dto.ProcessoResumoTratado;

@Service
public class DatalakeProcessosApiService {

    @Value("${datalake-processos-api.base-url}")
    private String apiBaseUrl;

    private final KeycloakTokenService keycloakTokenService;

    public DatalakeProcessosApiService(KeycloakTokenService keycloakTokenService) {
        this.keycloakTokenService = keycloakTokenService;
    }

    public ListaProcessosResponse indexarSentencasProcessosAtualizados(
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

        URI uri = builder.build(true).toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(keycloakTokenService.getAccessToken());

        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ListaProcessosResponse> respListaProcessos = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    entity,
                    ListaProcessosResponse.class
            );

            if (!respListaProcessos.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Chamada retornou status " + respListaProcessos.getStatusCodeValue());
            }

            ListaProcessosResponse bodyListaProcessos = respListaProcessos.getBody();
            List<ProcessoResumo> listaProcessos = bodyListaProcessos.content();

            int total = bodyListaProcessos.total();
            int maxElementsSize = bodyListaProcessos.maxElementsSize();
            int loops = (int) Math.ceil((double) total / maxElementsSize);

            this.handleSentencas(listaProcessos);

            return Objects.requireNonNull(respListaProcessos.getBody(), "Resposta sem corpo");
        } catch (RestClientException e) {
            throw new IllegalStateException("Falha ao consultar lista de processos", e);
        }
    }

    private void handleSentencas(
        List<ProcessoResumo> listaProcessos
    ) {
        for (ProcessoResumo processo : listaProcessos) {
            String id = processo.id();

            ResponseEntity<ListaDocumentosResponse> respListaDocumentos = this.getListaDocumentos(id);

            if (!respListaDocumentos.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Chamada retornou status " + respListaDocumentos.getStatusCodeValue());
            }

            ListaDocumentosResponse bodyListaDocumentos = respListaDocumentos.getBody();            
            List<Documentos> listaDocumentos = bodyListaDocumentos.documentos();

            ListaSentencas listaSentencas = this.filtrarSentencas(listaDocumentos);
            List<Documentos> publicas = listaSentencas.sentencasPublicas();
            List<Documentos> sigilosas = listaSentencas.sentencasSigilosas();

            ProcessoResumoTratado processoToIndex = this.getDadosProcessoIndexacao(processo);

            if (publicas != null && !publicas.isEmpty()) {
                this.indexarSentencasPublicas(processoToIndex,publicas);
            }

            if (sigilosas != null && !sigilosas.isEmpty()) {
                Integer as =1;
            }
        }
    }

    private ResponseEntity<ListaDocumentosResponse> getListaDocumentos(
        String numProcesso
    ) {
        String url = String.format(
            "%s/%s/documentos", 
            apiBaseUrl, numProcesso
        );

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(keycloakTokenService.getAccessToken());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ListaDocumentosResponse.class
        );
    }

    private ListaSentencas filtrarSentencas(
        List<Documentos> listaDocumentos
    ) {
        Set<Integer> codigosAceitos = Set.of(550, 795, 796); //Sentença - Sentença (Outras) - Sentença Normativa
        
        ListaSentencas listaSentencas = new ListaSentencas(new ArrayList<>(), new ArrayList<>());

        for (Documentos documento : listaDocumentos) {
            if (documento != null && documento.tipo() != null && codigosAceitos.contains(documento.tipo().codigo())) {

                if (documento.nivelSigilo().equals("PUBLICO")){
                    listaSentencas.sentencasPublicas().add(documento);
                } else {                    
                    listaSentencas.sentencasSigilosas().add(documento);
                }
            }
        }

        return listaSentencas;
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

    private ProcessoResumoTratado getDadosProcessoIndexacao(ProcessoResumo processo) {
        String desTribunal = processo.siglaTribunal(); 
        String numProcesso = processo.numeroProcesso();

        var t0 = (processo.tramitacoes() != null && !processo.tramitacoes().isEmpty())
                ? processo.tramitacoes().get(0) : null;

        Integer idClasse = null;
        String desClasse = null;
        if (t0 != null && t0.classe() != null && !t0.classe().isEmpty()) {
            var c0 = t0.classe().get(0);
            if (c0 != null) {
                idClasse = c0.codigo();
                desClasse = c0.descricao();
            }
        }

        Integer idAssuntoPrincipal = null;
        String desAssuntoPrincipal = null;
        List<Integer> idsAssuntos = new ArrayList<>();
        List<String>  desAssuntos = new ArrayList<>();
        if (t0 != null && t0.assunto() != null && !t0.assunto().isEmpty()) {
            var a0 = t0.assunto().get(0);
            if (a0 != null) {
                idAssuntoPrincipal = a0.codigo();
                desAssuntoPrincipal = a0.descricao();

                if (a0.outrosAssuntos() != null) {
                    for (Assunto a : a0.outrosAssuntos()) {
                        if (a != null) {
                            idsAssuntos.add(a.codigo());
                            desAssuntos.add(a.descricao());
                        }
                    }
                }
            }
        }

        Long idOrgao = null;
        String desOrgao = null;
        if (t0 != null && t0.orgaoJulgador() != null) {
            idOrgao = t0.orgaoJulgador().idLocal(); 
            desOrgao = t0.orgaoJulgador().nome();
        }

        return new ProcessoResumoTratado(
            desTribunal,
            numProcesso,
            idClasse,
            desClasse,
            idAssuntoPrincipal,
            desAssuntoPrincipal,
            idsAssuntos,
            desAssuntos,
            idOrgao,
            desOrgao
        );
    }

    private void indexarSentencasPublicas(ProcessoResumoTratado processoToIndex, List<Documentos> publicas){
        for (Documentos sentenca : publicas) {
            
        }

    }
}
