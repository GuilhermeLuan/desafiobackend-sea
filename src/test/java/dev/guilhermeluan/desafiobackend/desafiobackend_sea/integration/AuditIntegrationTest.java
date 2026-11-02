package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import com.jayway.jsonpath.JsonPath;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private Registered registerClient() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        String body = mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        return new Registered(id, email, login(email, PASSWORD));
    }

    private List<AuditLog> trailOf(AuditResourceType type, long id) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc(type, id);
    }

    @Test
    void registeringIsAuditedAsAnonymousWithTheNewUsersId() throws Exception {
        Registered client = registerClient();

        List<AuditLog> trail = trailOf(AuditResourceType.USER, client.id());

        assertEquals(1, trail.size());
        assertEquals(AuditAction.REGISTER, trail.get(0).getAction());
        assertEquals(AuditOutcome.SUCCESS, trail.get(0).getOutcome());
        assertEquals("anonymous", trail.get(0).getActor());
        assertNotNull(trail.get(0).getOccurredAt());
    }

    @Test
    void creatingAPrivilegedUserIsAuditedWithTheAdminAsTheActor() throws Exception {
        String email = "analyst-" + UUID.randomUUID() + "@example.com";
        String body = send(post("/admin/users"), adminToken,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ANALYST\",\"coverageUfs\":[\"SP\"]}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();

        List<AuditLog> trail = trailOf(AuditResourceType.USER, id);

        assertEquals(1, trail.size());
        assertEquals(AuditAction.CREATE_USER, trail.get(0).getAction());
        assertEquals(AuditOutcome.SUCCESS, trail.get(0).getOutcome());
        assertEquals(ADMIN_EMAIL, trail.get(0).getActor());
    }

    @Test
    void submitStartAndDecideLeaveATrailInOrderEachWithItsOwnActor() throws Exception {
        Registered client = registerClient();
        String analystEmail = "analyst-" + UUID.randomUUID() + "@example.com";
        send(post("/admin/users"), adminToken,
                "{\"email\":\"" + analystEmail + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ANALYST\",\"coverageUfs\":[\"SP\"]}")
                .andExpect(status().isCreated());
        String analystToken = login(analystEmail, PASSWORD);
        long id = submittedSolicitation(client.token(), SP_CEP);

        send(post("/analyst/solicitations/" + id + "/start"), analystToken).andExpect(status().isOk());
        send(post("/analyst/solicitations/" + id + "/decide"), analystToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Documentação conferida e aprovada\"}").andExpect(status().isOk());

        List<AuditLog> trail = trailOf(AuditResourceType.SOLICITATION, id);

        assertEquals(List.of(AuditAction.SUBMIT, AuditAction.START, AuditAction.DECIDE),
                trail.stream().map(AuditLog::getAction).toList());
        assertTrue(trail.stream().allMatch(entry -> entry.getOutcome() == AuditOutcome.SUCCESS));
        assertEquals(client.email(), trail.get(0).getActor());
        assertEquals(analystEmail, trail.get(1).getActor());
        assertEquals(analystEmail, trail.get(2).getActor());
    }

    @Test
    void aFailedSubmitIsStillAuditedEvenThoughTheOperationWasRolledBack() throws Exception {
        Registered client = registerClient();
        long id = createDraft(client.token());

        send(post("/solicitations/" + id + "/submit"), client.token()).andExpect(status().isBadRequest());

        List<AuditLog> trail = trailOf(AuditResourceType.SOLICITATION, id);
        assertEquals(1, trail.size());
        assertEquals(AuditAction.SUBMIT, trail.get(0).getAction());
        assertEquals(AuditOutcome.FAILURE, trail.get(0).getOutcome());
        assertEquals("IncompleteSolicitationException", trail.get(0).getErrorType());
        assertEquals(client.email(), trail.get(0).getActor());
    }

    @Test
    void aDeniedDecisionOutsideTheCoverageIsAuditedAsAFailure() throws Exception {
        Registered client = registerClient();
        long id = submittedSolicitation(client.token(), BSB_CEP);
        String spAnalystToken = createAnalystAndLogin("SP");

        send(post("/analyst/solicitations/" + id + "/decide"), spAnalystToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Tentando decidir fora da cobertura\"}")
                .andExpect(status().isNotFound());

        List<AuditLog> trail = trailOf(AuditResourceType.SOLICITATION, id);
        assertEquals(List.of(AuditAction.SUBMIT, AuditAction.DECIDE), trail.stream().map(AuditLog::getAction).toList());
        assertEquals(AuditOutcome.FAILURE, trail.get(1).getOutcome());
        assertEquals("CoverageAccessDeniedException", trail.get(1).getErrorType());
    }

    private record Registered(long id, String email, String token) {
    }
}
