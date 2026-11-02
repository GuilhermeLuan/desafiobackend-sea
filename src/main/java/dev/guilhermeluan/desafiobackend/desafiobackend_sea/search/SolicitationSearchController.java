package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.PageResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchItem;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchParams;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.CurrentRole;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Busca", description = "Busca no Elasticsearch para ANALYST e ADMIN")
@RestController
@RequestMapping("/analyst/solicitations")
public class SolicitationSearchController {

    private final SolicitationSearchService searchService;

    public SolicitationSearchController(SolicitationSearchService searchService) {
        this.searchService = searchService;
    }

    @Operation(summary = "Busca solicitações; a UF é forçada pela cobertura de quem busca e rascunhos nunca aparecem")
    @GetMapping("/search")
    public PageResponse<SolicitationSearchItem> search(@ParameterObject @Valid SolicitationSearchParams params, Authentication authentication) {
        return searchService.search(authentication.getName(), CurrentRole.of(authentication), params);
    }
}
