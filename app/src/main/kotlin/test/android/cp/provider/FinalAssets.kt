package test.android.cp.provider

import android.content.Context
import java.io.InputStream

internal class FinalAssets(
    private val context: Context,
) : Assets {
    override fun getAsset(name: String): InputStream {
        return context.assets.open(name)
    }
}
