package test.android.cp.entity

import kotlin.time.Duration

internal class EnterSalt(
    val time: Duration,
    val encryptedSalt: ByteArray,
    val signature: ByteArray,
)
