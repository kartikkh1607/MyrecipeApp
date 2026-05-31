package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.local.AiRecipeDao
import com.kartik.mealtime.data.local.AiRecipeEntity
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.repository.AiRecipeRepository
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit test for [AiCreationsViewModel] — uses a real [AiRecipeRepository] over a
 * controllable fake DAO so the VM's Flow → StateFlow plumbing is exercised end
 * to end.
 *
 * The VM exposes its state via `stateIn(..., WhileSubscribed(5_000), ...)`, which
 * stays parked at the initial value unless a collector keeps the upstream alive.
 * Each test launches a no-op collector in [runTest]'s `backgroundScope` so the
 * sharing strategy is satisfied and `.value` reflects the real upstream emission.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiCreationsViewModelTest {

    /** Hand-rolled fake — simpler than mocking suspend funs and a StateFlow source. */
    private class FakeAiRecipeDao(initial: List<AiRecipeEntity> = emptyList()) : AiRecipeDao {
        private val state = MutableStateFlow(initial)
        val deletedIds = mutableListOf<String>()

        override fun getAllFlow(): Flow<List<AiRecipeEntity>> = state
        override suspend fun getById(id: String) = state.value.firstOrNull { it.id == id }
        override suspend fun upsert(entity: AiRecipeEntity) {
            state.value = (state.value.filter { it.id != entity.id } + entity)
        }
        override suspend fun delete(id: String) {
            deletedIds += id
            state.value = state.value.filter { it.id != id }
        }
        override suspend fun deleteAll() {
            state.value = emptyList()
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun entity(id: String, name: String = "Recipe $id", createdAt: Long = id.hashCode().toLong()) =
        AiRecipeEntity(
            id = id,
            recipe = Recipe(id, name, "", "", ""),
            source = AiRecipeSource.GENERATED.name,
            createdAt = createdAt,
        )

    @Test
    fun creationsExposesRecipesFromTheRepository() = runTest(UnconfinedTestDispatcher()) {
        val dao = FakeAiRecipeDao(listOf(entity("a"), entity("b")))
        val vm = AiCreationsViewModel(AiRecipeRepository(dao))
        backgroundScope.launch { vm.creations.collect {} }

        assertEquals(setOf("a", "b"), vm.creations.value.map { it.id }.toSet())
    }

    @Test
    fun deleteForwardsToTheRepository() = runTest(UnconfinedTestDispatcher()) {
        val dao = FakeAiRecipeDao(listOf(entity("a"), entity("b")))
        val vm = AiCreationsViewModel(AiRecipeRepository(dao))
        backgroundScope.launch { vm.creations.collect {} }

        vm.delete("a")

        assertEquals(listOf("a"), dao.deletedIds)
        assertEquals(listOf("b"), vm.creations.value.map { it.id })
    }
}
