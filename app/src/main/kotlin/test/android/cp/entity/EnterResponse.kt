package test.android.cp.entity

internal class EnterResponse(
    val encryptedPayload: ByteArray,
    val signature: ByteArray,
)
