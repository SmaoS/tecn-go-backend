package com.tecngo.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI openAPI() {
        String scheme = "bearerAuth";
        return new OpenAPI()
                .info(new Info().title("TecnGo API").version("v1")
                        .description("API del marketplace de servicios técnicos a domicilio"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .schemaRequirement(scheme, new SecurityScheme().name(scheme)
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
    }
}
