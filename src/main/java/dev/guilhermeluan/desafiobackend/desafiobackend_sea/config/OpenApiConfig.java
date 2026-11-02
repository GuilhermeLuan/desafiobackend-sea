package dev.guilhermeluan.desafiobackend.desafiobackend_sea.config;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.exception.ApiError;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Desafio Backend SEA — API de Solicitações de Atendimento")
                        .version("1.0")
                        .description("Para testar pelo Swagger UI: faça o login em **POST /auth/login**, copie o token e use o "
                                + "botão **Authorize**. O ADMIN inicial é o do arquivo .env."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    @Bean
    public OpenApiCustomizer standardErrors() {
        return openApi -> {
            ModelConverters.getInstance().readAll(ApiError.class).forEach(openApi.getComponents()::addSchemas);

            openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
                boolean publicRoute = path.startsWith("/auth/");
                if (publicRoute) {
                    operation.setSecurity(List.of());
                } else {
                    operation.getResponses().addApiResponse("401", error("Sem token, ou token inválido ou expirado"));
                    operation.getResponses().addApiResponse("403", error("O seu perfil não pode usar esta rota"));
                }
                operation.getResponses().addApiResponse("500", error("Erro inesperado (sem detalhes internos)"));
            }));
        };
    }

    private static ApiResponse error(String description) {
        Schema<?> apiError = new Schema<>().$ref("#/components/schemas/ApiError");
        return new ApiResponse().description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(apiError)));
    }
}
