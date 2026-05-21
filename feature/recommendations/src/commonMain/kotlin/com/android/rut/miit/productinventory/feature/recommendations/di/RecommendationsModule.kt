package com.android.rut.miit.productinventory.feature.recommendations.di

import com.android.rut.miit.productinventory.feature.recommendations.api.FindRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetLikedRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeIngredientOptionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.api.SetRecipeLikedUseCase
import com.android.rut.miit.productinventory.feature.recommendations.data.LikedRecipeLocalDataSource
import com.android.rut.miit.productinventory.feature.recommendations.data.RecipeRemoteDataSource
import com.android.rut.miit.productinventory.feature.recommendations.data.RecipeRepositoryImpl
import com.android.rut.miit.productinventory.feature.recommendations.presentation.RecipeListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recommendationsModule = module {
    factory { RecipeRemoteDataSource(get()) }
    single { LikedRecipeLocalDataSource(get()) }
    factory<RecipeRepository> { RecipeRepositoryImpl(get(), get()) }
    factoryOf(::GetRecipesUseCase)
    factoryOf(::GetRecipeSuggestionsUseCase)
    factoryOf(::GetRecipeIngredientOptionsUseCase)
    factoryOf(::FindRecipesUseCase)
    factoryOf(::GetLikedRecipesUseCase)
    factoryOf(::SetRecipeLikedUseCase)
    viewModelOf(::RecipeListViewModel)
}
