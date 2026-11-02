package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.LoginRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.LoginResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.RegisterRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.RegisterResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Autenticação", description = "Cadastro de clientes e login")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Cadastra um CLIENT (nunca cria ANALYST nem ADMIN)")
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @Operation(summary = "Autentica e devolve o JWT")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(responseCode = "401", description = "Email ou senha inválidos")
    @PostMapping("/login")
    public LoginResponse authenticateUser(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(authService.login(request.email(), request.password()));
    }
}
