package br.jus.tjm.ism.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.Objects;
import java.time.*;
import java.time.format.DateTimeFormatter;

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
import br.jus.tjm.ism.dto.SentencaIndex;


@Service
public class DatalakeProcessosApiService {

    @Value("${datalake-processos-api.base-url}")
    private String apiBaseUrl;

    private final KeycloakTokenService keycloakTokenService;
    private final OpenSearchService openSearchService;

    public DatalakeProcessosApiService(KeycloakTokenService keycloakTokenService,OpenSearchService openSearchService) {
        this.keycloakTokenService = keycloakTokenService;
        this.openSearchService = openSearchService;
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
            Documentos publicas = listaSentencas.sentencaPublica();
            Documentos sigilosas = listaSentencas.sentencaSigilosa();

            ProcessoResumoTratado processoToIndex = this.getDadosProcessoIndexacao(processo);

            if (publicas != null) {
                this.indexarSentencasPublicas(processoToIndex,publicas);
            }

            if (sigilosas != null) {
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
        
        Documentos sentencaPublica  = null;
        Documentos sentencaSigilosa = null;

        for (Documentos documento : listaDocumentos) {
            if (documento != null && documento.tipo() != null && codigosAceitos.contains(documento.tipo().codigo())) {

                if (documento.nivelSigilo().equals("PUBLICO")){
                    sentencaPublica = documento;
                } else {                    
                    sentencaSigilosa = documento;
                }
            }
        }

        ListaSentencas base = ListaSentencas.vazia(); // new Documentos(), new Documentos()
        ListaSentencas listaSentencas = new ListaSentencas(
                sentencaPublica  != null ? sentencaPublica  : base.sentencaPublica(),
                sentencaSigilosa != null ? sentencaSigilosa : base.sentencaSigilosa()
        );

        return listaSentencas;
    }

    public String getDocumentoTexto(String numProcesso, String idDocumento) {
        String url = String.format("%s/%s/documentos/%s/texto", apiBaseUrl, numProcesso, idDocumento);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(keycloakTokenService.getAccessToken());
        headers.setAccept(List.of(MediaType.TEXT_PLAIN, MediaType.ALL));

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IllegalStateException("Falha ao obter texto: " + resp.getStatusCode());
        }

        //return decodeBytes(resp.getBody(), resp.getHeaders().getContentType());
        return resp.getBody();
    }

    private static String decodeBytes(byte[] bytes, @org.springframework.lang.Nullable MediaType contentType) {
        if (bytes.length == 0) return "";

        // remove BOM UTF-8 se houver
        if (bytes.length >= 3 && (bytes[0] & 0xFF) == 0xEF && (bytes[1] & 0xFF) == 0xBB && (bytes[2] & 0xFF) == 0xBF) {
            bytes = java.util.Arrays.copyOfRange(bytes, 3, bytes.length);
        }

        java.nio.charset.Charset cs =
            (contentType != null && contentType.getCharset() != null)
                ? contentType.getCharset()
                : java.nio.charset.StandardCharsets.UTF_8; // fallback inicial

        String s = new String(bytes, cs);

        // se vier caracteres de substituição, tente ISO-8859-1 como fallback
        if (s.indexOf('\uFFFD') >= 0) {
            s = new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1);
        }

        return s;
    }


    private ProcessoResumoTratado getDadosProcessoIndexacao(ProcessoResumo processo) {
        String desTribunal = processo.siglaTribunal(); 
        String numProcesso = processo.numeroProcesso();
        String idProcesso = processo.id();

        var t0 = (processo.tramitacoes() != null && !processo.tramitacoes().isEmpty())
                ? processo.tramitacoes().get(0) : null;

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
        Integer idClasse = null;
        String desClasse = null;
        

        return new ProcessoResumoTratado(
            desTribunal,
            idProcesso,
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

    private void indexarSentencasPublicas(ProcessoResumoTratado processoToIndex, Documentos publica){
        String texto = this.getDocumentoTexto(processoToIndex.id(),publica.id());

        SentencaIndex doc = this.montarSentencaIndex(processoToIndex,publica,texto);
        openSearchService.indexarSentenca(doc);

    }

    private SentencaIndex montarSentencaIndex(
            ProcessoResumoTratado p,
            Documentos doc,
            String texto 
    ) {
        // data_juntada (yyyy-MM-dd) e @timestamp (epoch seconds)
        String dataJuntada = toDateOnly(doc.dataHoraJuntada());   // ex: "2024-05-15"
        Long   ts          = toEpochSeconds(doc.dataHoraJuntada()); // ex: 1715769537

        return new SentencaIndex(
            p.des_tribunal(),
            doc.id(),
            p.num_processo(),
            p.id_classe(),
            p.des_classe(),
            p.id_assunto_principal(),
            p.des_assunto_principal(),
            p.ids_assuntos(),
            p.des_assuntos(),
            p.id_orgao(),
            p.des_orgao(),
            dataJuntada,
            ts,
            texto
        );
    }

    private static String toDateOnly(String isoLocalDateTime) {
        if (isoLocalDateTime == null || isoLocalDateTime.isBlank()) return null;
        // Ex.: "2024-05-15T07:58:57.156"
        LocalDateTime ldt = LocalDateTime.parse(isoLocalDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return ldt.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE); // "yyyy-MM-dd"
    }

    private static Long toEpochSeconds(String isoLocalDateTime) {
        if (isoLocalDateTime == null || isoLocalDateTime.isBlank()) return null;
        LocalDateTime ldt = LocalDateTime.parse(isoLocalDateTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // ajuste a zona se precisar (ex.: America/Sao_Paulo). Aqui uso UTC.
        return ldt.toEpochSecond(ZoneOffset.UTC);
    }
}
