package com.android.rut.miit.productinventory.feature.household.di

import com.android.rut.miit.productinventory.feature.household.api.*
import com.android.rut.miit.productinventory.feature.household.data.HouseholdRemoteDataSource
import com.android.rut.miit.productinventory.feature.household.data.HouseholdRepositoryImpl
import com.android.rut.miit.productinventory.feature.household.presentation.list.HouseholdListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val householdModule = module {
    factory { HouseholdRemoteDataSource(get()) }
    factory<HouseholdRepository> { HouseholdRepositoryImpl(get()) }
    factoryOf(::GetHouseholdsUseCase)
    factoryOf(::CreateHouseholdUseCase)
    factoryOf(::JoinHouseholdUseCase)
    factoryOf(::GetMembersUseCase)
    factoryOf(::GenerateInviteCodeUseCase)
    viewModelOf(::HouseholdListViewModel)
}
