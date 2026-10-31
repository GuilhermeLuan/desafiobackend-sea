package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.AddressRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.BasicDataRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.ConfirmationRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.SolicitationResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitations")
public class SolicitationController {

    private final SolicitationService solicitationService;
    private final UserService userService;

    public SolicitationController(SolicitationService solicitationService, UserService userService) {
        this.solicitationService = solicitationService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitationResponse createDraft(Authentication authentication) {
        return SolicitationResponse.from(solicitationService.createDraft(currentUserId(authentication)));
    }

    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(solicitationService.get(currentUserId(authentication), id));
    }

    @PutMapping("/{id}/steps/1")
    public SolicitationResponse saveBasicData(@PathVariable Long id, @Valid @RequestBody BasicDataRequest request,
                                              Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveBasicData(currentUserId(authentication), id, request));
    }

    @PutMapping("/{id}/steps/2")
    public SolicitationResponse saveAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request,
                                            Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveAddress(currentUserId(authentication), id, request));
    }

    @PutMapping("/{id}/steps/3")
    public SolicitationResponse saveConfirmation(@PathVariable Long id, @Valid @RequestBody ConfirmationRequest request,
                                                 Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveConfirmation(currentUserId(authentication), id, request));
    }

    @PostMapping("/{id}/submit")
    public SolicitationResponse submit(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(solicitationService.submit(currentUserId(authentication), id));
    }

    private Long currentUserId(Authentication authentication) {
        return userService.findIdByEmail(authentication.getName());
    }
}
