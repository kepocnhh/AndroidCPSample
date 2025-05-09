package test.android.cp.provider

import test.android.cp.entity.EnterSalt
import test.android.cp.entity.EnterState

internal class Sessions(
    var privateKey: ByteArray?,
    var enterSalt: EnterSalt?,
    var enterState: EnterState?,
)
