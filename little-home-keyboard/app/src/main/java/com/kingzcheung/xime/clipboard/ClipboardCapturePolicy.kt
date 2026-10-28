package com.kingzcheung.xime.clipboard

/** 保守过滤只能降低风险；未被识别的敏感文字仍可能存在。 */
object ClipboardCapturePolicy {
    const val MAX_TEXT_LENGTH = 20_000
    @Volatile var privateEditor: Boolean = false
    @Volatile var editorPackage: String? = null

    enum class Rejection { EMPTY, TOO_LONG, SENSITIVE, PRIVATE_EDITOR, SECRET_OR_PAYMENT }

    private val privateTerms = Regex(
        "(?i)(密码|口令|验证码|校验码|动态码|支付|付款|收款|银行卡|信用卡|安全码|助记词|私钥|" +
            "password|passwd|passcode|verification\\s*code|one[ -]?time|otp|cvv|cvc|" +
            "api[_ -]?key|access[_ -]?token|refresh[_ -]?token|client[_ -]?secret|" +
            "authorization\\s*[:=]|bearer\\s+|[?&](token|key|secret|code)=)"
    )
    private val secretValue = Regex(
        "(?i)(-----BEGIN [A-Z ]*PRIVATE KEY-----|" +
            "\\b(sk-[a-z0-9_-]{12,}|gh[pousr]_[a-z0-9]{16,}|github_pat_[a-z0-9_]{16,}|AKIA[A-Z0-9]{16})\\b|" +
            "\\beyJ[a-z0-9_-]{8,}\\.[a-z0-9_-]{8,}\\.[a-z0-9_-]{8,}\\b)"
    )
    private val shortCode = Regex("[0-9０-９][0-9０-９ \\-]{2,10}[0-9０-９]")
    private val longNumber = Regex("(?<![0-9])(?:[0-9][ -]?){13,19}(?![0-9])")

    fun rejectLocal(text: String): Rejection? = when {
        text.isBlank() -> Rejection.EMPTY
        text.length > MAX_TEXT_LENGTH -> Rejection.TOO_LONG
        else -> null
    }

    fun reject(text: String, sensitive: Boolean = false, privateEditor: Boolean = false): Rejection? {
        if (sensitive) return Rejection.SENSITIVE
        if (privateEditor) return Rejection.PRIVATE_EDITOR
        if (text.isBlank()) return Rejection.EMPTY
        if (text.length > MAX_TEXT_LENGTH) return Rejection.TOO_LONG
        val normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
        if (Regex("(?i)(LHK-[a-z0-9_-]{10,}|lhk_device_[a-z0-9_-]{10,})").containsMatchIn(normalized) || '\u0000' in normalized || privateTerms.containsMatchIn(normalized) ||
            secretValue.containsMatchIn(normalized) || shortCode.matches(normalized.trim()) ||
            longNumber.containsMatchIn(normalized)
        ) return Rejection.SECRET_OR_PAYMENT
        return null
    }
}
