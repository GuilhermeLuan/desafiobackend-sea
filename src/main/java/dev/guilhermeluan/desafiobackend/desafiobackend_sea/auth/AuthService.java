package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.JwtUtils;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.AuthenticatedUser;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository, PasswordEncoder encoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
    }

    public User register(String email, String password) {
        throwIfEmailAlreadyRegistered(email);

        User user = new User(null, email, encoder.encode(password), Role.CLIENT);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            throw new EmailAlreadyRegisteredException(email);
        }

        return user;
    }

    private void throwIfEmailAlreadyRegistered(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
    }

    public String login(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();

        return jwtUtils.generateToken(user.getUsername(), user.getRole());
    }
}
