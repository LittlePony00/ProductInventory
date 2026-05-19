package com.android.rut.miit.productinventory.feature.realtime.di

import com.android.rut.miit.productinventory.feature.realtime.api.ObserveHouseholdEventsUseCase
import com.android.rut.miit.productinventory.feature.realtime.api.RealtimeRepository
import com.android.rut.miit.productinventory.feature.realtime.data.RealtimeRepositoryImpl
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val realtimeModule = module {
    factory<RealtimeRepository> { RealtimeRepositoryImpl(get()) }
    factoryOf(::ObserveHouseholdEventsUseCase)
}
