package dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository repository;

    private Target target;

    private static void login(String email) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
    }

    @BeforeEach
    void setUp() {
        AspectJProxyFactory factory = new AspectJProxyFactory(new Target());
        factory.addAspect(new AuditAspect(repository));
        target = factory.getProxy();
        login("user@example.com");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private AuditLog recorded() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void aSuccessfulCallIsRecordedWithWhoWhatAndWhichResourceAndTheReturnValueIsUntouched() {
        assertEquals(42L, target.createUser().getId());

        AuditLog entry = recorded();
        assertEquals(AuditOutcome.SUCCESS, entry.getOutcome());
        assertEquals(AuditAction.CREATE_USER, entry.getAction());
        assertEquals(AuditResourceType.USER, entry.getResourceType());
        assertEquals("user@example.com", entry.getActor());
        assertEquals(42L, entry.getResourceId());
        assertNull(entry.getErrorType());
    }

    @Test
    void theResourceIdCanComeFromAMethodParameterByName() {
        assertEquals("started", target.startById(10L, 7L));

        assertEquals(7L, recorded().getResourceId());
    }

    @Test
    void aFailureIsRecordedAndTheSameExceptionIsRethrown() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> target.decideFails(7L));

        assertEquals("boom", thrown.getMessage());
        AuditLog entry = recorded();
        assertEquals(AuditOutcome.FAILURE, entry.getOutcome());
        assertEquals("IllegalStateException", entry.getErrorType());
        assertEquals(7L, entry.getResourceId());
    }

    @Test
    void aBrokenExpressionNeverBreaksTheBusinessCall() {
        assertEquals("submitted", target.submitWithBrokenExpression());

        AuditLog entry = recorded();
        assertEquals(AuditOutcome.SUCCESS, entry.getOutcome());
        assertNull(entry.getResourceId());
    }

    @Test
    void withoutAuthenticationTheActorIsAnonymous() {
        SecurityContextHolder.clearContext();

        target.createUser();

        assertEquals("anonymous", recorded().getActor());
    }

    @Test
    void aFailureWhileWritingTheAuditNeverReachesTheCaller() {
        doThrow(new RuntimeException("audit database is down")).when(repository).save(any());

        assertEquals(42L, target.createUser().getId());
    }

    @Test
    void aFailureWhileWritingTheAuditNeverMasksTheOriginalException() {
        doThrow(new RuntimeException("audit database is down")).when(repository).save(any());

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> target.decideFails(7L));

        assertEquals("boom", thrown.getMessage());
    }

    public static class Target {

        @Audit(action = AuditAction.CREATE_USER, resourceId = "#result.id")
        public Created createUser() {
            return new Created();
        }

        @Audit(action = AuditAction.START, resourceId = "#id")
        public String startById(Long ownerId, Long id) {
            return "started";
        }

        @Audit(action = AuditAction.DECIDE, resourceId = "#id")
        public void decideFails(Long id) {
            throw new IllegalStateException("boom");
        }

        @Audit(action = AuditAction.SUBMIT, resourceId = "#naoExiste.foo.bar")
        public String submitWithBrokenExpression() {
            return "submitted";
        }

        public static class Created {
            public Long getId() {
                return 42L;
            }
        }
    }
}
