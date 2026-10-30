package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.CreateUserRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.UpdateCoverageRequest;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = userService.createPrivilegedUser(request.email(), request.password(), request.role(), request.coverageUfs());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCoverageUfs());
    }

    @PutMapping("/{id}/coverage")
    public UserResponse updateCoverage(@PathVariable Long id, @Valid @RequestBody UpdateCoverageRequest request) {
        User user = userService.updateCoverage(id, request.coverageUfs());
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCoverageUfs());
    }
}
