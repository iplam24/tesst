// src/main/java/com/schedule/schedule/config/CorsConfig.java
package com.schedule.schedule.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://10.0.2.2:8080", "http://localhost:8080", "https://tesst-production-38e3.up.railway.app",
                                "https://www.chayproject.online",              // Domain riêng
                                "https://chayproject.online"
                                ,"https://schedulefea.fly.dev","*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(false); // QUAN TRỌNG: ĐẶT FALSE!!!
            }
        };
    }
}