package com.kartik.mealtime.ui.viewmodel

import com.google.firebase.auth.FirebaseUser
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.ShoppingDao
import com.kartik.mealtime.data.local.ShoppingItemEntity
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import com.kartik.mealtime.domain.model.Ingredient
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.ShoppingListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

/**
 * Unit tests for [ShoppingListViewModel].
 *
 * [FakeShoppingDao] is backed by a [MutableStateFlow] so the VM's `init`-block
 * collector mirrors every DAO mutation into `shoppingList` synchronously (the
 * `UnconfinedTestDispatcher` runs the launched collect eagerly). Local list
 * behavior is asserted off that state; the Firestore sync side-effects are
 * Mockito-verified on a mocked [SyncRepository] (its suspend funcs are verified
 * inside `runTest`). Signed-in vs. signed-out is toggled via [UserRepository].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private class FakeShoppingDao : ShoppingDao {
        val items = MutableStateFlow<List<ShoppingItemEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<ShoppingItemEntity>> = items
        override suspend fun insert(entity: ShoppingItemEntity) {
            // Mirrors Room's INSERT OR IGNORE: a duplicate primary key is a no-op.
            if (items.value.none { it.key == entity.key }) items.value = items.value + entity
        }
        override suspend fun setChecked(key: String, checked: Boolean) {
            items.value = items.value.map { if (it.key == key) it.copy(checked = checked) else it }
        }
        override suspend fun deleteByKey(key: String) {
            items.value = items.value.filterNot { it.key == key }
        }
        override suspend fun deleteChecked() {
            items.value = items.value.filterNot { it.checked }
        }
        override suspend fun deleteAll() { items.value = emptyList() }
    }

    private lateinit var dao: FakeShoppingDao
    private lateinit var userRepository: UserRepository
    private lateinit var syncRepository: SyncRepository
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        dao = FakeShoppingDao()
        userRepository = mock(UserRepository::class.java)
        syncRepository = mock(SyncRepository::class.java)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun signInAs(uid: String) {
        val user = mock(FirebaseUser::class.java)
        Mockito.`when`(user.uid).thenReturn(uid)
        Mockito.`when`(userRepository.currentUser).thenReturn(user)
    }

    private fun buildVm() = ShoppingListViewModel(
        shoppingDao = dao,
        userRepository = userRepository,
        syncRepository = syncRepository,
        analytics = mock(AnalyticsHelper::class.java)
    )

    private fun recipeWithIngredients() = Recipe(
        id = "r1", name = "Pasta", description = "", imageUrl = "", category = "",
        ingredients = listOf(
            Ingredient(id = "i1", name = "Spaghetti", amount = "200", unit = "g"),
            Ingredient(id = "i2", name = "Tomato", amount = "3", unit = "")
        )
    )

    // ── addToShoppingList ─────────────────────────────────────────────────────────

    @Test
    fun `addToShoppingList adds every ingredient and records the recipe name`() {
        val vm = buildVm()

        vm.addToShoppingList(recipeWithIngredients())

        assertEquals(2, vm.shoppingList.value.size)
        assertEquals(setOf("r1_i1", "r1_i2"), vm.shoppingList.value.map { it.key }.toSet())
        assertEquals("Pasta", vm.lastAddedRecipeName.value)
    }

    @Test
    fun `addToShoppingList de-duplicates ingredients already present`() {
        val vm = buildVm()

        vm.addToShoppingList(recipeWithIngredients())
        vm.addToShoppingList(recipeWithIngredients())

        assertEquals(2, vm.shoppingList.value.size)
    }

    @Test
    fun `addToShoppingList uploads each item to sync when signed in`() = runTest(dispatcher.scheduler) {
        signInAs("uid1")
        val vm = buildVm()

        vm.addToShoppingList(recipeWithIngredients())

        // Concrete args (no matchers): verifying a suspend fun with Mockito matchers
        // collides with the synthetic Continuation parameter, so match by value.
        verify(syncRepository).uploadShoppingItem(
            "uid1",
            ShoppingItemEntity("r1_i1", "Spaghetti", "200", "g", "Pasta")
        )
        verify(syncRepository).uploadShoppingItem(
            "uid1",
            ShoppingItemEntity("r1_i2", "Tomato", "3", "", "Pasta")
        )
    }

    @Test
    fun `addToShoppingList does not touch sync when signed out`() {
        val vm = buildVm()   // userRepository.currentUser defaults to null

        vm.addToShoppingList(recipeWithIngredients())

        verifyNoInteractions(syncRepository)
    }

    // ── toggle / remove ────────────────────────────────────────────────────────────

    @Test
    fun `toggleShoppingItem flips the checked flag`() {
        val vm = buildVm()
        vm.addCustomShoppingItem("Milk")
        val key = vm.shoppingList.value.single().key

        vm.toggleShoppingItem(key)
        assertTrue(vm.shoppingList.value.single().isChecked)

        vm.toggleShoppingItem(key)
        assertFalse(vm.shoppingList.value.single().isChecked)
    }

    @Test
    fun `removeItem removes locally and syncs the deletion when signed in`() = runTest(dispatcher.scheduler) {
        signInAs("uid1")
        val vm = buildVm()
        vm.addCustomShoppingItem("Milk")
        val key = vm.shoppingList.value.single().key

        vm.removeItem(key)

        assertTrue(vm.shoppingList.value.isEmpty())
        verify(syncRepository).deleteShoppingItem("uid1", key)
    }

    @Test
    fun `removeCheckedItems removes only the checked items`() {
        val vm = buildVm()
        vm.addCustomShoppingItem("Milk")
        vm.addCustomShoppingItem("Eggs")
        val milkKey = vm.shoppingList.value.first { it.ingredientName == "Milk" }.key
        vm.toggleShoppingItem(milkKey)

        vm.removeCheckedItems()

        assertEquals(listOf("Eggs"), vm.shoppingList.value.map { it.ingredientName })
    }

    @Test
    fun `restoreShoppingItem re-inserts with its checked state preserved`() {
        val vm = buildVm()
        val item = ShoppingListItem(
            key = "k1", ingredientName = "Butter", amount = "1", unit = "stick",
            recipeName = "Cookies", isChecked = true
        )

        vm.restoreShoppingItem(item)

        val restored = vm.shoppingList.value.single()
        assertEquals("Butter", restored.ingredientName)
        assertTrue(restored.isChecked)
    }

    @Test
    fun `clearShoppingList empties the list and clears remote when signed in`() = runTest(dispatcher.scheduler) {
        signInAs("uid1")
        val vm = buildVm()
        vm.addCustomShoppingItem("Milk")

        vm.clearShoppingList()

        assertTrue(vm.shoppingList.value.isEmpty())
        verify(syncRepository).clearShoppingList("uid1")
    }

    // ── addCustomShoppingItem ─────────────────────────────────────────────────────

    @Test
    fun `addCustomShoppingItem adds the item under the Custom section`() {
        val vm = buildVm()

        vm.addCustomShoppingItem("  Olive Oil  ")

        val item = vm.shoppingList.value.single()
        assertEquals("Olive Oil", item.ingredientName)   // trimmed
        assertEquals("Custom", item.recipeName)
    }

    @Test
    fun `addCustomShoppingItem ignores blank input`() {
        val vm = buildVm()

        vm.addCustomShoppingItem("   ")

        assertTrue(vm.shoppingList.value.isEmpty())
    }
}
