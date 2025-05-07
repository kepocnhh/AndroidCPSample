package test.android.cp.entity

import java.util.UUID
import kotlin.time.Duration

internal class EnterRequest(
    val authority: String,
    val time: Duration,
    val id: UUID,
)
