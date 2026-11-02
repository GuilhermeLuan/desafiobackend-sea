package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import com.jayway.jsonpath.JsonPath;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepClient;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep.CepResult;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ImportTestcontainers(TestcontainersConfigurations.class)
@TestPropertySource(properties = {
        "spring.docker.compose.enabled=false",
        "jwt.secret=integration-test-secret-integration-test-secret",
        "spring.flyway.placeholders.admin_email=" + BaseIntegrationTest.ADMIN_EMAIL,
        "spring.flyway.placeholders.admin_password=" + BaseIntegrationTest.ADMIN_PASSWORD
})
public abstract class BaseIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@integration.test";
    protected static final String ADMIN_PASSWORD = "admin-password-123";
    protected static final String PASSWORD = "password123";

    protected static final String SP_CEP = "01310100";
    protected static final String BSB_CEP = "70150900";

    @Autowired
    protected MockMvc mockMvc;

    @MockitoBean
    protected CepClient cepClient;

    protected String adminToken;

    @BeforeEach
    protected void setUp() throws Exception {
        when(cepClient.findByCep(SP_CEP))
                .thenReturn(Optional.of(new CepResult("Avenida Paulista", "Bela Vista", "São Paulo", "SP")));
        when(cepClient.findByCep(BSB_CEP))
                .thenReturn(Optional.of(new CepResult("Praça dos Três Poderes", "Zona Cívico-Administrativa", "Brasília", "DF")));

        adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected ResultActions send(MockHttpServletRequestBuilder request, String token) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token));
    }

    protected ResultActions send(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        return mockMvc.perform(request.header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected String login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    protected String registerClientAndLogin() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    protected String createAnalystAndLogin(String... ufs) throws Exception {
        String email = "analyst-" + UUID.randomUUID() + "@example.com";
        String coverage = "[\"" + String.join("\",\"", ufs) + "\"]";
        send(post("/admin/users"), adminToken,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ANALYST\",\"coverageUfs\":" + coverage + "}")
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    protected static final String BASIC_DATA = """
            {"serviceType":"INSTALLATION","title":"Instalar ar-condicionado",
             "description":"Instalação de um split na sala de estar do apartamento"}""";

    protected static final String CONFIRMATION = """
            {"priority":"MEDIUM","preferredDate":"2099-01-15","estimatedValue":250.00,"termsAccepted":true}""";

    protected static String address(String cep, String number) {
        return "{\"cep\":\"" + cep + "\",\"number\":\"" + number + "\"}";
    }

    protected long createDraft(String clientToken) throws Exception {
        String body = send(post("/solicitations"), clientToken)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected long submittedSolicitation(String clientToken, String cep) throws Exception {
        long id = createDraft(clientToken);
        send(put("/solicitations/" + id + "/steps/1"), clientToken, BASIC_DATA).andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/2"), clientToken, address(cep, "1578")).andExpect(status().isOk());
        send(put("/solicitations/" + id + "/steps/3"), clientToken, CONFIRMATION).andExpect(status().isOk());
        send(post("/solicitations/" + id + "/submit"), clientToken).andExpect(status().isOk());
        return id;
    }
}
