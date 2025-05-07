package test.android.cp.provider

import test.android.cp.entity.EnterSalt

internal class Sessions(
    var privateKey: ByteArray?,
    var enterSalt: EnterSalt?,
)
