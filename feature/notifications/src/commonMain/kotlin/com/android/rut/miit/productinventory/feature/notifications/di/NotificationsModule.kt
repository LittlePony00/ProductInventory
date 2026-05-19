package com.android.rut.miit.productinventory.feature.notifications.di

import com.android.rut.miit.productinventory.feature.notifications.api.*
import com.android.rut.miit.productinventory.feature.notifications.data.NotificationRemoteDataSource
import com.android.rut.miit.productinventory.feature.notifications.data.NotificationRepositoryImpl
import com.android.rut.miit.productinventory.feature.notifications.presentation.NotificationListViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val notificationsModule = module {
    factory { NotificationRemoteDataSource(get()) }
    factory<NotificationRepository> { NotificationRepositoryImpl(get()) }
    factoryOf(::GetNotificationsUseCase)
    factoryOf(::GetNotificationSettingsUseCase)
    factoryOf(::MarkNotificationReadUseCase)
    factoryOf(::MarkAllReadUseCase)
    factoryOf(::UpdateNotificationSettingsUseCase)
    viewModelOf(::NotificationListViewModel)
}
