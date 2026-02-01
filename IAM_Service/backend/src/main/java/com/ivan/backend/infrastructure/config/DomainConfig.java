package com.ivan.backend.infrastructure.config;

import com.ivan.backend.domain.repository.UserRepository;
import com.ivan.backend.domain.service.OnboardingDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public OnboardingDomainService onboardingDomainService(UserRepository userRepository) {
        return new OnboardingDomainService(userRepository);
    }
}