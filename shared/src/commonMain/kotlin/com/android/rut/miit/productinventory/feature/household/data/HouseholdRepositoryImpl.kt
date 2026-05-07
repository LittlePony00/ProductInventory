package com.android.rut.miit.productinventory.feature.household.data

import com.android.rut.miit.productinventory.feature.household.api.HouseholdRepository
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.api.models.InviteCode
import com.android.rut.miit.productinventory.feature.household.api.models.Member
import com.android.rut.miit.productinventory.feature.household.data.mappers.toDomain
import com.android.rut.miit.productinventory.feature.household.data.models.CreateHouseholdRequestDto
import com.android.rut.miit.productinventory.feature.household.data.models.JoinHouseholdRequestDto

class HouseholdRepositoryImpl(
    private val remoteDataSource: HouseholdRemoteDataSource
) : HouseholdRepository {

    override suspend fun getMyHouseholds(): List<Household> {
        return remoteDataSource.getMyHouseholds().map { it.toDomain() }
    }

    override suspend fun getHousehold(householdId: String): Household {
        return remoteDataSource.getHousehold(householdId).toDomain()
    }

    override suspend fun createHousehold(name: String): Household {
        return remoteDataSource.createHousehold(CreateHouseholdRequestDto(name)).toDomain()
    }

    override suspend fun getMembers(householdId: String): List<Member> {
        return remoteDataSource.getMembers(householdId).map { it.toDomain() }
    }

    override suspend fun generateInviteCode(householdId: String): InviteCode {
        return remoteDataSource.generateInviteCode(householdId).toDomain()
    }

    override suspend fun joinByInviteCode(inviteCode: String): Household {
        return remoteDataSource.joinByInviteCode(JoinHouseholdRequestDto(inviteCode)).toDomain()
    }

    override suspend fun removeMember(householdId: String, memberId: String) {
        remoteDataSource.removeMember(householdId, memberId)
    }

    override suspend fun leaveHousehold(householdId: String) {
        remoteDataSource.leaveHousehold(householdId)
    }
}
