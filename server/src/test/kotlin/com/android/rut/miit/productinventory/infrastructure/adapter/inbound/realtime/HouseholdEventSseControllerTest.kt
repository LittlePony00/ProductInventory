package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.Household
import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import com.android.rut.miit.productinventory.domain.model.Membership
import com.android.rut.miit.productinventory.domain.port.inbound.IHouseholdService
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class HouseholdEventSseControllerTest {

    @AfterTest
    fun tearDown() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `subscribe validates current user household access before opening emitter`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = RecordingHouseholdService(householdId)
        val controller = HouseholdEventSseController(
            service,
            HouseholdEventSseBroadcaster(DefaultSseEmitterFactory())
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())

        controller.subscribe(householdId)

        assertEquals(listOf(userId to householdId), service.getHouseholdCalls)
    }

    @Test
    fun `missed events validates access and returns broadcaster retained events`() {
        val userId = UUID.randomUUID()
        val householdId = UUID.randomUUID()
        val service = RecordingHouseholdService(householdId)
        val broadcaster = HouseholdEventSseBroadcaster(SseEmitterFactory { DefaultSseEmitterFactory().create() })
        val controller = HouseholdEventSseController(service, broadcaster)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(userId, null, emptyList())
        val firstEvent = HouseholdEvent(
            type = HouseholdEventType.PRODUCT_CREATED,
            householdId = householdId,
            actorUserId = userId
        )
        val secondEvent = HouseholdEvent(
            type = HouseholdEventType.PRODUCT_UPDATED,
            householdId = householdId,
            actorUserId = userId
        )
        broadcaster.publish(firstEvent)
        broadcaster.publish(secondEvent)

        val missedEvents = controller.missedEvents(householdId, firstEvent.id)

        assertEquals(listOf(secondEvent), missedEvents)
        assertEquals(listOf(userId to householdId), service.getHouseholdCalls)
    }

    private class RecordingHouseholdService(
        private val householdId: UUID
    ) : IHouseholdService {
        val getHouseholdCalls = mutableListOf<Pair<UUID, UUID>>()

        override fun createHousehold(userId: UUID, name: String): Household =
            Household(id = householdId, name = name)

        override fun getHousehold(userId: UUID, householdId: UUID): Household {
            getHouseholdCalls += userId to householdId
            return Household(id = householdId, name = "Home")
        }

        override fun getUserHouseholds(userId: UUID): List<Household> = emptyList()

        override fun getMembers(userId: UUID, householdId: UUID): List<Membership> = emptyList()

        override fun generateInviteCode(userId: UUID, householdId: UUID): String = "ABC12345"

        override fun joinByInviteCode(userId: UUID, code: String): Household =
            Household(id = householdId, name = "Home")

        override fun removeMember(ownerId: UUID, householdId: UUID, memberId: UUID) = Unit

        override fun leaveHousehold(userId: UUID, householdId: UUID) = Unit
    }
}
