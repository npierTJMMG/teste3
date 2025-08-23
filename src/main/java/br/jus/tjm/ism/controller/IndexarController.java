package br.jus.tjm.ism.controller;

import br.jus.tjm.ism.service.IndexadorService;

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

    @GetMapping
    public ResponseEntity<?> indexarSentenca(
        @RequestParam("tribunal") String tribunal,
        @RequestParam("dth_inicio") String dthInicio,
        @RequestParam("dth_fim") String dthFim
    ) {
        try {
            return ResponseEntity.ok(indexadorService.indexar(tribunal,dthInicio,dthFim));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Erro ao realizar indexação: " + ex.getMessage());
        }
    }
}
