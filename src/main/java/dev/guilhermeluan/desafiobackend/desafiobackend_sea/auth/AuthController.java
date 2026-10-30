package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;


import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.LoginRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.LoginResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.RegisterRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos.RegisterResponse;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        User user = authService.register(request.email(), request.password());
        return new RegisterResponse(user.getId(), user.getEmail(), user.getRole());
    }

    @PostMapping("/login")
    public LoginResponse authenticateUser(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(authService.login(request.email(), request.password()));
    }
}
