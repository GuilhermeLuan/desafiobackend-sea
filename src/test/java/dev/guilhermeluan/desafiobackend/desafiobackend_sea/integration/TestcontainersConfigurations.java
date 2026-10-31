package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Containers compartilhados pelos testes de integração. Os campos são estáticos, então o mesmo
 * Postgres e o mesmo Elasticsearch servem a todas as classes da suíte (sobem uma vez só).
 */
public final class TestcontainersConfigurations {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    // mesma imagem do compose.yaml; sem segurança (como lá) e com heap limitada para não pesar na máquina
    @ServiceConnection
    static final ElasticsearchContainer ELASTICSEARCH =
            new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:9.3.3")
                    .withEnv("xpack.security.enabled", "false")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    private TestcontainersConfigurations() {
    }
}
