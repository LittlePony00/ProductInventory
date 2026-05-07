package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.network.HttpClientFactory
import com.android.rut.miit.productinventory.core.storage.InMemoryTokenStorage
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val coreModule = module {
    single<TokenStorage> { InMemoryTokenStorage() }
    singleOf(::HttpClientFactory)
    single { get<HttpClientFactory>().create() }
}
