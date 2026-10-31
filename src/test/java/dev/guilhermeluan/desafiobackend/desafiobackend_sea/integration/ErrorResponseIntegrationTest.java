package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepServiceUnavailableException;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ErrorResponseIntegrationTest extends BaseIntegrationTest {

    // validação

    @Test
    void validationErrorsListEachInvalidFieldWithItsMessage() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(put("/solicitations/" + id + "/steps/1"), clientToken,
                "{\"serviceType\":\"INSTALLATION\",\"title\":\"ab\",\"description\":\"curta\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/solicitations/" + id + "/steps/1"))
                .andExpect(jsonPath("$.timestamp").value(not(emptyOrNullString())))
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')].message")
                        .value(hasItem("title deve ter entre 3 e 80 caracteres")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description')].message")
                        .value(hasItem("description deve ter entre 20 e 1000 caracteres")));
    }

    @Test
    void aBodyThatIsNotValidJsonIsReportedAsMalformed() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(put("/solicitations/" + id + "/steps/1"), clientToken, "{ isto não é json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void anUnknownEnumValueIsReportedAsMalformed() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);

        send(put("/solicitations/" + id + "/steps/1"), clientToken,
                "{\"serviceType\":\"NOPE\",\"title\":\"Instalar ar\",\"description\":\"Instalação de um split na sala de estar\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    // autenticação e autorização (erros que acontecem antes do controller)

    @Test
    void aMissingTokenIsAnUnauthorizedErrorInTheStandardFormat() throws Exception {
        mockMvc.perform(post("/solicitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/solicitations"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)))
                .andExpect(jsonPath("$.timestamp").value(not(emptyOrNullString())));
    }

    @Test
    void anInvalidTokenIsAnUnauthorizedError() throws Exception {
        send(post("/solicitations"), "isto.nao.e.um.jwt")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void aWrongRoleIsAForbiddenErrorInTheStandardFormat() throws Exception {
        String clientToken = registerClientAndLogin();

        send(get("/analyst/solicitations/1"), clientToken)
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.path").value("/analyst/solicitations/1"));
    }

    @Test
    void wrongCredentialsAreAnInvalidCredentialsError() throws Exception {
        mockMvc.perform(post("/auth/login").contentType("application/json")
                        .content("{\"email\":\"ninguem@example.com\",\"password\":\"senha-errada-123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // regras de negócio

    @Test
    void registeringTheSameEmailTwiceIsAConflict() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/register").contentType("application/json").content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void anInvalidStateTransitionIsAConflict() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = submittedSolicitation(clientToken, SP_CEP);

        send(post("/solicitations/" + id + "/submit"), clientToken)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
    }

    @Test
    void highPriorityBelowTheMinimumValueIsAnInvalidDataError() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA);
        send(put("/solicitations/" + id + "/steps/2"), clientToken, address(SP_CEP, "10"));

        send(put("/solicitations/" + id + "/steps/3"), clientToken,
                "{\"priority\":\"HIGH\",\"estimatedValue\":99.99,\"termsAccepted\":true}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }

    @Test
    void anIncompleteSubmitListsTheMissingStepsAndFields() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA);

        send(post("/solicitations/" + id + "/submit"), clientToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INCOMPLETE_SOLICITATION"))
                .andExpect(jsonPath("$.missingSteps[0]").value(2))
                .andExpect(jsonPath("$.missingSteps[1]").value(3))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='cep')].message").value(hasItem("required")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='priority')].message").value(hasItem("required")));
    }

    @Test
    void aValidationErrorDoesNotCarryMissingSteps() throws Exception {
        mockMvc.perform(post("/solicitations"))
                .andExpect(jsonPath("$.missingSteps").doesNotExist());
    }

    // CEP

    @Test
    void anUnknownCepIsAnInvalidCepError() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA);
        when(cepClient.findByCep("99999999")).thenReturn(Optional.empty());

        send(put("/solicitations/" + id + "/steps/2"), clientToken, address("99999999", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CEP"));
    }

    @Test
    void theCepServiceBeingDownIsAServiceUnavailableError() throws Exception {
        String clientToken = registerClientAndLogin();
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA);
        when(cepClient.findByCep("88888888"))
                .thenThrow(new CepServiceUnavailableException("ViaCEP is down", new RuntimeException()));

        send(put("/solicitations/" + id + "/steps/2"), clientToken, address("88888888", "10"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value("CEP_SERVICE_UNAVAILABLE"));
    }

    // 404 e erros do Spring MVC

    @Test
    void aMissingResourceIsANotFoundError() throws Exception {
        String clientToken = registerClientAndLogin();

        send(get("/solicitations/999999999"), clientToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Resource not found"))
                .andExpect(jsonPath("$.path").value("/solicitations/999999999"));
    }

    @Test
    void anUnknownRouteIsANotFoundErrorInTheStandardFormat() throws Exception {
        String clientToken = registerClientAndLogin();

        send(get("/solicitations/1/nao-existe"), clientToken)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(0)));
    }

    @Test
    void anUnsupportedMethodIsAMethodNotAllowedError() throws Exception {
        String clientToken = registerClientAndLogin();

        send(delete("/solicitations"), clientToken)
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.code").value("REQUEST_ERROR"))
                .andExpect(jsonPath("$.path").value("/solicitations"));
    }
}
