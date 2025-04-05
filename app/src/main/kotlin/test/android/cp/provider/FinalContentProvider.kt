package test.android.cp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import test.android.cp.BuildConfig

internal class FinalContentProvider : ContentProvider() {
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        uriMatcher.addURI("${BuildConfig.APPLICATION_ID}.authority.provider", "app", 1)
    }

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
        when (uriMatcher.match(uri)) {
            1 -> {
                val cursor = MatrixCursor(arrayOf("appId", "versionName"))
                cursor.addRow(arrayOf(BuildConfig.APPLICATION_ID, BuildConfig.VERSION_NAME))
                return cursor
            }
            else -> error("No match uri $uri!")
        }
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
