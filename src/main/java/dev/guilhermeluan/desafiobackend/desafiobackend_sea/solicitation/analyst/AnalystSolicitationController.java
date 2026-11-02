package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.CurrentRole;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.analyst.dtos.DecideRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.SolicitationResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/analyst/solicitations")
public class AnalystSolicitationController {

    private final AnalystSolicitationService analystSolicitationService;
    private final UserService userService;

    public AnalystSolicitationController(AnalystSolicitationService analystSolicitationService, UserService userService) {
        this.analystSolicitationService = analystSolicitationService;
        this.userService = userService;
    }

    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(analystSolicitationService.get(authentication.getName(), CurrentRole.of(authentication), id));
    }

    @PostMapping("/{id}/start")
    public SolicitationResponse start(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(analystSolicitationService.start(authentication.getName(), CurrentRole.of(authentication), id));
    }

    @PostMapping("/{id}/decide")
    public SolicitationResponse decide(@PathVariable Long id, @Valid @RequestBody DecideRequest request,
                                       Authentication authentication) {
        String email = authentication.getName();
        Long analystId = userService.findIdByEmail(email);
        return SolicitationResponse.from(
                analystSolicitationService.decide(email, CurrentRole.of(authentication), analystId, id, request.decision(), request.comment()));
    }
}
