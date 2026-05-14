package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.exception.*
import com.android.rut.miit.productinventory.domain.model.*
import com.android.rut.miit.productinventory.domain.port.inbound.IHouseholdService
import com.android.rut.miit.productinventory.domain.port.outbound.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class HouseholdServiceImpl(
    private val householdRepository: IHouseholdRepository,
    private val membershipRepository: IMembershipRepository,
    private val inviteCodeRepository: IInviteCodeRepository,
    private val notificationRepository: INotificationRepository,
    private val notificationSender: INotificationSender,
    private val householdEventPublisher: IHouseholdEventPublisher
) : IHouseholdService {

    @Transactional
    override fun createHousehold(userId: UUID, name: String): Household {
        val household = householdRepository.save(Household(name = name.trim()))

        membershipRepository.save(
            Membership(
                userId = userId,
                householdId = household.id,
                role = MembershipRole.OWNER
            )
        )

        return household
    }

    @Transactional(readOnly = true)
    override fun getHousehold(userId: UUID, householdId: UUID): Household {
        requireMembership(userId, householdId)
        return householdRepository.findById(householdId)
            ?: throw EntityNotFoundException("Household", householdId)
    }

    @Transactional(readOnly = true)
    override fun getUserHouseholds(userId: UUID): List<Household> {
        val memberships = membershipRepository.findByUserId(userId)
        return memberships.mapNotNull { householdRepository.findById(it.householdId) }
    }

    @Transactional(readOnly = true)
    override fun getMembers(userId: UUID, householdId: UUID): List<Membership> {
        requireMembership(userId, householdId)
        return membershipRepository.findByHouseholdId(householdId)
    }

    @Transactional
    override fun generateInviteCode(userId: UUID, householdId: UUID): String {
        requireOwnership(userId, householdId)

        val code = UUID.randomUUID().toString().take(8).uppercase()
        inviteCodeRepository.save(
            InviteCode(
                code = code,
                householdId = householdId,
                createdByUserId = userId,
                expiresAt = Instant.now().plus(7, ChronoUnit.DAYS)
            )
        )

        return code
    }

    @Transactional
    override fun joinByInviteCode(userId: UUID, code: String): Household {
        val inviteCode = inviteCodeRepository.findByCode(code)
            ?: throw EntityNotFoundException("InviteCode", code)

        if (inviteCode.used) throw InviteCodeAlreadyUsedException()
        if (inviteCode.expiresAt.isBefore(Instant.now())) throw InviteCodeExpiredException()

        val existing = membershipRepository.findByUserIdAndHouseholdId(userId, inviteCode.householdId)
        if (existing != null) {
            return householdRepository.findById(inviteCode.householdId)
                ?: throw EntityNotFoundException("Household", inviteCode.householdId)
        }

        membershipRepository.save(
            Membership(
                userId = userId,
                householdId = inviteCode.householdId,
                role = MembershipRole.MEMBER
            )
        )

        inviteCodeRepository.save(inviteCode.copy(used = true))

        val household = householdRepository.findById(inviteCode.householdId)
            ?: throw EntityNotFoundException("Household", inviteCode.householdId)

        notifyOtherMembers(userId, inviteCode.householdId, "New member", "A new member joined ${household.name}")
        householdEventPublisher.publish(
            HouseholdEvent(
                type = HouseholdEventType.MEMBER_JOINED,
                householdId = inviteCode.householdId,
                actorUserId = userId,
                memberUserId = userId
            )
        )

        return household
    }

    @Transactional
    override fun removeMember(ownerId: UUID, householdId: UUID, memberId: UUID) {
        requireOwnership(ownerId, householdId)

        if (ownerId == memberId) {
            throw AccessDeniedException("Owner cannot remove themselves. Use leaveHousehold instead.")
        }

        membershipRepository.deleteByUserIdAndHouseholdId(memberId, householdId)
    }

    @Transactional
    override fun leaveHousehold(userId: UUID, householdId: UUID) {
        val membership = requireMembership(userId, householdId)

        if (membership.role == MembershipRole.OWNER) {
            val otherMembers = membershipRepository.findByHouseholdId(householdId)
                .filter { it.userId != userId }
            if (otherMembers.isNotEmpty()) {
                throw AccessDeniedException("Owner must transfer ownership or remove all members before leaving")
            }
            householdRepository.deleteById(householdId)
        }

        membershipRepository.deleteByUserIdAndHouseholdId(userId, householdId)
    }

    private fun requireMembership(userId: UUID, householdId: UUID): Membership {
        return membershipRepository.findByUserIdAndHouseholdId(userId, householdId)
            ?: throw AccessDeniedException("User is not a member of this household")
    }

    private fun requireOwnership(userId: UUID, householdId: UUID) {
        val membership = requireMembership(userId, householdId)
        if (membership.role != MembershipRole.OWNER) {
            throw AccessDeniedException("Only household owner can perform this action")
        }
    }

    private fun notifyOtherMembers(actorId: UUID, householdId: UUID, title: String, message: String) {
        val members = membershipRepository.findByHouseholdId(householdId)
        members.filter { it.userId != actorId }.forEach { m ->
            notificationRepository.save(Notification(userId = m.userId, title = title, message = message))
            notificationSender.sendPush(m.userId, title, message)
        }
    }
}
