package com.android.rut.miit.productinventory.feature.auth.di

import com.android.rut.miit.productinventory.feature.auth.api.AuthRepository
import com.android.rut.miit.productinventory.feature.auth.api.LoginUseCase
import com.android.rut.miit.productinventory.feature.auth.api.RegisterUseCase
import com.android.rut.miit.productinventory.feature.auth.data.AuthRemoteDataSource
import com.android.rut.miit.productinventory.feature.auth.data.AuthRepositoryImpl
import com.android.rut.miit.productinventory.feature.auth.presentation.login.LoginViewModel
import com.android.rut.miit.productinventory.feature.auth.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.core.module.dsl.bind

val authModule = module {
    factory { AuthRemoteDataSource(get()) }
    factory<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factoryOf(::LoginUseCase)
    factoryOf(::RegisterUseCase)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
