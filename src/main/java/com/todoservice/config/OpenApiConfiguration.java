package com.todoservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI metadata configuration for API documentation endpoints.
 */
@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI todoServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Todo Service API")
                        .description("REST API for managing todo items with overdue enforcement and pagination")
                        .version("v1")
                        .license(new License().name("MIT")));
    }
}
