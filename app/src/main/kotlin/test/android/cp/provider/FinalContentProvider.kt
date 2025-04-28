package test.android.cp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import test.android.cp.App
import test.android.cp.BuildConfig

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
                when (uri.path) {
                    "/getPublicKey" -> {
                        val publicKey = App.injection.locals.keys?.publicKey ?: error("No public key!")
                        val cursor = MatrixCursor(arrayOf("publicKey"))
                        cursor.addRow(arrayOf(App.injection.secrets.base64(publicKey)))
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
