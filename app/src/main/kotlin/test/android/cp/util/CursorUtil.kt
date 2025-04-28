package test.android.cp.util

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

internal fun ContentResolver.query(uri: Uri, projection: Array<String> = emptyArray()): Cursor? {
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

internal fun <T : Any> Cursor.single(transform: (Cursor) -> T): T {
    if (!moveToFirst()) throw NoSuchElementException("Cursor is empty.")
    val value = transform(this)
    if (moveToNext()) throw IllegalArgumentException("Cursor has more than one element.")
    return value
}

internal fun <T : Any> Cursor.single(key: String, transform: (Cursor, Int) -> T): T {
    if (!moveToFirst()) throw NoSuchElementException("Cursor is empty.")
    val value = transform(this, getColumnIndexOrThrow(key))
    if (moveToNext()) throw IllegalArgumentException("Cursor has more than one element.")
    return value
}

internal fun Cursor.requireString(key: String): String {
    return getString(getColumnIndexOrThrow(key))
}
