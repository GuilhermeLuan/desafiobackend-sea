package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.AddressRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.BasicDataRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.client.dtos.ConfirmationRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.dtos.SolicitationResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Solicitações (cliente)", description = "Rascunho, as 3 etapas e o envio; só o dono acessa")
@RestController
@RequestMapping("/solicitations")
public class SolicitationController {

    private final SolicitationService solicitationService;
    private final UserService userService;

    public SolicitationController(SolicitationService solicitationService, UserService userService) {
        this.solicitationService = solicitationService;
        this.userService = userService;
    }

    @Operation(summary = "Cria um rascunho (DRAFT)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitationResponse createDraft(Authentication authentication) {
        return SolicitationResponse.from(solicitationService.createDraft(currentUserId(authentication)));
    }

    @Operation(summary = "Consulta uma solicitação do próprio cliente")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "404", description = "Não existe ou não é sua (a resposta é a mesma de propósito, para não revelar quais ids existem)")
    @GetMapping("/{id}")
    public SolicitationResponse get(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(solicitationService.get(currentUserId(authentication), id));
    }

    @Operation(summary = "Salva a etapa 1: dados básicos")
    @PutMapping("/{id}/steps/1")
    public SolicitationResponse saveBasicData(@PathVariable Long id, @Valid @RequestBody BasicDataRequest request,
                                              Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveBasicData(currentUserId(authentication), id, request));
    }

    @Operation(summary = "Salva a etapa 2: endereço (rua, bairro, cidade e UF vêm do ViaCEP)")
    @PutMapping("/{id}/steps/2")
    public SolicitationResponse saveAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request,
                                            Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveAddress(currentUserId(authentication), id, request));
    }

    @Operation(summary = "Salva a etapa 3: confirmação")
    @PutMapping("/{id}/steps/3")
    public SolicitationResponse saveConfirmation(@PathVariable Long id, @Valid @RequestBody ConfirmationRequest request,
                                                 Authentication authentication) {
        return SolicitationResponse.from(solicitationService.saveConfirmation(currentUserId(authentication), id, request));
    }

    @Operation(summary = "Envia a solicitação (valida todas as etapas e regras)")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "400", description = "Incompleta ou com regra violada; lista missingSteps e fieldErrors")
    @ApiResponse(responseCode = "409", description = "Já enviada (só um rascunho pode ser enviado)")
    @PostMapping("/{id}/submit")
    public SolicitationResponse submit(@PathVariable Long id, Authentication authentication) {
        return SolicitationResponse.from(solicitationService.submit(currentUserId(authentication), id));
    }

    private Long currentUserId(Authentication authentication) {
        return userService.findIdByEmail(authentication.getName());
    }
}
