package com.emeraldgrove.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI emeraldGroveOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Emerald Grove API")
                .description("REST API документация для Emerald Grove.")
                .version("v1")
                .contact(new Contact().name("Emerald Grove Team"))
                .license(new License().name("Internal use")));
    }
}