package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.rest

import com.android.rut.miit.productinventory.application.dto.request.CreateHouseholdRequest
import com.android.rut.miit.productinventory.application.dto.request.JoinHouseholdRequest
import com.android.rut.miit.productinventory.application.dto.response.HouseholdResponse
import com.android.rut.miit.productinventory.application.dto.response.InviteCodeResponse
import com.android.rut.miit.productinventory.application.dto.response.MembershipResponse
import com.android.rut.miit.productinventory.application.mapper.toResponse
import com.android.rut.miit.productinventory.domain.port.inbound.IHouseholdService
import com.android.rut.miit.productinventory.domain.port.outbound.IUserRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/households")
class HouseholdController(
    private val householdService: IHouseholdService,
    private val userRepository: IUserRepository
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createHousehold(@Valid @RequestBody request: CreateHouseholdRequest): HouseholdResponse {
        return householdService.createHousehold(currentUserId(), request.name).toResponse()
    }

    @GetMapping
    fun getMyHouseholds(): List<HouseholdResponse> {
        return householdService.getUserHouseholds(currentUserId()).map { it.toResponse() }
    }

    @GetMapping("/{householdId}")
    fun getHousehold(@PathVariable householdId: UUID): HouseholdResponse {
        return householdService.getHousehold(currentUserId(), householdId).toResponse()
    }

    @GetMapping("/{householdId}/members")
    fun getMembers(@PathVariable householdId: UUID): List<MembershipResponse> {
        val memberships = householdService.getMembers(currentUserId(), householdId)
        return memberships.map { membership ->
            val user = userRepository.findById(membership.userId)
            membership.toResponse(user!!)
        }
    }

    @PostMapping("/{householdId}/invite")
    fun generateInviteCode(@PathVariable householdId: UUID): InviteCodeResponse {
        val code = householdService.generateInviteCode(currentUserId(), householdId)
        return InviteCodeResponse(
            code = code,
            expiresAt = java.time.Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS)
        )
    }

    @PostMapping("/join")
    fun joinByInviteCode(@Valid @RequestBody request: JoinHouseholdRequest): HouseholdResponse {
        return householdService.joinByInviteCode(currentUserId(), request.inviteCode).toResponse()
    }

    @DeleteMapping("/{householdId}/members/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(@PathVariable householdId: UUID, @PathVariable memberId: UUID) {
        householdService.removeMember(currentUserId(), householdId, memberId)
    }

    @PostMapping("/{householdId}/leave")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveHousehold(@PathVariable householdId: UUID) {
        householdService.leaveHousehold(currentUserId(), householdId)
    }
}
