package com.android.rut.miit.productinventory.feature.recommendations.di

import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipesUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.GetRecipeSuggestionsUseCase
import com.android.rut.miit.productinventory.feature.recommendations.api.RecipeRepository
import com.android.rut.miit.productinventory.feature.recommendations.data.RecipeRemoteDataSource
import com.android.rut.miit.productinventory.feature.recommendations.data.RecipeRepositoryImpl
import com.android.rut.miit.productinventory.feature.recommendations.presentation.RecipeListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val recommendationsModule = module {
    factory { RecipeRemoteDataSource(get()) }
    factory<RecipeRepository> { RecipeRepositoryImpl(get()) }
    factoryOf(::GetRecipesUseCase)
    factoryOf(::GetRecipeSuggestionsUseCase)
    viewModelOf(::RecipeListViewModel)
}
