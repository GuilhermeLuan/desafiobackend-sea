package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.jwt.JwtUtils;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.AuthenticatedUser;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

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
    private JwtUtils jwtUtils;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCreatesUserWithClientRole() {
        User created = new User(1L, "a@example.com", "hashed", Role.CLIENT);
        when(userService.createUser("a@example.com", "password123", Role.CLIENT)).thenReturn(created);

        User response = authService.register("a@example.com", "password123");

        assertEquals(created, response);
        verify(userService).createUser("a@example.com", "password123", Role.CLIENT);
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
