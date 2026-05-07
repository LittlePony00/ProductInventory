package com.android.rut.miit.productinventory.feature.profile.di

import com.android.rut.miit.productinventory.feature.profile.api.GetProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.api.ProfileRepository
import com.android.rut.miit.productinventory.feature.profile.api.UpdateProfileUseCase
import com.android.rut.miit.productinventory.feature.profile.data.ProfileRemoteDataSource
import com.android.rut.miit.productinventory.feature.profile.data.ProfileRepositoryImpl
import com.android.rut.miit.productinventory.feature.profile.presentation.ProfileViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val profileModule = module {
    factory { ProfileRemoteDataSource(get()) }
    factory<ProfileRepository> { ProfileRepositoryImpl(get()) }
    factoryOf(::GetProfileUseCase)
    factoryOf(::UpdateProfileUseCase)
    viewModelOf(::ProfileViewModel)
}
