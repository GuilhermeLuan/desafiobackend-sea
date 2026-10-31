package dev.guilhermeluan.desafiobackend.desafiobackend_sea.cep;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Optional;

@Component
public class ViaCepClient implements CepClient {

    private final RestClient restClient;

    @Autowired
    public ViaCepClient(RestClient.Builder builder,
                        @Value("${viacep.base-url:https://viacep.com.br/ws}") String baseUrl,
                        @Value("${viacep.timeout:3s}") Duration timeout) {
        this(builder.baseUrl(baseUrl).requestFactory(requestFactory(timeout)).build());
    }

    ViaCepClient(RestClient restClient) {
        this.restClient = restClient;
    }

    private static SimpleClientHttpRequestFactory requestFactory(Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return factory;
    }

    @Override
    public Optional<CepResult> findByCep(String cep) {
        ViaCepResponse response;
        try {
            response = restClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponse.class);
        } catch (HttpClientErrorException.BadRequest e) {
            // ViaCEP responde 400 para CEP malformado
            return Optional.empty();
        } catch (RestClientException e) {
            throw new CepServiceUnavailableException("ViaCEP lookup failed", e);
        }

        if (response == null) {
            throw new CepServiceUnavailableException("ViaCEP returned an empty response", null);
        }
        if (Boolean.TRUE.equals(response.erro())) {
            return Optional.empty();
        }
        return Optional.of(new CepResult(response.logradouro(), response.bairro(), response.localidade(), response.uf()));
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViaCepResponse(String logradouro, String bairro, String localidade, String uf, Boolean erro) {
    }
}
