package dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SolicitationRepositoryTest {

    @Autowired
    private SolicitationRepository solicitationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    private User saveUser(String email, Role role) {
        return userRepository.saveAndFlush(new User(null, email, "hashed", role));
    }

    private Solicitation saveAndReload(Solicitation solicitation) {
        Long id = solicitationRepository.saveAndFlush(solicitation).getId();
        entityManager.clear();
        return solicitationRepository.findById(id).orElseThrow();
    }

    @Test
    void persistsAPartialDraftWithNullableStepFieldsEmpty() {
        User owner = saveUser("owner-draft@example.com", Role.CLIENT);

        Solicitation reloaded = saveAndReload(Solicitation.draft(owner.getId()));

        assertNotNull(reloaded.getId());
        assertEquals(owner.getId(), reloaded.getOwnerId());
        assertEquals(Status.DRAFT, reloaded.getStatus());
        assertEquals(1, reloaded.getCurrentStep());
        assertNull(reloaded.getTitle());
        assertNull(reloaded.getState());
        assertNull(reloaded.getEstimatedValue());
        assertNotNull(reloaded.getCreatedAt());
        assertNotNull(reloaded.getUpdatedAt());
        assertNull(reloaded.getSubmittedAt());
    }

    @Test
    void persistsAllThreeStepsAndReadsThemBack() {
        User owner = saveUser("owner-full@example.com", Role.CLIENT);
        Solicitation solicitation = Solicitation.draft(owner.getId());
        solicitation.saveBasicData(ServiceType.MAINTENANCE, "Revisar elevador", "Revisão preventiva do elevador social do prédio");
        solicitation.saveAddress(new Address("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", "SP", "12A", null));
        solicitation.saveConfirmation(Priority.HIGH, LocalDate.now().plusDays(3), new BigDecimal("150.50"), true);

        Solicitation reloaded = saveAndReload(solicitation);

        assertEquals(ServiceType.MAINTENANCE, reloaded.getServiceType());
        assertEquals("Revisar elevador", reloaded.getTitle());
        assertEquals("01310100", reloaded.getCep());
        assertEquals("SP", reloaded.getState());
        assertEquals("12A", reloaded.getNumber());
        assertNull(reloaded.getComplement());
        assertEquals(Priority.HIGH, reloaded.getPriority());
        assertEquals(LocalDate.now().plusDays(3), reloaded.getPreferredDate());
        assertEquals(0, new BigDecimal("150.50").compareTo(reloaded.getEstimatedValue()));
        assertTrue(reloaded.isTermsAccepted());
        assertEquals(3, reloaded.getCurrentStep());
    }

    @Test
    void persistsTheDecisionAndSubmissionTime() {
        User owner = saveUser("owner-decided@example.com", Role.CLIENT);
        User analyst = saveUser("analyst-decided@example.com", Role.ANALYST);
        Solicitation solicitation = Solicitation.draft(owner.getId());
        solicitation.submit();
        solicitation.decide(Decision.REJECT, analyst.getId(), "Endereço fora da área de atendimento");

        Solicitation reloaded = saveAndReload(solicitation);

        assertEquals(Status.REJECTED, reloaded.getStatus());
        assertEquals(analyst.getId(), reloaded.getDecidedBy());
        assertEquals("Endereço fora da área de atendimento", reloaded.getDecisionComment());
        assertNotNull(reloaded.getSubmittedAt());
    }
}
