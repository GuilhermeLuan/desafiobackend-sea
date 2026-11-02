package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.CreateUserRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.UpdateCoverageRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Usuários (ADMIN)", description = "Criação de usuários internos e cobertura por UF")
@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Cria um ANALYST (com as UFs de cobertura) ou um ADMIN")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createPrivilegedUser(request.email(), request.password(), request.role(), request.coverageUfs());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCoverageUfs());
    }

    @Operation(summary = "Substitui a lista de UFs que um ANALYST cobre")
    @PutMapping("/{id}/coverage")
    public UserResponse updateCoverage(@PathVariable Long id, @Valid @RequestBody UpdateCoverageRequest request) {
        User user = userService.updateCoverage(id, request.coverageUfs());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCoverageUfs());
    }
}
