package com.android.rut.miit.productinventory.infrastructure.config

import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Bean
    fun expirationCheckService(): ExpirationCheckService = ExpirationCheckService()

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
