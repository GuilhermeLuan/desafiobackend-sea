package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditAction;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditLog;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditLogRepository;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditOutcome;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.audit.AuditResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminAnalysisIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void anAdminReadsSolicitationsInAnyUf() throws Exception {
        String clientToken = registerClientAndLogin();
        long inSp = submittedSolicitation(clientToken, SP_CEP);
        long inDf = submittedSolicitation(clientToken, BSB_CEP);

        send(get("/analyst/solicitations/" + inSp), adminToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("SP"));
        send(get("/analyst/solicitations/" + inDf), adminToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.state").value("DF"));
    }

    @Test
    void anAdminCanStartAndDecideASolicitationOutsideAnyAnalystsCoverage() throws Exception {
        long id = submittedSolicitation(registerClientAndLogin(), BSB_CEP);

        send(post("/analyst/solicitations/" + id + "/start"), adminToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        send(post("/analyst/solicitations/" + id + "/decide"), adminToken,
                "{\"decision\":\"REJECT\",\"comment\":\"Endereço fora da área de atendimento\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.decidedBy").isNumber());
    }

    @Test
    void anAdminStillCannotSeeADraft() throws Exception {
        long id = createDraft(registerClientAndLogin());

        send(get("/analyst/solicitations/" + id), adminToken).andExpect(status().isNotFound());
        send(post("/analyst/solicitations/" + id + "/start"), adminToken).andExpect(status().isNotFound());
    }

    @Test
    void anAdminsDecisionIsAuditedWithTheAdminAsTheActor() throws Exception {
        long id = submittedSolicitation(registerClientAndLogin(), SP_CEP);

        send(post("/analyst/solicitations/" + id + "/decide"), adminToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Aprovado diretamente pelo administrador\"}")
                .andExpect(status().isOk());

        List<AuditLog> trail = auditLogRepository.findByResourceTypeAndResourceIdOrderByIdAsc(AuditResourceType.SOLICITATION, id);
        AuditLog decision = trail.get(trail.size() - 1);
        assertEquals(AuditAction.DECIDE, decision.getAction());
        assertEquals(AuditOutcome.SUCCESS, decision.getOutcome());
        assertEquals(ADMIN_EMAIL, decision.getActor());
    }

    @Test
    void anAdminStillCannotUseTheClientRoutes() throws Exception {
        send(post("/solicitations"), adminToken).andExpect(status().isForbidden());
    }

    @Test
    void aClientStillCannotUseTheAnalystRoutes() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = submittedSolicitation(clientToken, SP_CEP);

        send(get("/analyst/solicitations/" + id), clientToken).andExpect(status().isForbidden());
        send(post("/analyst/solicitations/" + id + "/decide"), clientToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Cliente tentando decidir a própria solicitação\"}")
                .andExpect(status().isForbidden());
    }
}
