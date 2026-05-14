package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.network.HttpClientFactory
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val coreModule = module {
    singleOf(::HttpClientFactory)
    single { get<HttpClientFactory>().create() }
}
