package test.android.cp.provider

import java.io.InputStream

internal interface Assets {
    fun getAsset(name: String): InputStream
}
