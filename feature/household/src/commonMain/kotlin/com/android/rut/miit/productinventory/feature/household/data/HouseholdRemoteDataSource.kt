package com.android.rut.miit.productinventory.feature.household.data

import com.android.rut.miit.productinventory.core.network.ApiConstants
import com.android.rut.miit.productinventory.feature.household.data.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class HouseholdRemoteDataSource(private val httpClient: HttpClient) {

    suspend fun getMyHouseholds(): List<HouseholdResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households").body()
    }

    suspend fun getHousehold(householdId: String): HouseholdResponseDto {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId").body()
    }

    suspend fun createHousehold(request: CreateHouseholdRequestDto): HouseholdResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households") {
            setBody(request)
        }.body()
    }

    suspend fun getMembers(householdId: String): List<MemberResponseDto> {
        return httpClient.get("${ApiConstants.API_V1}/households/$householdId/members").body()
    }

    suspend fun generateInviteCode(householdId: String): InviteCodeResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/$householdId/invite").body()
    }

    suspend fun joinByInviteCode(request: JoinHouseholdRequestDto): HouseholdResponseDto {
        return httpClient.post("${ApiConstants.API_V1}/households/join") {
            setBody(request)
        }.body()
    }

    suspend fun removeMember(householdId: String, memberId: String) {
        httpClient.delete("${ApiConstants.API_V1}/households/$householdId/members/$memberId")
    }

    suspend fun leaveHousehold(householdId: String) {
        httpClient.post("${ApiConstants.API_V1}/households/$householdId/leave")
    }
}
