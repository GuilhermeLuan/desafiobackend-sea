package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.JwtUtils;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.AuthenticatedUser;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtils jwtUtils;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesClientWithEncodedPassword() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        User response = authService.register("a@example.com", "password123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("hashed", saved.getValue().getPasswordHash());
        assertEquals(Role.CLIENT, saved.getValue().getRole());
        assertEquals(1L, response.getId());
        assertEquals("a@example.com", response.getEmail());
    }

    @Test
    void registerWithExistingEmailThrows() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register("a@example.com", "password123"));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerThrowsWhenDatabaseRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> authService.register("a@example.com", "password123"));
    }

    @Test
    void loginReturnsTokenWithUserRole() {
        AuthenticatedUser principal = new AuthenticatedUser(new User(1L, "a@example.com", "hashed", Role.ADMIN));
        when(authenticationManager.authenticate(any()))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        when(jwtUtils.generateToken("a@example.com", Role.ADMIN)).thenReturn("token");

        String token = authService.login("a@example.com", "password123");

        assertEquals("token", token);
    }

    @Test
    void loginWithBadCredentialsPropagatesException() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThrows(BadCredentialsException.class,
                () -> authService.login("a@example.com", "wrong"));

        verify(jwtUtils, never()).generateToken(any(), any());
    }
}
