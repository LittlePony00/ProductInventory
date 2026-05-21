package com.android.rut.miit.productinventory.domain.port.outbound

import com.android.rut.miit.productinventory.domain.model.UserFoodPreferences
import java.util.UUID

interface IUserFoodPreferencesRepository {
    fun findByUserId(userId: UUID): UserFoodPreferences?
    fun save(preferences: UserFoodPreferences): UserFoodPreferences
}
