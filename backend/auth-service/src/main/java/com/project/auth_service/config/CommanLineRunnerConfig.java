package com.project.auth_service.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommanLineRunnerConfig {
    @Autowired
    DataInitializer dataInitializer;

    @Bean
    CommandLineRunner seed() {
        return args -> dataInitializer.seedData();
    }
}
