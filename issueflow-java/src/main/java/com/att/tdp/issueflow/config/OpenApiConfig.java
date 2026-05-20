package com.att.tdp.issueflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Springdoc OpenAPI (Swagger UI).
 * <p>
 * Accessible at: {@code http://localhost:8080/swagger-ui.html}
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI issueFlowOpenApi() {
        return new OpenAPI()
                .info(new Info().title("IssueFlow API")
                        .description("Backend API for the IssueFlow ticket management system (TDP 2026).")
                        .version("v1.0.0")
                        .contact(new Contact().name("Backend Team").url("https://github.com/rontzvick1/TDP-2026-Home-Assignment"))
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                
                // Add global security requirement (applies to all endpoints)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                
                // Define the security scheme (JWT Bearer token)
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")));
    }
}
