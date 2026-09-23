package edu.ku.bookapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Central OpenAPI 3 metadata. Endpoint-level details (summaries, parameters,
 * response codes) are documented directly on {@code BookController}.
 *
 * <p>Swagger UI: {@code /swagger-ui.html} &middot; OpenAPI JSON:
 * {@code /v3/api-docs}.</p>
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return the API-level OpenAPI definition (title, contact, license, servers)
     */
    @Bean
    public OpenAPI bookApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book Catalogue API")
                        .description("REST API for managing a library catalogue: create, read, "
                                + "update and delete books with validation, pagination and caching.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Enterprise Web Application Development - Kabul University")
                                .email("info@example.edu"))
                        .license(new License().name("MIT").identifier("MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.example.edu").description("Production (placeholder)")));
    }
}