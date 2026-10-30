package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoverageGuardTest {

    private static final String EMAIL = "analyst@example.com";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CoverageGuard guard;

    @Test
    void adminHasUnrestrictedCoverageWithoutQueryingTheDatabase() {
        Coverage coverage = guard.coverageOf(EMAIL, Role.ADMIN);

        assertTrue(coverage.isUnrestricted());
        verifyNoInteractions(userRepository);
    }

    @Test
    void analystCoverageComesFromTheUfsStoredForThatUser() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of("SP", "DF"));

        Coverage coverage = guard.coverageOf(EMAIL, Role.ANALYST);

        assertFalse(coverage.isUnrestricted());
        assertEquals(Set.of("SP", "DF"), coverage.ufs());
    }

    @Test
    void analystWithoutStoredUfsCoversNothing() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of());

        Coverage coverage = guard.coverageOf(EMAIL, Role.ANALYST);

        assertFalse(coverage.isUnrestricted());
        assertFalse(coverage.covers("SP"));
    }

    @Test
    void clientCoversNothingWithoutQueryingTheDatabase() {
        Coverage coverage = guard.coverageOf(EMAIL, Role.CLIENT);

        assertFalse(coverage.isUnrestricted());
        assertFalse(coverage.covers("SP"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void assertCoversPassesWhenAnalystCoversTheUf() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of("SP"));

        assertDoesNotThrow(() -> guard.assertCovers(EMAIL, Role.ANALYST, "SP"));
    }

    @Test
    void assertCoversIgnoresCaseOfTheUf() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of("SP"));

        assertDoesNotThrow(() -> guard.assertCovers(EMAIL, Role.ANALYST, "sp"));
    }

    @Test
    void assertCoversRejectsAnalystOutsideTheirCoverage() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of("SP"));

        assertThrows(CoverageAccessDeniedException.class,
                () -> guard.assertCovers(EMAIL, Role.ANALYST, "RJ"));
    }

    @Test
    void assertCoversRejectsMissingOrBlankUf() {
        when(userRepository.findCoverageUfsByEmail(EMAIL)).thenReturn(Set.of("SP"));

        assertThrows(CoverageAccessDeniedException.class,
                () -> guard.assertCovers(EMAIL, Role.ANALYST, null));
        assertThrows(CoverageAccessDeniedException.class,
                () -> guard.assertCovers(EMAIL, Role.ANALYST, " "));
    }

    @Test
    void assertCoversAllowsAdminAnyUf() {
        assertDoesNotThrow(() -> guard.assertCovers(EMAIL, Role.ADMIN, "RJ"));
        verifyNoInteractions(userRepository);
    }

    @Test
    void assertCoversRejectsClient() {
        assertThrows(CoverageAccessDeniedException.class,
                () -> guard.assertCovers(EMAIL, Role.CLIENT, "SP"));
    }
}
