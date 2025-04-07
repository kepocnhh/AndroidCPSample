package test.android.cp.provider

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import test.android.cp.BuildConfig
import test.android.cp.entity.Keys

internal class FinalLocals(context: Context) : Locals {
    private val prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)

    override var keys: Keys?
        get() {
            if (!prefs.getBoolean("keys", false)) return null
            return Keys(
                publicKey = prefs.getBytes("keys:publicKey"),
                privateKeyEncrypted = prefs.getBytes("keys:privateKeyEncrypted"),
            )
        }
        set(value) {
            if (value == null) {
                prefs.edit()
                    .putBoolean("keys", false)
                    .remove("keys:publicKey")
                    .remove("keys:privateKeyEncrypted")
                    .commit()
            } else {
                prefs.edit()
                    .putBoolean("keys", true)
                    .putString("keys:publicKey", value.publicKey)
                    .putString("keys:privateKeyEncrypted", value.privateKeyEncrypted)
                    .commit()
            }
        }

    companion object {
        private fun SharedPreferences.Editor.putString(key: String, bytes: ByteArray): SharedPreferences.Editor {
            return putString(key, Base64.encodeToString(bytes, Base64.DEFAULT))
        }

        private fun SharedPreferences.getBytes(key: String): ByteArray {
            val text = getString(key, null) ?: error("No \"$key\"!")
            return Base64.decode(text, Base64.DEFAULT)
        }
    }
}
