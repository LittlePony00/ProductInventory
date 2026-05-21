package com.android.rut.miit.productinventory.application.service.recommendation

import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.service.RecipeRetriever
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

interface RecipeCandidateProvider {
    fun findCandidates(context: RecommendationContext): List<RecipeCandidate>
    fun findAnyCandidates(context: RecommendationContext): List<RecipeCandidate> = emptyList()
}

@Component
class LocalRecipeCandidateProvider(
    private val recipeRetriever: RecipeRetriever,
    @param:Value("\${local-recipes.enabled:false}") private val enabled: Boolean
) : RecipeCandidateProvider {

    override fun findCandidates(context: RecommendationContext): List<RecipeCandidate> =
        if (!enabled) {
            emptyList()
        } else {
            recipeRetriever.retrieve(context.candidateProducts, limit = CANDIDATE_LIMIT)
                .map(RecipeDocumentMatch::toCandidate)
        }

    override fun findAnyCandidates(context: RecommendationContext): List<RecipeCandidate> =
        if (!enabled) {
            emptyList()
        } else {
            recipeRetriever.retrieveAny(limit = CANDIDATE_LIMIT)
                .map(RecipeDocumentMatch::toCandidate)
        }

    private companion object {
        const val CANDIDATE_LIMIT = 12
    }
}

data class RecipeCandidate(
    val document: RecipeDocument,
    val match: RecipeDocumentMatch
)

private fun RecipeDocumentMatch.toCandidate(): RecipeCandidate =
    RecipeCandidate(document = document, match = this)
