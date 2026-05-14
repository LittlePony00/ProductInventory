package com.android.rut.miit.productinventory.core.di

import org.koin.core.module.Module
import org.koin.dsl.module

actual fun localDataSourceModule(): Module = module {
    // Room-based implementations are registered from the composeApp module
}
