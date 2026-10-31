package dev.guilhermeluan.desafiobackend.desafiobackend_sea.integration;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

public final class TestcontainersConfigurations {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18");

    private TestcontainersConfigurations() {
    }
}
