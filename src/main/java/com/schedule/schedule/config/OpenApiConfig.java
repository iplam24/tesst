package com.schedule.schedule.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI scheduleOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Schedule VNUA API")
                        .description("API Crawl Thời Khóa Biểu – VNUA")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Developer: Lâm đẹp trai 😘")
                                .email("contact@example.com")
                        ));
    }
}
