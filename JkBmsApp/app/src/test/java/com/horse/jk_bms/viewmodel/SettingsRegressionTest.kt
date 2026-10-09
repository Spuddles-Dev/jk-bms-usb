package com.horse.jk_bms.viewmodel

import com.horse.jk_bms.data.backup.ConfigBackupStore
import com.horse.jk_bms.model.BmsConfig
import com.horse.jk_bms.protocol.validConfig
import com.horse.jk_bms.repository.BmsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRegressionTest {
    private val remote = MutableStateFlow<BmsConfig?>(validConfig())
    private lateinit var viewModel: SettingsViewModel

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val repository = mockk<BmsRepository>()
        every { repository.config } returns remote
        every { repository.deviceInfo } returns MutableStateFlow(null)
        viewModel = SettingsViewModel(repository, mockk<ConfigBackupStore>())
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun testInvalidVisibleInputCannotSubmitOldParsedValue() {
        viewModel.updateInput("volCellOV", "-")
        assertEquals("-", viewModel.state.value.inputs["volCellOV"])
        assertFalse(viewModel.state.value.isValid)
        assertTrue("volCellOV" in viewModel.state.value.inputErrors)
        viewModel.resetEdits()
        assertTrue(viewModel.state.value.isValid)
        assertFalse(viewModel.state.value.hasUnsavedChanges)
    }

    @Test fun testRemoteCleanFieldsMergeWhileDirtyConflictBlocksWrite() {
        viewModel.updateInput("volCellOV", "4.1")
        remote.value = remote.value!!.copy(volCellOV = 4.2f, volBalanTrig = 0.01f)
        val state = viewModel.state.value
        assertEquals("4.1", state.inputs["volCellOV"])
        assertEquals(0.01f, state.editConfig!!.volBalanTrig)
        assertTrue("volCellOV" in state.conflicts)
        assertFalse(state.isValid)
        viewModel.resetEdits()
        assertEquals(4.2f, viewModel.state.value.editConfig!!.volCellOV)
        assertTrue(viewModel.state.value.conflicts.isEmpty())
    }

    @Test fun testDisconnectClearsAllDraftState() {
        viewModel.updateInput("volCellOV", "4.1")
        remote.value = null
        assertEquals(SettingsState(), viewModel.state.value)
    }
}
