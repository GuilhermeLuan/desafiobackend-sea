package dev.guilhermeluan.desafiobackend.desafiobackend_sea.search;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.PageResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchItem;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.search.dtos.SolicitationSearchParams;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analyst/solicitations")
public class SolicitationSearchController {

    private final SolicitationSearchService searchService;

    public SolicitationSearchController(SolicitationSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public PageResponse<SolicitationSearchItem> search(@Valid SolicitationSearchParams params, Authentication authentication) {
        return searchService.search(authentication.getName(), params);
    }
}
