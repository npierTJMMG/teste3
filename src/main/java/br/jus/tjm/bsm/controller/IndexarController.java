package br.jus.tjm.bsm.controller;

import br.jus.tjm.bsm.dto.IndexadorRequest;
import br.jus.tjm.bsm.service.IndexadorService;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sentencas/indexar")
public class IndexarController {

    private final IndexadorService indexadorService;

    public IndexarController(
        IndexadorService indexadorService
    ) {
        this.indexadorService = indexadorService;
    }

    @PostMapping
    public ResponseEntity<?> buscarSentencas(@RequestBody IndexadorRequest request) {
        try {
            return ResponseEntity.ok(indexadorService.buscar(request));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Erro ao realizar indexação: " + ex.getMessage());
        }
    }
}
