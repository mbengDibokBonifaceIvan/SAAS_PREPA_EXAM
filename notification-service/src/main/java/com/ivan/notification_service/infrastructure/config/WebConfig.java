package com.ivan.notification_service.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = PageSerializationMode.VIA_DTO)
public class WebConfig { 
    // Cette configuration garantit que vos objets Page (Spring Data) 
    // seront transformés en un JSON stable et standard pour le Front-end.
}