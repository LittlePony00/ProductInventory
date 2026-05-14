package com.android.rut.miit.productinventory.core.di

import com.android.rut.miit.productinventory.core.storage.NSUserDefaultsTokenStorage
import com.android.rut.miit.productinventory.core.storage.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module = module {
    single<TokenStorage> { NSUserDefaultsTokenStorage() }
}
