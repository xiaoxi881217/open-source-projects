package com.kingzcheung.xime.clipboard

import org.junit.Assert.*
import org.junit.Test

class ClipboardCapturePolicyTest {
    @Test fun ownBindingCodesAndDeviceTokensNeverEnterUpload() {
        listOf("LHK-test_fixture_binding_code", "lhk_device_test_fixture_device_secret", "ＬＨＫ-test_fixture_binding_code").forEach {
            assertNotNull(ClipboardCapturePolicy.reject(it))
        }
    }

    @Test fun localHistoryKeepsExplicitlyCopiedSecretsButRemotePolicyStillRejectsThem() {
        listOf("验证码：839201", "123456", "password: sample", "LHK-test_fixture_binding_code").forEach {
            assertNull(ClipboardCapturePolicy.rejectLocal(it))
            assertNotNull(ClipboardCapturePolicy.reject(it))
        }
        assertEquals(ClipboardCapturePolicy.Rejection.EMPTY, ClipboardCapturePolicy.rejectLocal("  "))
        assertEquals(ClipboardCapturePolicy.Rejection.TOO_LONG, ClipboardCapturePolicy.rejectLocal("字".repeat(20_001)))
    }

    @Test fun visibleRefreshRecognizesSameObservationButNotAnotherCopyOrContent() {
        assertEquals(ClipboardObservation.stamp(1, "片段"), ClipboardObservation.stamp(1, "片段"))
        assertNotEquals(ClipboardObservation.stamp(1, "片段"), ClipboardObservation.stamp(2, "片段"))
        assertNotEquals(ClipboardObservation.stamp(1, "片段"), ClipboardObservation.stamp(1, "另一段"))
        assertFalse(ClipboardObservation.stamp(1, "私人文字").contains("私人文字"))
    }

    @Test fun ordinaryLifeAndPublicLinksRemainLocalCandidates() {
        assertNull(ClipboardCapturePolicy.reject("下次一起试试这家面包店，好像有桂花味的。"))
        assertNull(ClipboardCapturePolicy.reject("https://example.org/notes/spring"))
    }

    @Test fun systemMarkAndPrivateEditorAlwaysWin() {
        assertEquals(ClipboardCapturePolicy.Rejection.SENSITIVE,
            ClipboardCapturePolicy.reject("ordinary words", sensitive = true))
        assertEquals(ClipboardCapturePolicy.Rejection.PRIVATE_EDITOR,
            ClipboardCapturePolicy.reject("ordinary words", privateEditor = true))
    }

    @Test fun otpPaymentKeysAndTokenLinksAreRejected() {
        listOf("123456", "１２３４５６", "验证码：839201", "银行卡 6222 0200 1234 5678",
            "支付链接 https://example.org/pay", "sk-proj-abcdefghijklmnopqrstuvw",
            "github_pat_abcdefghijklmnopqrstuv", "Authorization: Bearer abcdef",
            "https://example.org/callback?code=abc", "-----BEGIN PRIVATE KEY-----",
            "a\u0000b").forEach { assertNotNull(it, ClipboardCapturePolicy.reject(it)) }
    }

    @Test fun oversizedDataIsRejectedWithoutTruncatingItIntoSomethingElse() {
        assertEquals(ClipboardCapturePolicy.Rejection.TOO_LONG,
            ClipboardCapturePolicy.reject("字".repeat(ClipboardCapturePolicy.MAX_TEXT_LENGTH + 1)))
        assertNull(ClipboardCapturePolicy.reject("字".repeat(ClipboardCapturePolicy.MAX_TEXT_LENGTH)))
    }
}
