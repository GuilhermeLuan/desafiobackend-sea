package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.Audit;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditAction;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.JwtUtils;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userService = userService;
    }

    @Audit(action = AuditAction.REGISTER, resourceId = "#result.id")
    public User register(String email, String password) {
        return userService.createUser(email, password, Role.CLIENT);
    }

    public String login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        return jwtUtils.generateToken(user.getUsername(), user.getRole());
    }
}
