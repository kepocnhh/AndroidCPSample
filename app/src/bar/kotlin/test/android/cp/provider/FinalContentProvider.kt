package test.android.cp.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

internal class FinalContentProvider : ContentProvider() {
    private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

    init {
        uriMatcher.addURI("test.android.cp.bar.authority.provider", "persons", 1)
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
                val cursor = MatrixCursor(arrayOf("firstName", "lastName"))
                cursor.addRow(arrayOf("fn:bar:1", "ln:bar:1"))
                cursor.addRow(arrayOf("fn:bar:2", "ln:bar:2"))
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
