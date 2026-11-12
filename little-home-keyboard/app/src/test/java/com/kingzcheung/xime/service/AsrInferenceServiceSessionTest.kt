package com.kingzcheung.xime.service

import android.content.Intent
import com.kingzcheung.xime.speech.LocalAsrSession
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.*

class AsrInferenceServiceSessionTest {
    @Test fun sessionsReuseWeightsAndSwitchWithoutDeletingOriginalModels() {
        val old=mock<LocalAsrSession>();val next=mock<LocalAsrSession>();val receiver=mock<IInferenceAsrCallback>()
        val service=AsrInferenceService();val requested=mutableListOf<String>()
        service.sessionFactory={dir->requested+=dir;if(dir=="/models/old")old else next}
        val api=service.onBind(Intent()) as IInferenceAsrService
        assertTrue(api.startAsr("/models/old",receiver));assertTrue(api.startAsr("/models/old",receiver))
        assertEquals(listOf("/models/old"),requested);verify(old,times(2)).reset()
        assertTrue(api.startAsr("/models/new",receiver));verify(old).close();verify(next).reset()
        service.onDestroy();verify(next).close()
    }
    @Test fun audioPartialFinishAndCancelReachTheSelectedSession() {
        val model=mock<LocalAsrSession>();val receiver=mock<IInferenceAsrCallback>()
        whenever(model.partial()).thenReturn("合成测试");whenever(model.finish()).thenReturn("合成测试完成")
        val service=AsrInferenceService();service.sessionFactory={model}
        val api=service.onBind(Intent()) as IInferenceAsrService
        assertTrue(api.startAsr("/models/local",receiver));val audio=byteArrayOf(0,0)
        api.pushAsrAudio(audio);verify(model).accept(audio);verify(receiver).onPartialResult("合成测试")
        assertEquals("合成测试完成",api.stopAsr());api.cancelAsr();verify(model,times(2)).reset()
        api.releaseAsr();verify(model).close()
    }
    @Test fun loadingOrDecodingFailureIsHonestAndReleasesTheFailedSession() {
        val receiver=mock<IInferenceAsrCallback>();val service=AsrInferenceService()
        service.sessionFactory={throw IllegalStateException("fixture load failure")}
        val api=service.onBind(Intent()) as IInferenceAsrService
        assertFalse(api.startAsr("/models/bad",receiver))
        val bad=mock<LocalAsrSession>();service.sessionFactory={bad};assertTrue(api.startAsr("/models/bad",receiver))
        whenever(bad.accept(any())).thenThrow(IllegalStateException("fixture decode failure"))
        api.pushAsrAudio(byteArrayOf(0,0));verify(bad).close();verify(receiver).onError(any())
        assertEquals("",api.stopAsr())
    }
}
