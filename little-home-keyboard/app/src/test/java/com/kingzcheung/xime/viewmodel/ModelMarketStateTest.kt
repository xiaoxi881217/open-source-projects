package com.kingzcheung.xime.viewmodel

import com.kingzcheung.xime.model.*
import com.kingzcheung.xime.speech.models.AsrModelRegistry
import org.junit.Assert.*
import org.junit.Test
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ModelMarketStateTest {
    @Test fun detailDoesNotWaitForRemoteNetwork() {
        kotlinx.coroutines.Dispatchers.setMain(kotlinx.coroutines.test.StandardTestDispatcher())
        val store = androidx.lifecycle.ViewModelStore()
        try {
            val app = org.mockito.kotlin.mock<android.app.Application>()
            org.mockito.kotlin.whenever(app.applicationContext).thenReturn(app)
            val vm = ModelManagementViewModel(app)
            store.put("test", vm)
            assertFalse(vm.uiState.value.isLoading)
            assertEquals(7, vm.uiState.value.models.size)
        } finally { store.clear(); kotlinx.coroutines.Dispatchers.resetMain() }
    }

    @Test fun builtInsAreAvailableBeforeRemoteRefresh() {
        val models = ModelManager.getAllModels()
        assertTrue(models.map { it.id }.containsAll(AsrModelRegistry.profiles.map { it.id }))
        assertTrue(models.all { ModelManager.getModel(it.id) != null })
    }
    @Test fun refreshKeepsProgressAndErrorByIdAfterReordering() {
        val a = ModelManager.getAllModels()[0]; val b = ModelManager.getAllModels()[1]
        val progress = ModelDownloadState.Downloading(.5f, 50, 100)
        val error = ModelDownloadState.Error("连接超时")
        val previous = listOf(ModelItemState(a, downloadState = progress), ModelItemState(b, downloadState = error))
        val merged = mergeModelItems(listOf(ModelItemState(b), ModelItemState(a, isDownloaded = true)), previous)
        assertEquals(error, merged[0].downloadState); assertEquals(progress, merged[1].downloadState)
        assertTrue(merged[1].isDownloaded)
    }
}
