package test.android.cp.entity

import java.util.UUID
import kotlin.time.Duration

internal class EnterRequest(
    val publicKey: ByteArray,
    val time: Duration,
    val id: UUID,
    val signature: ByteArray,
)
