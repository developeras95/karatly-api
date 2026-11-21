package com.karatly.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI karatlyOpenApi(@Value("${karatly.api.version:0.1.0}") String version) {
        return new OpenAPI().info(new Info()
                .title("Karatly API")
                .version(version)
                .description("""
                        Precious metal holdings, purchases, sales and true-profit analysis.
                        Track gold, silver, platinum and palladium in every form you hold them."""));
    }
}
