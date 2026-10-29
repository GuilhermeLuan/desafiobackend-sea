package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService userService;

    @Test
    void createUserSavesWithEncodedPasswordAndGivenRole() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        User created = userService.createUser("a@example.com", "password123", Role.ANALYST);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(saved.capture());
        assertEquals("hashed", saved.getValue().getPasswordHash());
        assertEquals(Role.ANALYST, saved.getValue().getRole());
        assertEquals(1L, created.getId());
        assertEquals("a@example.com", created.getEmail());
    }

    @Test
    void createUserWithExistingEmailThrows() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> userService.createUser("a@example.com", "password123", Role.CLIENT));

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUserThrowsWhenDatabaseRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("a@example.com")).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThrows(EmailAlreadyRegisteredException.class,
                () -> userService.createUser("a@example.com", "password123", Role.CLIENT));
    }

    @Test
    void createPrivilegedUserAcceptsAnalystAndAdmin() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(encoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals(Role.ANALYST, userService.createPrivilegedUser("a@example.com", "password123", Role.ANALYST).getRole());
        assertEquals(Role.ADMIN, userService.createPrivilegedUser("b@example.com", "password123", Role.ADMIN).getRole());
    }

    @Test
    void createPrivilegedUserRejectsClientRole() {
        assertThrows(InvalidRoleException.class,
                () -> userService.createPrivilegedUser("a@example.com", "password123", Role.CLIENT));

        verify(userRepository, never()).saveAndFlush(any());
    }
}
