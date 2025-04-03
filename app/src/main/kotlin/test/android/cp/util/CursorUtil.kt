package test.android.cp.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

internal fun ContentResolver.query(uri: Uri, projection: Array<String>): Cursor? {
    val selection: String? = null
    val selectionArgs: Array<String> = arrayOf()
    val sortOrder: String? = null
    return query(uri, projection, selection, selectionArgs, sortOrder)
}

internal fun <T : Any> Cursor.map(transform: (Cursor) -> T): List<T> {
    if (!moveToFirst()) return emptyList()
    val list = mutableListOf<T>()
    do {
        list.add(transform(this))
    } while (moveToNext())
    return list
}
