package com.divyang.studymateai

import com.divyang.studymateai.data.model.quizz.QuizHistory
import com.divyang.studymateai.data.viewmodel.HistoryViewModel
import com.divyang.studymateai.fakes.FakeAuthRepository
import com.divyang.studymateai.fakes.FakeQuizHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(userId = "u1")

    private fun sample(id: String) =
        QuizHistory(id = id, chapterId = "c", score = 80, date = Date(), chapterTitle = "T")

    @Test
    fun `loads history on init`() = runTest {
        val repo = FakeQuizHistoryRepository(listOf(sample("h1"), sample("h2")))
        val vm = HistoryViewModel(auth, repo)

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.histories.size)
    }

    @Test
    fun `confirmDelete removes the pending item`() = runTest {
        val repo = FakeQuizHistoryRepository(listOf(sample("h1"), sample("h2")))
        val vm = HistoryViewModel(auth, repo)
        advanceUntilIdle()

        vm.requestDelete(vm.uiState.value.histories.first())
        vm.confirmDelete()
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.histories.size)
    }
}
