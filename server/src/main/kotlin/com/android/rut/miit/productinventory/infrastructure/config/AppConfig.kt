package com.android.rut.miit.productinventory.infrastructure.config

import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {

    @Bean
    fun expirationCheckService(): ExpirationCheckService = ExpirationCheckService()
}
