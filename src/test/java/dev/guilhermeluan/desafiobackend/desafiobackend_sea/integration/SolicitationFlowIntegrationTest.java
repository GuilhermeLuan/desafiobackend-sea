package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SolicitationFlowIntegrationTest extends BaseIntegrationTest {

    // fluxo principal

    private static final String BASIC_DATA = """
            {"serviceType":"INSTALLATION","title":"Instalar ar-condicionado",
             "description":"Instalação de um split na sala de estar do apartamento"}""";
    private static final String CONFIRMATION = """
            {"priority":"MEDIUM","preferredDate":"2099-01-15","estimatedValue":250.00,"termsAccepted":true}""";

    // etapas e submit

    private static String address(String cep, String number) {
        return "{\"cep\":\"" + cep + "\",\"number\":\"" + number + "\"}";
    }

    @Test
    void clientSubmitsAndTheAnalystCoveringTheUfApprovesIt() throws Exception {
        String analystToken = createAnalystAndLogin("SP");
        String clientToken = registerClientAndLogin();
        long id = submittedSolicitation(clientToken, SP_CEP);

        send(get("/analyst/solicitations/" + id), analystToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.street").value("Avenida Paulista"));

        send(post("/analyst/solicitations/" + id + "/start"), analystToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        send(post("/analyst/solicitations/" + id + "/decide"), analystToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Documentação conferida e aprovada\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decisionComment").value("Documentação conferida e aprovada"));

        send(get("/solicitations/" + id), clientToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.decidedBy").isNumber());

        send(post("/analyst/solicitations/" + id + "/decide"), analystToken,
                "{\"decision\":\"REJECT\",\"comment\":\"Mudei de ideia sobre esta solicitação\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void theAnalystCanDecideStraightFromSubmittedWithoutStarting() throws Exception {
        String analystToken = createAnalystAndLogin("SP");
        long id = submittedSolicitation(registerClientAndLogin(), SP_CEP);

        send(post("/analyst/solicitations/" + id + "/decide"), analystToken,
                "{\"decision\":\"REJECT\",\"comment\":\"Endereço fora da área de atendimento\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // isolamento entre clientes

    @Test
    void submitOfAnIncompleteDraftListsWhatIsMissing() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA).andExpect(status().isOk());

        send(post("/solicitations/" + id + "/submit"), clientToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.missingSteps[0]").value(2))
                .andExpect(jsonPath("$.missingSteps[1]").value(3))
                .andExpect(jsonPath("$.fieldErrors").isArray());

        send(get("/solicitations/" + id), clientToken)
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    // cobertura do analista

    @Test
    void stepsMustBeSavedInOrderAndAnUnknownCepIsRejected() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(put("/solicitations/" + id + "/steps/2"), clientToken, address("01310-100", "1578"))
                .andExpect(status().isConflict());

        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA).andExpect(status().isOk());

        send(put("/solicitations/" + id + "/steps/2"), clientToken, address("99999999", "10"))
                .andExpect(status().isBadRequest());
        send(get("/solicitations/" + id), clientToken)
                .andExpect(jsonPath("$.currentStep").value(2));
    }

    @Test
    void aSubmittedSolicitationCanNoLongerBeEdited() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = submittedSolicitation(clientToken, SP_CEP);

        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA).andExpect(status().isConflict());
        send(post("/solicitations/" + id + "/submit"), clientToken).andExpect(status().isConflict());
    }

    @Test
    void anotherClientCannotSeeEditOrSubmitSomeoneElsesSolicitation() throws Exception {
        long id = createDraft(registerClientAndLogin());
        String intruderToken = registerClientAndLogin();

        send(get("/solicitations/" + id), intruderToken).andExpect(status().isNotFound());
        send(put("/solicitations/" + id + "/steps/1"), intruderToken, BASIC_DATA).andExpect(status().isNotFound());
        send(post("/solicitations/" + id + "/submit"), intruderToken).andExpect(status().isNotFound());
    }

    @Test
    void anAnalystOutsideTheCoverageGetsNotFoundAndNothingChanges() throws Exception {
        String analystToken = createAnalystAndLogin("DF");
        String clientToken = registerClientAndLogin();
        long id = submittedSolicitation(clientToken, SP_CEP);

        send(get("/analyst/solicitations/" + id), analystToken).andExpect(status().isNotFound());
        send(post("/analyst/solicitations/" + id + "/start"), analystToken).andExpect(status().isNotFound());
        send(post("/analyst/solicitations/" + id + "/decide"), analystToken,
                "{\"decision\":\"APPROVE\",\"comment\":\"Tentando decidir fora da cobertura\"}")
                .andExpect(status().isNotFound());

        send(get("/solicitations/" + id), clientToken)
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.decidedBy").doesNotExist());
    }

    // papéis

    @Test
    void theAnalystCoveringBrasiliaSeesItAndTheOneCoveringSaoPauloDoesNot() throws Exception {
        String brasiliaAnalyst = createAnalystAndLogin("DF");
        String saoPauloAnalyst = createAnalystAndLogin("SP");
        long id = submittedSolicitation(registerClientAndLogin(), BSB_CEP);

        send(get("/analyst/solicitations/" + id), saoPauloAnalyst).andExpect(status().isNotFound());

        send(get("/analyst/solicitations/" + id), brasiliaAnalyst)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("DF"))
                .andExpect(jsonPath("$.city").value("Brasília"));
        send(post("/analyst/solicitations/" + id + "/decide"), brasiliaAnalyst,
                "{\"decision\":\"APPROVE\",\"comment\":\"Atendimento autorizado no Distrito Federal\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void theNotFoundForAnUncoveredUfLooksLikeTheNotFoundForAMissingId() throws Exception {
        String analystToken = createAnalystAndLogin("DF");
        long id = submittedSolicitation(registerClientAndLogin(), SP_CEP);

        String uncovered = send(get("/analyst/solicitations/" + id), analystToken)
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String missing = send(get("/analyst/solicitations/999999999"), analystToken)
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();

        org.junit.jupiter.api.Assertions.assertEquals(
                JsonPath.<String>read(uncovered, "$.detail"), JsonPath.<String>read(missing, "$.detail"));
    }

    // apoio

    @Test
    void anAnalystNeverSeesDrafts() throws Exception {
        String analystToken = createAnalystAndLogin("SP");
        long id = createDraft(registerClientAndLogin());

        send(get("/analyst/solicitations/" + id), analystToken).andExpect(status().isNotFound());
    }

    @Test
    void eachRoleStaysInItsOwnArea() throws Exception {
        String analystToken = createAnalystAndLogin("SP");
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(get("/analyst/solicitations/" + id), clientToken).andExpect(status().isForbidden());
        send(post("/solicitations"), analystToken).andExpect(status().isForbidden());
        send(post("/solicitations"), adminToken).andExpect(status().isForbidden());
        send(post("/admin/users"), clientToken, "{}").andExpect(status().isForbidden());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mockMvc.perform(post("/solicitations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/analyst/solicitations/1")).andExpect(status().isUnauthorized());
    }

    private long createDraft(String clientToken) throws Exception {
        String body = send(post("/solicitations"), clientToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private long submittedSolicitation(String clientToken, String cep) throws Exception {
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA).andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/2"), clientToken, address(cep, "1578")).andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/3"), clientToken, CONFIRMATION).andExpect(status().isOk());
        send(post("/solicitations/" + id + "/submit"), clientToken).andExpect(status().isOk());
        return id;
    }
}
