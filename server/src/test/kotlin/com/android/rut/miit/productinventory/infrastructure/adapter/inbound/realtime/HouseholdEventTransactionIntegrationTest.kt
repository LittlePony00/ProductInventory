package com.android.rut.miit.productinventory.infrastructure.adapter.inbound.realtime

import com.android.rut.miit.productinventory.domain.model.HouseholdEvent
import com.android.rut.miit.productinventory.domain.model.HouseholdEventType
import com.android.rut.miit.productinventory.domain.port.outbound.IHouseholdEventPublisher
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@SpringBootTest
@ActiveProfiles("test")
class HouseholdEventTransactionIntegrationTest @Autowired constructor(
    private val householdEventPublisher: IHouseholdEventPublisher,
    private val broadcaster: HouseholdEventSseBroadcaster,
    private val transactionTemplate: TransactionTemplate,
    private val emitterFactory: RecordingSseEmitterFactory
) {

    @AfterEach
    fun tearDown() {
        emitterFactory.reset()
    }

    @Test
    fun `household event is sent after transaction commit`() {
        val emitter = emitterFactory.nextEmitter
        val event = event()
        broadcaster.subscribe(event.householdId)

        transactionTemplate.executeWithoutResult {
            householdEventPublisher.publish(event)
        }

        assertTrue(event in emitter.sentData)
    }

    @Test
    fun `household event is not sent after transaction rollback`() {
        val emitter = emitterFactory.nextEmitter
        val event = event()
        broadcaster.subscribe(event.householdId)

        transactionTemplate.executeWithoutResult { status ->
            householdEventPublisher.publish(event)
            status.setRollbackOnly()
        }

        assertEquals(0, emitter.sentData.size)
    }

    @TestConfiguration
    class Config {
        @Bean
        @Primary
        fun recordingSseEmitterFactory(): RecordingSseEmitterFactory = RecordingSseEmitterFactory()
    }

    class RecordingSseEmitterFactory : SseEmitterFactory {
        var nextEmitter = RecordingSseEmitter()
            private set

        override fun create(): SseEmitter = nextEmitter

        fun reset() {
            nextEmitter = RecordingSseEmitter()
        }
    }

    class RecordingSseEmitter : SseEmitter(0L) {
        val sentData = mutableListOf<Any>()

        override fun send(builder: SseEventBuilder) {
            sentData.addAll(builder.build().mapNotNull { it.data })
        }
    }

    private companion object {
        fun event(): HouseholdEvent =
            HouseholdEvent(
                type = HouseholdEventType.PRODUCT_CREATED,
                householdId = UUID.randomUUID(),
                actorUserId = UUID.randomUUID()
            )
    }
}
