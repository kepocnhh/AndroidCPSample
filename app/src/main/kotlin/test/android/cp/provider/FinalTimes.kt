package test.android.cp.provider

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class FinalTimes : Times {
    override fun now(): Duration {
        return System.currentTimeMillis().milliseconds
    }
}
