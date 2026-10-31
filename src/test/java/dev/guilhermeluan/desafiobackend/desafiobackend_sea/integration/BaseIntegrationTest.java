package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import com.jayway.jsonpath.JsonPath;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.CepClient;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.solicitation.CepResult;
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

    /**
     * Registra um CLIENT novo e devolve o token dele.
     */
    protected String registerClientAndLogin() throws Exception {
        String email = "client-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }

    /**
     * Cria (como ADMIN) um ANALYST novo que cobre as UFs dadas e devolve o token dele.
     */
    protected String createAnalystAndLogin(String... ufs) throws Exception {
        String email = "analyst-" + UUID.randomUUID() + "@example.com";
        String coverage = "[\"" + String.join("\",\"", ufs) + "\"]";
        send(post("/admin/users"), adminToken,
                "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ANALYST\",\"coverageUfs\":" + coverage + "}")
                .andExpect(status().isCreated());
        return login(email, PASSWORD);
    }
}
