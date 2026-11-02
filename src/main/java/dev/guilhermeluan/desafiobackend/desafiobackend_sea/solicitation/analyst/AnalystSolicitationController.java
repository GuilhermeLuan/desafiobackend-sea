package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.CurrentRole;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst.dtos.DecideRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.SolicitationResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Solicitações (analista)", description = "Para ANALYST (só as UFs que cobre) e ADMIN (qualquer UF). Rascunhos nunca aparecem")
@RestController
@RequestMapping("/analyst/solicitations")
public class AnalystSolicitationController {

    private final AnalystSolicitationService analystSolicitationService;
    private final UserService userService;

    public AnalystSolicitationController(AnalystSolicitationService analystSolicitationService, UserService userService) {
        this.analystSolicitationService = analystSolicitationService;
        this.userService = userService;
    }

    @Operation(summary = "Consulta uma solicitação enviada")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Não existe, é um rascunho ou está fora da cobertura (a resposta é a mesma de propósito)")
    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(analystSolicitationService.get(authentication.getName(), CurrentRole.of(authentication), id));
    }

    @Operation(summary = "Inicia a análise (SUBMITTED → IN_REVIEW)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "409", description = "Transição inválida (só uma solicitação SUBMITTED pode ser iniciada)")
    @PostMapping("/{id}/start")
    public SolicitationResponse start(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(analystSolicitationService.start(authentication.getName(), CurrentRole.of(authentication), id));
    }

    @Operation(summary = "Aprova ou rejeita, com comentário de 10 a 1000 caracteres")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "409", description = "Transição inválida (já decidida, ou ainda não enviada)")
    @PostMapping("/{id}/decide")
    public SolicitationResponse decide(@PathVariable Long id, @Valid @RequestBody DecideRequest request,
                                       Authentication authentication) {
        String email = authentication.getName();
        Long analystId = userService.findIdByEmail(email);
        return SolicitationResponse.from(
                analystSolicitationService.decide(email, CurrentRole.of(authentication), analystId, id, request.decision(), request.comment()));
    }
}
