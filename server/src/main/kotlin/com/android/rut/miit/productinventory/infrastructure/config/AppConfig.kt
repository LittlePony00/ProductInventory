package com.android.rut.miit.productinventory.infrastructure.config

import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import com.android.rut.miit.productinventory.domain.port.outbound.IRecipeKnowledgeRepository
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {

    @Bean
    fun expirationCheckService(): ExpirationCheckService = ExpirationCheckService()

    @Bean
    fun recipeRetriever(recipeKnowledgeRepository: IRecipeKnowledgeRepository): RecipeRetriever =
        RecipeRetriever(recipeKnowledgeRepository)

    @Bean
    fun restTemplate(): RestTemplate = RestTemplate()
}
