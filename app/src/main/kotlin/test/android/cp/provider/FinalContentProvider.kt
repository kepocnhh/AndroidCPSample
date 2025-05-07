package test.android.cp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import test.android.cp.App
import test.android.cp.BuildConfig
import test.android.cp.entity.EnterSalt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class FinalContentProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        when (uri.authority) {
            BuildConfig.PROVIDER_AUTHORITY -> {
                val injection = App.injection
                when (uri.path) {
                    "/getPublicKey" -> {
                        val publicKey = injection.locals.keys?.publicKey ?: error("No public key!")
                        val cursor = MatrixCursor(arrayOf("publicKey"))
                        cursor.addRow(arrayOf(injection.secrets.toBase64String(publicKey)))
                        return cursor
                    }
                    "/putSalt" -> {
                        val time = uri.getQueryParameter("time")?.toLongOrNull()?.milliseconds ?: error("No time!")
                        if (injection.times.now() - time > 30.seconds) error("Wrong time!")
                        val encryptedSalt = uri.getQueryParameter("encryptedSalt")?.let(injection.secrets::fromBase64) ?: error("No encrypted salt!")
                        val signature = uri.getQueryParameter("signature")?.let(injection.secrets::fromBase64) ?: error("No signature!")
                        injection.sessions.enterSalt = EnterSalt(
                            time = time,
                            encryptedSalt = encryptedSalt,
                            signature = signature,
                        )
                        val cursor = MatrixCursor(arrayOf("code"))
                        cursor.addRow(arrayOf(200))
                        return cursor
                    }
                }
            }
        }
        error("No match uri $uri!")
    }

    override fun getType(uri: Uri): String? {
        TODO("${this::class.java}:getType($uri)")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        TODO("${this::class.java}:insert($uri)")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        TODO("${this::class.java}:delete($uri)")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        TODO("${this::class.java}:update($uri)")
    }
}
