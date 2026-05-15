package com.android.rut.miit.productinventory.application.service

import com.android.rut.miit.productinventory.domain.model.Household
import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import com.android.rut.miit.productinventory.domain.model.InviteCode
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.model.MembershipRole
import com.android.rut.miit.productinventory.domain.model.Notification
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IInviteCodeRepository
import com.android.rut.miit.productinventory.domain.port.outbound.IMembershipRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationRepository
import com.android.rut.miit.productinventory.domain.port.outbound.INotificationSender
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class HouseholdServiceImplTest {

    @Test
    fun `join by invite code publishes member joined event for new member`() {
        val ownerId = UUID.randomUUID()
        val newMemberId = UUID.randomUUID()
        val household = Household(name = "Home")
        val inviteCode = InviteCode(
            code = "ABC12345",
            householdId = household.id,
            createdByUserId = ownerId,
            expiresAt = Instant.now().plusSeconds(60)
        )
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = HouseholdServiceImpl(
            householdRepository = InMemoryHouseholdRepository(listOf(household)),
            membershipRepository = InMemoryMembershipRepository(
                listOf(Membership(userId = ownerId, householdId = household.id, role = MembershipRole.OWNER))
            ),
            inviteCodeRepository = InMemoryInviteCodeRepository(listOf(inviteCode)),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        val joinedHousehold = service.joinByInviteCode(newMemberId, inviteCode.code)

        assertEquals(household, joinedHousehold)
        val event = eventPublisher.events.single()
        assertEquals(HouseholdEventType.MEMBER_JOINED, event.type)
        assertEquals(household.id, event.householdId)
        assertEquals(newMemberId, event.actorUserId)
        assertEquals(newMemberId, event.memberUserId)
    }

    @Test
    fun `join by invite code does not publish event when user is already member`() {
        val userId = UUID.randomUUID()
        val household = Household(name = "Home")
        val inviteCode = InviteCode(
            code = "ABC12345",
            householdId = household.id,
            createdByUserId = userId,
            expiresAt = Instant.now().plusSeconds(60)
        )
        val eventPublisher = RecordingHouseholdEventPublisher()
        val service = HouseholdServiceImpl(
            householdRepository = InMemoryHouseholdRepository(listOf(household)),
            membershipRepository = InMemoryMembershipRepository(
                listOf(Membership(userId = userId, householdId = household.id, role = MembershipRole.OWNER))
            ),
            inviteCodeRepository = InMemoryInviteCodeRepository(listOf(inviteCode)),
            notificationRepository = RecordingNotificationRepository(),
            notificationSender = RecordingNotificationSender(),
            householdEventPublisher = eventPublisher
        )

        service.joinByInviteCode(userId, inviteCode.code)

        assertEquals(emptyList(), eventPublisher.events)
    }

    private class InMemoryHouseholdRepository(initialHouseholds: List<Household>) : IHouseholdRepository {
        private val households = initialHouseholds.associateBy { it.id }.toMutableMap()

        override fun findById(id: UUID): Household? = households[id]

        override fun save(household: Household): Household {
            households[household.id] = household
            return household
        }

        override fun deleteById(id: UUID) {
            households.remove(id)
        }
    }

    private class InMemoryMembershipRepository(initialMemberships: List<Membership>) : IMembershipRepository {
        private val memberships = initialMemberships.toMutableList()

        override fun findByUserId(userId: UUID): List<Membership> =
            memberships.filter { it.userId == userId }

        override fun findByHouseholdId(householdId: UUID): List<Membership> =
            memberships.filter { it.householdId == householdId }

        override fun findByUserIdAndHouseholdId(userId: UUID, householdId: UUID): Membership? =
            memberships.firstOrNull { it.userId == userId && it.householdId == householdId }

        override fun save(membership: Membership): Membership {
            memberships += membership
            return membership
        }

        override fun deleteByUserIdAndHouseholdId(userId: UUID, householdId: UUID) {
            memberships.removeIf { it.userId == userId && it.householdId == householdId }
        }
    }

    private class InMemoryInviteCodeRepository(initialInviteCodes: List<InviteCode>) : IInviteCodeRepository {
        private val inviteCodes = initialInviteCodes.associateBy { it.code }.toMutableMap()

        override fun findByCode(code: String): InviteCode? = inviteCodes[code]

        override fun findActiveByHouseholdId(householdId: UUID): List<InviteCode> =
            inviteCodes.values.filter { it.householdId == householdId && !it.used }

        override fun save(inviteCode: InviteCode): InviteCode {
            inviteCodes[inviteCode.code] = inviteCode
            return inviteCode
        }
    }

    private class RecordingNotificationRepository : INotificationRepository {
        override fun findByUserId(userId: UUID): List<Notification> = emptyList()

        override fun findUnreadByUserId(userId: UUID): List<Notification> = emptyList()

        override fun existsByUserIdAndDedupeKey(userId: UUID, dedupeKey: String): Boolean = false

        override fun save(notification: Notification): Notification = notification

        override fun markAsRead(id: UUID, userId: UUID) = Unit

        override fun markAllAsRead(userId: UUID) = Unit
    }

    private class RecordingNotificationSender : INotificationSender {
        override fun sendPush(userId: UUID, title: String, message: String) = Unit
    }

    private class RecordingHouseholdEventPublisher : IHouseholdEventPublisher {
        val events = mutableListOf<HouseholdEvent>()

        override fun publish(event: HouseholdEvent) {
            events += event
        }
    }
}
