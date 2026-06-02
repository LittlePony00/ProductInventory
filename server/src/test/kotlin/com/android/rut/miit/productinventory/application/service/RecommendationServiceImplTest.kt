package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.AccessDeniedException
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Product
import com.android.rut.miit.productinventory.domain.model.ProductCategory
import com.android.rut.miit.productinventory.domain.model.Quantity
import com.android.rut.miit.productinventory.domain.model.QuantityUnit
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationContext
import com.android.rut.miit.productinventory.domain.model.AiRecipeGenerationRequest
import com.android.rut.miit.productinventory.domain.model.RecommendationMode
import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeCandidate
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeCandidateProvider
import com.android.rut.miit.productinventory.application.service.recommendation.RecipeSafetyFilter
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContext
import com.android.rut.miit.productinventory.application.service.recommendation.RecommendationContextBuilder
import com.android.rut.miit.productinventory.domain.model.Recipe
import com.android.rut.miit.productinventory.domain.model.RecipeDiscoveryResult
import com.android.rut.miit.productinventory.domain.model.RecipeDocument
import com.android.rut.miit.productinventory.domain.model.RecipeDocumentMatch
import com.android.rut.miit.productinventory.domain.model.RecipeIngredient
import com.android.rut.miit.productinventory.domain.model.RecipeSearchRequest
import com.android.rut.miit.productinventory.domain.model.RecipeSource
import com.android.rut.miit.productinventory.domain.model.SystemCategoryCatalog
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeGenerator
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeLocalizer
import com.android.rut.miit.productinventory.domain.port.outbound.IAiRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IExternalRecipeSearchProvider
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IProductRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IUserFoodPreferencesRepository
import com.android.rut.miit.productinventory.domain.service.ExpirationCheckService
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RecommendationServiceImplTest {

    @Test
    fun `passes expiration-prioritized product context to recipe provider`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Внешний рецепт"))
        val service = service(
            productRepository = FakeProductRepository(
                listOf(
                    product(name = "Rice", householdId = householdId, expirationDate = LocalDate.now().plusDays(7)),
                    product(name = "Yogurt", householdId = householdId, expirationDate = LocalDate.now().plusDays(1))
                )
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider)
        )

        service.getRecipes(userId, householdId)

        assertEquals(listOf("Yogurt", "Rice"), externalProvider.contexts.single().candidateProducts.map { it.name })
    }

    @Test
    fun `uses only remaining products for recipe provider context`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Внешний рецепт"))
        val service = service(
            productRepository = FakeProductRepository(
                listOf(
                    product(name = "Rice", householdId = householdId, remainingAmount = 2.0),
                    product(name = "Empty Yogurt", householdId = householdId, remainingAmount = 0.0)
                )
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider)
        )

        service.getRecipes(userId, householdId)

        assertEquals(listOf("Rice"), externalProvider.contexts.single().candidateProducts.map { it.name })
    }

    @Test
    fun `rejects users outside household`() {
        val service = service(
            productRepository = FakeProductRepository(emptyList()),
            membershipRepository = FakeMembershipRepository(emptyList()),
            candidateProvider = RecordingCandidateProvider()
        )

        assertFailsWith<AccessDeniedException> {
            service.getRecipes(UUID.randomUUID(), UUID.randomUUID())
        }
    }

    @Test
    fun `use soon passes only expiring products to external providers`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Внешний рецепт"))
        val service = service(
            productRepository = FakeProductRepository(
                listOf(
                    product(name = "Rice", householdId = householdId, expirationDate = LocalDate.now().plusDays(7)),
                    product(name = "Yogurt", householdId = householdId, expirationDate = LocalDate.now().plusDays(1))
                )
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider)
        )

        service.getRecipes(userId, householdId, RecommendationMode.USE_SOON)

        assertEquals(listOf("Yogurt"), externalProvider.contexts.single().candidateProducts.map { it.name })
    }

    @Test
    fun `get recipes uses external services when local knowledge base has no result`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(
                listOf(product(name = "Рис", householdId = householdId))
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(FixedRecipeSearchProvider(externalRecipe("Внешний рис")))
        )

        val recipes = service.getRecipes(userId, householdId)

        assertEquals(listOf("Внешний рис"), recipes.map { it.title })
        assertEquals(RecipeSource.EXTERNAL_API, recipes.single().source)
    }

    @Test
    fun `rejects ai generated recipe when avoided category appears only in generated ingredients`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(
                listOf(product(name = "Rice", householdId = householdId))
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            preferencesRepository = FakeFoodPreferencesRepository(
                UserFoodPreferences(userId = userId, avoidedCategoryIds = setOf(SystemCategoryCatalog.dairyId))
            ),
            aiRecipeGenerator = FixedAiRecipeGenerator(
                Recipe(
                    title = "Milk rice",
                    ingredients = listOf(RecipeIngredient("milk", "1 cup")),
                    steps = listOf("Cook"),
                    time = "10 minutes",
                    calories = 120
                )
            )
        )

        assertFailsWith<IllegalArgumentException> {
            service.generateAiRecipe(userId, householdId, AiRecipeGenerationRequest())
        }
    }

    @Test
    fun `saved free-form product preferences are reloaded and applied to external recipe safety`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val tomato = product(name = "Томаты", householdId = householdId)
        val potato = product(name = "Картофель", householdId = householdId)
        val shrimp = product(name = "Креветки", householdId = householdId)
        val preferencesRepository = SavingFoodPreferencesRepository().apply {
            save(
                UserFoodPreferences(
                    userId = userId,
                    preferredProducts = setOf("томат"),
                    avoidedProducts = setOf("кревет")
                )
            )
        }
        val service = service(
            productRepository = FakeProductRepository(listOf(tomato, potato, shrimp)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("Салат с томатами", "томаты"),
                    externalRecipe("Печёный картофель", "картофель"),
                    externalRecipe("Рис с креветками", "креветки")
                )
            ),
            preferencesRepository = preferencesRepository
        )

        val recipes = service.getRecipes(userId, householdId)

        assertEquals(setOf("Салат с томатами", "Печёный картофель"), recipes.map { it.title }.toSet())
    }

    @Test
    fun `selected recipe search limits candidate products to selected ids`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val potato = product(name = "Картофель", householdId = householdId)
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Внешний картофель"))
        val service = service(
            productRepository = FakeProductRepository(listOf(rice, potato)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider)
        )

        service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(potato.id)))

        assertEquals(listOf("Картофель"), externalProvider.contexts.single().candidateProducts.map { it.name })
    }

    @Test
    fun `random recipe search is not constrained to current products`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val potato = product(name = "Картофель", householdId = householdId)
        val milk = product(name = "Молоко", householdId = householdId)
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Случайный рецепт"))
        val service = service(
            productRepository = FakeProductRepository(listOf(rice, potato, milk)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider),
            preferencesRepository = FakeFoodPreferencesRepository(
                UserFoodPreferences(userId = userId, avoidedProductIds = setOf(milk.id))
            )
        )

        service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(emptyList(), externalProvider.contexts)
        assertEquals(true, externalProvider.randomContexts.single().searchesAnyProducts)
        assertEquals(emptyList(), externalProvider.randomContexts.single().candidateProducts)
    }

    @Test
    fun `random recipe search ignores local recipes when household has no products`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(emptyList()),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = StaticCandidateProvider(
                listOf(candidate(title = "Случайный плов", requiredIngredient = "рис"))
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(emptyList(), recipes)
    }

    @Test
    fun `random recipe search returns multiple recipes`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(emptyList()),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("Случайный плов", "рис"),
                    externalRecipe("Случайная паста", "паста"),
                    externalRecipe("Случайный салат", "овощи")
                )
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(setOf("Случайный плов", "Случайная паста", "Случайный салат"), recipes.map { it.title }.toSet())
    }


    @Test
    fun `find recipes uses external services after local search is empty before creating recipe`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val aiRecipeGenerator = RecordingAiRecipeGenerator(
            Recipe(
                title = "Созданный рецепт",
                ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                steps = listOf("Сварить рис"),
                time = "20 минут",
                calories = 210
            )
        )
        val service = service(
            productRepository = FakeProductRepository(
                listOf(product(name = "Рис", householdId = householdId))
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            aiRecipeGenerator = aiRecipeGenerator,
            externalRecipeSearchProviders = listOf(FixedRecipeSearchProvider(externalRecipe("Внешний рисовый боул")))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(1, recipes.size)
        assertEquals("Внешний рисовый боул", recipes.single().title)
        assertEquals(RecipeSource.EXTERNAL_API, recipes.single().source)
        assertEquals(false, recipes.single().aiAssisted)
        assertEquals(false, recipes.single().aiGenerated)
        assertEquals(0, aiRecipeGenerator.calls)
    }

    @Test
    fun `find recipes includes ai-assisted website search results in parallel with external results`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val service = service(
            productRepository = FakeProductRepository(listOf(rice)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(FixedRecipeSearchProvider(externalRecipe("Внешний рис"))),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(externalRecipe("ИИ рис"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(rice.id)))

        assertEquals(setOf("Внешний рис", "ИИ рис"), recipes.map { it.title }.toSet())
        assertEquals(RecipeSource.AI_ASSISTED, recipes.first { it.title == "ИИ рис" }.source)
        assertEquals(true, recipes.first { it.title == "ИИ рис" }.aiAssisted)
        assertEquals(false, recipes.first { it.title == "ИИ рис" }.aiGenerated)
    }

    @Test
    fun `find recipes queries every external provider and merges their results with ai-assisted`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val chicken = product(name = "Курица", householdId = householdId)
        val russianProvider = RecordingRecipeSearchProvider(
            externalRecipe("Русский рецепт с курицей", "курица").copy(requiresLocalization = false)
        )
        val translatedProvider = RecordingRecipeSearchProvider(externalRecipe("Chicken stew", "chicken"))
        val service = service(
            productRepository = FakeProductRepository(listOf(chicken)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(russianProvider, translatedProvider),
            aiRecipeLocalizer = FixedRecipeLocalizer(
                Recipe(
                    title = "Переведённое рагу с курицей",
                    ingredients = listOf(RecipeIngredient("курица", "300 г")),
                    steps = listOf("Потушить курицу"),
                    time = "35 минут",
                    calories = 320
                )
            ),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(aiAssistedRecipe("Подборка с курицей", "курица"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(chicken.id)))

        assertEquals(1, russianProvider.calls)
        assertEquals(1, translatedProvider.calls)
        assertEquals(
            setOf("Русский рецепт с курицей", "Переведённое рагу с курицей", "Подборка с курицей"),
            recipes.map { it.title }.toSet()
        )
        assertEquals(2, recipes.count { it.source == RecipeSource.EXTERNAL_API })
        assertEquals(1, recipes.count { it.aiAssisted })
    }

    @Test
    fun `find recipes treats chicken product as used by Russian chicken ingredient names`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val chicken = product(name = "Курица", householdId = householdId)
        val service = service(
            productRepository = FakeProductRepository(listOf(chicken)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("Салат с курицей", "Куриное мясо отварное")
                        .copy(requiresLocalization = false)
                )
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(chicken.id)))

        assertEquals(listOf("Курица"), recipes.single().usedHouseholdProducts)
        assertTrue("Куриное мясо отварное" !in recipes.single().missingIngredients)
    }

    @Test
    fun `find recipes includes ai-assisted result even when deduped source results fill the limit`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val service = service(
            productRepository = FakeProductRepository(listOf(rice)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    *(1..6).map { index -> externalRecipe("Внешний рис $index") }.toTypedArray()
                )
            ),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(externalRecipe("ИИ рис"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(rice.id)))

        assertEquals(6, recipes.size)
        assertTrue(recipes.any { it.aiAssisted && it.title == "ИИ рис" })
    }

    @Test
    fun `random recipe search keeps external recipes visible alongside ai-assisted recipes`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val aiRecipeGenerator = RecordingAiRecipeGenerator(
            Recipe(
                title = "Сгенерированный запасной рецепт",
                ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                steps = listOf("Сварить рис"),
                time = "20 минут",
                calories = 210
            )
        )
        val service = service(
            productRepository = FakeProductRepository(emptyList()),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            aiRecipeGenerator = aiRecipeGenerator,
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("TheMealDB рис"),
                    externalRecipe("TheMealDB паста"),
                    externalRecipe("TheMealDB суп")
                )
            ),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(
                aiAssistedRecipe("AI рис 1"),
                aiAssistedRecipe("AI рис 2"),
                aiAssistedRecipe("AI рис 3"),
                aiAssistedRecipe("AI рис 4"),
                aiAssistedRecipe("AI рис 5"),
                aiAssistedRecipe("AI рис 6")
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(6, recipes.size)
        assertTrue(recipes.any { it.source == RecipeSource.EXTERNAL_API })
        assertTrue(recipes.any { it.aiAssisted })
        assertEquals(0, aiRecipeGenerator.calls)
    }

    @Test
    fun `find recipes keeps external duplicate and still includes ai-assisted when external exists`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val externalProvider = RecordingRecipeSearchProvider(externalRecipe("Внешний рис"))
        val service = service(
            productRepository = FakeProductRepository(listOf(rice)),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(externalProvider),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(aiAssistedRecipe("Рисовый боул"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(setOf("Рисовый боул", "Внешний рис"), recipes.map { it.title }.toSet())
        assertEquals(1, recipes.count { it.title == "Рисовый боул" })
        assertEquals(RecipeSource.AI_ASSISTED, recipes.single { it.title == "Рисовый боул" }.source)
        assertEquals(true, recipes.single { it.title == "Рисовый боул" }.aiAssisted)
        assertEquals(1, externalProvider.calls)
    }

    @Test
    fun `find recipes localizes external found recipes and fills missing metadata`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(listOf(product(name = "Рис", householdId = householdId))),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(FixedRecipeSearchProvider(externalRecipe("Chicken Rice"))),
            aiRecipeLocalizer = FixedRecipeLocalizer(
                Recipe(
                    title = "Курица с рисом",
                    ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                    steps = listOf("Приготовить рис"),
                    time = "25 минут",
                    calories = 320
                )
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = emptySet()))

        assertEquals("Курица с рисом", recipes.single().title)
        assertEquals(listOf(RecipeIngredient("рис", "1 стакан")), recipes.single().ingredients)
        assertEquals(listOf("Приготовить рис"), recipes.single().steps)
        assertEquals("25 минут", recipes.single().time)
        assertEquals(320, recipes.single().calories)
        assertEquals(true, recipes.single().caloriesKnown)
    }

    @Test
    fun `find recipes keeps Russian external recipes without AI localization`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val localizer = RecordingRecipeLocalizer()
        val service = service(
            productRepository = FakeProductRepository(listOf(product(name = "Курица", householdId = householdId))),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("Салат с курицей", "Куриная грудка").copy(requiresLocalization = false)
                )
            ),
            aiRecipeLocalizer = localizer
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals("Салат с курицей", recipes.single().title)
        assertEquals(0, localizer.batchCalls)
    }

    @Test
    fun `find recipes does not show raw external recipes when batch localization is partial`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(listOf(product(name = "Рис", householdId = householdId))),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(
                FixedRecipeSearchProvider(
                    externalRecipe("Chicken Rice"),
                    externalRecipe("Pasta Soup")
                )
            ),
            aiRecipeLocalizer = PartialRecipeLocalizer(
                listOf(
                    Recipe(
                        title = "Курица с рисом",
                        ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                        steps = listOf("Приготовить рис"),
                        time = "25 минут",
                        calories = 320
                    ),
                    null
                )
            ),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(
                aiAssistedRecipe("AI рис 1"),
                aiAssistedRecipe("AI рис 2"),
                aiAssistedRecipe("AI рис 3"),
                aiAssistedRecipe("AI рис 4"),
                aiAssistedRecipe("AI рис 5"),
                aiAssistedRecipe("AI рис 6")
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertTrue(recipes.any { it.source == RecipeSource.EXTERNAL_API && it.title == "Курица с рисом" })
        assertTrue(recipes.none { it.source == RecipeSource.EXTERNAL_API && it.title == "Pasta Soup" })
        assertTrue(recipes.any { it.source == RecipeSource.AI_ASSISTED })
    }

    @Test
    fun `find recipes keeps external result over duplicate ai-assisted result when local is empty`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(listOf(product(name = "Рис", householdId = householdId))),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            externalRecipeSearchProviders = listOf(FixedRecipeSearchProvider(externalRecipe("Общий рисовый боул"))),
            aiRecipeSearchProvider = FixedAiRecipeSearchProvider(externalRecipe("Общий рисовый боул"))
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(1, recipes.size)
        assertEquals("Общий рисовый боул", recipes.single().title)
        assertEquals(RecipeSource.EXTERNAL_API, recipes.single().source)
        assertEquals(false, recipes.single().aiAssisted)
    }

    @Test
    fun `find recipes creates ai recipe only when local external and ai-assisted search find nothing`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val rice = product(name = "Рис", householdId = householdId)
        val aiRecipeGenerator = RecordingAiRecipeGenerator(
            Recipe(
                title = "Созданная рисовая каша",
                ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                steps = listOf("Сварить рис"),
                time = "20 минут",
                calories = 210
            )
        )
        val service = service(
            productRepository = FakeProductRepository(
                listOf(rice)
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            aiRecipeGenerator = aiRecipeGenerator
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest(selectedProductIds = setOf(rice.id)))

        assertEquals(1, recipes.size)
        assertEquals(RecipeSource.AI_GENERATED, recipes.single().source)
        assertEquals(false, recipes.single().aiAssisted)
        assertEquals(true, recipes.single().aiGenerated)
        assertEquals(listOf("Рис"), recipes.single().usedHouseholdProducts)
        assertEquals(listOf("Рис"), aiRecipeGenerator.contexts.single().products.map { it.name })
    }

    @Test
    fun `random search creates ai-generated fallback when discovery returns nothing`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val aiRecipeGenerator = RecordingAiRecipeGenerator(
            Recipe(
                title = "Один случайный рецепт",
                ingredients = listOf(RecipeIngredient("рис", "1 стакан")),
                steps = listOf("Сварить рис"),
                time = "20 минут",
                calories = 210
            )
        )
        val service = service(
            productRepository = FakeProductRepository(
                listOf(product(name = "Рис", householdId = householdId))
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            aiRecipeGenerator = aiRecipeGenerator
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertEquals(1, recipes.size)
        assertEquals(RecipeSource.AI_GENERATED, recipes.single().source)
        assertEquals(true, recipes.single().aiGenerated)
        assertEquals(emptyList(), recipes.single().usedHouseholdProducts)
        assertEquals(1, aiRecipeGenerator.calls)
        assertEquals(emptyList(), aiRecipeGenerator.contexts.single().products)
    }

    @Test
    fun `find recipes drops unsafe created fallback`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = service(
            productRepository = FakeProductRepository(
                listOf(product(name = "Рис", householdId = householdId))
            ),
            membershipRepository = FakeMembershipRepository(
                listOf(Membership(userId = userId, householdId = householdId, role = MembershipRole.OWNER))
            ),
            candidateProvider = RecordingCandidateProvider(),
            preferencesRepository = FakeFoodPreferencesRepository(
                UserFoodPreferences(userId = userId, avoidedCategoryIds = setOf(SystemCategoryCatalog.dairyId))
            ),
            aiRecipeGenerator = FixedAiRecipeGenerator(
                Recipe(
                    title = "Молочная рисовая каша",
                    ingredients = listOf(RecipeIngredient("milk", "1 cup")),
                    steps = listOf("Cook"),
                    time = "20 minutes",
                    calories = 240
                )
            )
        )

        val recipes = service.findRecipes(userId, householdId, RecipeSearchRequest())

        assertTrue(recipes.isEmpty())
    }

    private fun product(
        name: String,
        householdId: UUID,
        expirationDate: LocalDate = LocalDate.now().plusDays(7),
        remainingAmount: Double = 1.0
    ): Product =
        Product(
            name = name,
            category = ProductCategory.OTHER,
            quantity = Quantity(1.0, QuantityUnit.PIECES),
            remainingAmount = remainingAmount,
            expirationDate = com.android.rut.miit.productinventory.domain.model.ExpirationDate(expirationDate),
            householdId = householdId,
            addedByUserId = UUID.randomUUID()
        )

    private fun service(
        productRepository: IProductRepository,
        membershipRepository: IMembershipRepository,
        candidateProvider: RecipeCandidateProvider,
        preferencesRepository: IUserFoodPreferencesRepository = FakeFoodPreferencesRepository(),
        aiRecipeGenerator: IAiRecipeGenerator = NoopAiRecipeGenerator(),
        externalRecipeSearchProviders: List<IExternalRecipeSearchProvider> = emptyList(),
        aiRecipeSearchProvider: IAiRecipeSearchProvider? = null,
        aiRecipeLocalizer: IAiRecipeLocalizer? = PassThroughRecipeLocalizer()
    ): RecommendationServiceImpl =
        RecommendationServiceImpl(
            contextBuilder = RecommendationContextBuilder(
                productRepository = productRepository,
                membershipRepository = membershipRepository,
                preferencesRepository = preferencesRepository,
                expirationCheckService = ExpirationCheckService()
            ),
            safetyFilter = RecipeSafetyFilter(),
            aiRecipeGenerator = aiRecipeGenerator,
            externalRecipeSearchProviders = externalRecipeSearchProviders,
            aiRecipeSearchProvider = aiRecipeSearchProvider,
            aiRecipeLocalizer = aiRecipeLocalizer
        )

    private class RecordingCandidateProvider : RecipeCandidateProvider {
        val contexts = mutableListOf<RecommendationContext>()
        val randomContexts = mutableListOf<RecommendationContext>()

        override fun findCandidates(context: RecommendationContext): List<RecipeCandidate> {
            contexts += context
            return emptyList()
        }

        override fun findAnyCandidates(context: RecommendationContext): List<RecipeCandidate> {
            randomContexts += context
            return emptyList()
        }
    }

    private class StaticCandidateProvider(
        private val candidates: List<RecipeCandidate>
    ) : RecipeCandidateProvider {
        override fun findCandidates(context: RecommendationContext): List<RecipeCandidate> = candidates
        override fun findAnyCandidates(context: RecommendationContext): List<RecipeCandidate> = candidates
    }

    private class NoopAiRecipeGenerator : IAiRecipeGenerator {
        override fun generateRecipe(context: AiRecipeGenerationContext): Recipe? = null
    }

    private class FixedAiRecipeGenerator(private val recipe: Recipe) : IAiRecipeGenerator {
        override fun generateRecipe(context: AiRecipeGenerationContext): Recipe = recipe
    }

    private class RecordingAiRecipeGenerator(private val recipe: Recipe) : IAiRecipeGenerator {
        var calls = 0
        val contexts = mutableListOf<AiRecipeGenerationContext>()
        override fun generateRecipe(context: AiRecipeGenerationContext): Recipe {
            calls += 1
            contexts += context
            return recipe
        }
    }

    private class FixedRecipeSearchProvider(private vararg val recipes: RecipeDiscoveryResult) : IExternalRecipeSearchProvider {
        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> = recipes.toList()
        override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> = recipes.toList()
    }

    private class RecordingRecipeSearchProvider(private val recipe: RecipeDiscoveryResult) : IExternalRecipeSearchProvider {
        var calls = 0
        val contexts = mutableListOf<RecommendationContext>()
        val randomContexts = mutableListOf<RecommendationContext>()

        override fun searchRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
            calls += 1
            contexts += context
            return listOf(recipe)
        }

        override fun searchRandomRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> {
            calls += 1
            randomContexts += context
            return listOf(recipe)
        }
    }

    private class FixedAiRecipeSearchProvider(private vararg val recipes: RecipeDiscoveryResult) : IAiRecipeSearchProvider {
        override fun searchWebRecipes(context: RecommendationContext): List<RecipeDiscoveryResult> = recipes.toList()
    }

    private class FixedRecipeLocalizer(private val recipe: Recipe) : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe =
            this.recipe
    }

    private class PartialRecipeLocalizer(private val recipes: List<Recipe?>) : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe? = recipes.firstOrNull()
        override fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> =
            this.recipes
    }

    private class PassThroughRecipeLocalizer : IAiRecipeLocalizer {
        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe =
            recipe
    }

    private class RecordingRecipeLocalizer : IAiRecipeLocalizer {
        var batchCalls = 0

        override fun localizeAndEnrichFoundRecipe(recipe: Recipe): Recipe {
            error("Single localization must not be called")
        }

        override fun localizeAndEnrichFoundRecipes(recipes: List<Recipe>): List<Recipe?> {
            batchCalls += 1
            return recipes
        }
    }

    private class FakeFoodPreferencesRepository(
        private val preferences: UserFoodPreferences? = null
    ) : IUserFoodPreferencesRepository {
        override fun findByUserId(userId: UUID): UserFoodPreferences? = preferences
        override fun save(preferences: UserFoodPreferences): UserFoodPreferences = preferences
    }

    private class SavingFoodPreferencesRepository : IUserFoodPreferencesRepository {
        private val preferencesByUserId = mutableMapOf<UUID, UserFoodPreferences>()
        override fun findByUserId(userId: UUID): UserFoodPreferences? = preferencesByUserId[userId]
        override fun save(preferences: UserFoodPreferences): UserFoodPreferences {
            preferencesByUserId[preferences.userId] = preferences
            return preferences
        }
    }

    private fun candidate(
        title: String,
        requiredIngredient: String,
        product: Product? = null
    ): RecipeCandidate {
        val document = RecipeDocument(
            id = title,
            title = title,
            ingredients = listOf(RecipeIngredient(requiredIngredient, "1 порция")),
            steps = listOf("Приготовить"),
            time = "10 минут",
            calories = 100,
            requiredIngredients = setOf(requiredIngredient),
            categories = emptySet(),
            rules = emptyList()
        )
        return RecipeCandidate(
            document = document,
            match = RecipeDocumentMatch(
                document = document,
                score = 1.0,
                matchedProducts = listOfNotNull(product),
                appliedRules = emptyList()
            )
        )
    }

    private fun externalRecipe(title: String, ingredient: String = "рис"): RecipeDiscoveryResult =
        RecipeDiscoveryResult(
            recipe = Recipe(
                title = title,
                ingredients = listOf(RecipeIngredient(ingredient, "1 стакан")),
                steps = listOf("Приготовить"),
                time = "20 минут",
                calories = 200
            ),
            source = if (title.startsWith("ИИ")) RecipeSource.AI_ASSISTED else RecipeSource.EXTERNAL_API,
            aiAssisted = title.startsWith("ИИ")
        )

    private fun aiAssistedRecipe(title: String, ingredient: String = "рис"): RecipeDiscoveryResult =
        externalRecipe(title, ingredient).copy(
            source = RecipeSource.AI_ASSISTED,
            aiAssisted = true
        )

    private class FakeProductRepository(private val products: List<Product>) : IProductRepository {
        override fun findById(id: UUID): Product? = products.firstOrNull { it.id == id }
        override fun findByBarcodeAndHouseholdId(barcode: String, householdId: UUID): Product? =
            products.firstOrNull { it.barcode == barcode && it.householdId == householdId }

        override fun findByHouseholdId(householdId: UUID): List<Product> =
            products.filter { it.householdId == householdId }

        override fun findByHouseholdIdAndCategoryId(householdId: UUID, categoryId: UUID): List<Product> =
            products.filter { it.householdId == householdId && it.categoryId == categoryId }

        override fun findExpiringBefore(householdId: UUID, date: LocalDate): List<Product> =
            products.filter { it.householdId == householdId && it.expirationDate?.date?.isBefore(date) == true }

        override fun findExpiringBetween(startInclusive: LocalDate, endExclusive: LocalDate): List<Product> =
            products.filter { product ->
                product.expirationDate?.date?.let { !it.isBefore(startInclusive) && it.isBefore(endExclusive) } == true
            }

        override fun findExpiringBetweenByHouseholdId(
            householdId: UUID,
            startInclusive: LocalDate,
            endExclusive: LocalDate
        ): List<Product> =
            findExpiringBetween(startInclusive, endExclusive).filter { it.householdId == householdId }

        override fun findLowStock(): List<Product> =
            products.filter { product ->
                product.lowStockThreshold?.let { product.remainingAmount <= it } == true
            }

        override fun findLowStockByHouseholdId(householdId: UUID): List<Product> =
            findLowStock().filter { it.householdId == householdId }

        override fun save(product: Product): Product = product
        override fun deleteById(id: UUID) = Unit
        override fun existsById(id: UUID): Boolean = products.any { it.id == id }
    }

    private class FakeMembershipRepository(private val memberships: List<Membership>) : IMembershipRepository {
        override fun findByUserId(userId: UUID): List<Membership> =
            memberships.filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            memberships.filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership = membership
        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) = Unit
    }
}
