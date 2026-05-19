package com.android.rut.miit.productinventory.feature.household.presentation.list

import com.android.rut.miit.productinventory.feature.household.api.CreateHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.GenerateInviteCodeUseCase
import com.android.rut.miit.productinventory.feature.household.api.GetHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.HouseholdRepository
import com.android.rut.miit.productinventory.feature.household.api.JoinHouseholdUseCase
import com.android.rut.miit.productinventory.feature.household.api.RefreshHouseholdsUseCase
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import com.android.rut.miit.productinventory.feature.household.api.models.InviteCode
import com.android.rut.miit.productinventory.feature.household.api.models.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class HouseholdListViewModelTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `generates invite code action for selected household`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = viewModel(FakeHouseholdRepository())
            val action = async { viewModel.viewAction.first { it is HouseholdListAction.ShowInviteCode } }

            viewModel.onEvent(HouseholdListEvent.OnGenerateInviteCodeClick("household-id"))
            advanceUntilIdle()

            val inviteAction = assertIs<HouseholdListAction.ShowInviteCode>(action.await())
            assertEquals("INVITE42", inviteAction.code)
            assertEquals("2026-05-22T00:00:00Z", inviteAction.expiresAt)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `loads households after create and join`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeHouseholdRepository()
            val viewModel = viewModel(repository)

            viewModel.onEvent(HouseholdListEvent.OnCreate)
            advanceUntilIdle()
            viewModel.onEvent(HouseholdListEvent.OnCreateHouseholdConfirm("Home"))
            advanceUntilIdle()
            viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdConfirm("INVITE42"))
            advanceUntilIdle()

            val state = assertIs<HouseholdListState.Content>(viewModel.viewState.value)
            assertEquals(listOf("Home", "Joined"), state.households.map { it.name })
            assertEquals(listOf("Home"), repository.createdNames)
            assertEquals(listOf("INVITE42"), repository.joinedCodes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `successful join emits close dialog action`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = viewModel(FakeHouseholdRepository())
            val action = async { viewModel.viewAction.first { it is HouseholdListAction.CloseJoinDialog } }

            viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdConfirm("INVITE42"))
            advanceUntilIdle()

            assertIs<HouseholdListAction.CloseJoinDialog>(action.await())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `failed join emits visible error message`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = viewModel(
                FakeHouseholdRepository(joinError = IllegalStateException("Invite code is invalid"))
            )
            val action = async { viewModel.viewAction.first { it is HouseholdListAction.ShowMessage } }

            viewModel.onEvent(HouseholdListEvent.OnJoinHouseholdConfirm("BADCODE"))
            advanceUntilIdle()

            val message = assertIs<HouseholdListAction.ShowMessage>(action.await())
            assertEquals("Invite code is invalid", message.message)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun viewModel(repository: HouseholdRepository): HouseholdListViewModel =
        HouseholdListViewModel(
            getHouseholdsUseCase = GetHouseholdsUseCase(repository),
            refreshHouseholdsUseCase = RefreshHouseholdsUseCase(repository),
            createHouseholdUseCase = CreateHouseholdUseCase(repository),
            generateInviteCodeUseCase = GenerateInviteCodeUseCase(repository),
            joinHouseholdUseCase = JoinHouseholdUseCase(repository)
        )

    private class FakeHouseholdRepository(
        private val joinError: Throwable? = null
    ) : HouseholdRepository {
        private val households = mutableListOf<Household>()
        val createdNames = mutableListOf<String>()
        val joinedCodes = mutableListOf<String>()

        override suspend fun getMyHouseholds(): List<Household> = households.toList()

        override suspend fun getHousehold(householdId: String): Household =
            households.first { it.id == householdId }

        override suspend fun createHousehold(name: String): Household {
            createdNames += name
            val household = Household(id = "created-${createdNames.size}", name = name, createdAt = "2026-05-15T00:00:00Z")
            households += household
            return household
        }

        override suspend fun getMembers(householdId: String): List<Member> = emptyList()

        override suspend fun generateInviteCode(householdId: String): InviteCode =
            InviteCode(code = "INVITE42", expiresAt = "2026-05-22T00:00:00Z")

        override suspend fun joinByInviteCode(inviteCode: String): Household {
            joinError?.let { throw it }
            joinedCodes += inviteCode
            val household = Household(id = "joined-${joinedCodes.size}", name = "Joined", createdAt = "2026-05-15T00:00:00Z")
            households += household
            return household
        }

        override suspend fun removeMember(householdId: String, memberId: String) = Unit
        override suspend fun leaveHousehold(householdId: String) = Unit
    }
}
