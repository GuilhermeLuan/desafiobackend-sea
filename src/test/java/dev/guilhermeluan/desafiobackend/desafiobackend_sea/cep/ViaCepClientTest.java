package dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class ViaCepClientTest {

    private static final String CEP = "22785275";
    private static final String PATH = "/ws/" + CEP + "/json/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(300);

    private static final String REAL_RESPONSE = """
            {"cep":"22785-275","logradouro":"Estrada dos Bandeirantes","complemento":"de 25001 a 26649 - lado ímpar",
             "unidade":"","bairro":"Vargem Grande","localidade":"Rio de Janeiro","uf":"RJ","estado":"Rio de Janeiro",
             "regiao":"Sudeste","ibge":"3304557","gia":"","ddd":"21","siafi":"6001"}""";

    @RegisterExtension
    WireMockExtension viaCep = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private ViaCepClient client;

    @BeforeEach
    void setUp() {
        client = new ViaCepClient(RestClient.builder(), viaCep.baseUrl() + "/ws", TIMEOUT);
    }

    @Test
    void anExistingCepBecomesTheAddressAndAsksViaCepForTheRightPath() {
        viaCep.stubFor(get(urlEqualTo(PATH)).willReturn(okJson(REAL_RESPONSE)));

        Optional<CepResult> result = client.findByCep(CEP);

        assertEquals(Optional.of(new CepResult("Estrada dos Bandeirantes", "Vargem Grande", "Rio de Janeiro", "RJ")), result);
        viaCep.verify(1, getRequestedFor(urlEqualTo(PATH)));
    }

    @Test
    void anUnknownCepComesBackAsEmpty() {
        viaCep.stubFor(get(urlEqualTo(PATH)).willReturn(okJson("{\"erro\": true}")));

        assertEquals(Optional.empty(), client.findByCep(CEP));
    }

    @Test
    void aBadRequestFromViaCepComesBackAsEmpty() {
        viaCep.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(400)));

        assertEquals(Optional.empty(), client.findByCep(CEP));
    }

    @Test
    void aServerErrorIsReportedAsServiceUnavailable() {
        viaCep.stubFor(get(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        assertThrows(CepServiceUnavailableException.class, () -> client.findByCep(CEP));
    }

    @Test
    void aSlowViaCepIsCutOffByTheTimeoutInsteadOfHoldingTheCaller() {
        viaCep.stubFor(get(urlEqualTo(PATH)).willReturn(okJson(REAL_RESPONSE).withFixedDelay(5000)));
        ViaCepClient impatientClient = new ViaCepClient(RestClient.builder(), viaCep.baseUrl() + "/ws", SHORT_TIMEOUT);

        long start = System.nanoTime();
        assertThrows(CepServiceUnavailableException.class, () -> impatientClient.findByCep(CEP));
        Duration waited = Duration.ofNanos(System.nanoTime() - start);

        assertTrue(waited.compareTo(Duration.ofSeconds(4)) < 0, "esperou " + waited.toMillis() + " ms; o timeout não cortou a chamada");
    }

    @Test
    void aViaCepThatIsDownIsReportedAsServiceUnavailable() {
        viaCep.shutdownServer();

        assertThrows(CepServiceUnavailableException.class, () -> client.findByCep(CEP));
    }
}
