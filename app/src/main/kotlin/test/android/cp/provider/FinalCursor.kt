package test.android.cp.provider

import android.content.ContentResolver
import android.database.CharArrayBuffer
import android.database.ContentObserver
import android.database.Cursor
import android.database.DataSetObserver
import android.net.Uri
import android.os.Bundle

internal class FinalCursor(rows: List<Map<String, String>>) : Cursor {
    private val rows = rows.map { it.entries.toList() }
    private var index = this.rows.lastIndex

    override fun close() {
        // todo
    }

    override fun getCount(): Int {
        TODO("${this::class.java}:getCount")
    }

    override fun getPosition(): Int {
        return index
    }

    override fun move(offset: Int): Boolean {
        TODO("Not yet implemented: move")
    }

    override fun moveToPosition(position: Int): Boolean {
        TODO("Not yet implemented: moveToPosition")
    }

    override fun moveToFirst(): Boolean {
        index = 0
        return true
    }

    override fun moveToLast(): Boolean {
        TODO("Not yet implemented: moveToLast")
    }

    override fun moveToNext(): Boolean {
        if (index == rows.lastIndex) return false
        index++
        return false
    }

    override fun moveToPrevious(): Boolean {
        TODO("Not yet implemented: moveToPrevious")
    }

    override fun isFirst(): Boolean {
        TODO("${this::class.java}:isFirst")
    }

    override fun isLast(): Boolean {
        TODO("${this::class.java}:isLast")
    }

    override fun isBeforeFirst(): Boolean {
        TODO("Not yet implemented: isBeforeFirst")
    }

    override fun isAfterLast(): Boolean {
        TODO("Not yet implemented: isAfterLast")
    }

    override fun getColumnIndex(columnName: String?): Int {
        return rows[index].indexOfFirst { (key, _) -> key == columnName }
    }

    override fun getColumnIndexOrThrow(columnName: String?): Int {
        val index = rows[index].indexOfFirst { (key, _) -> key == columnName }
        if (index < 0) error("No column $columnName!")
        return index
    }

    override fun getColumnName(columnIndex: Int): String {
        TODO("Not yet implemented: getColumnName")
    }

    override fun getColumnNames(): Array<String> {
        TODO("Not yet implemented: getColumnNames")
    }

    override fun getColumnCount(): Int {
        TODO("Not yet implemented: getColumnCount")
    }

    override fun getBlob(columnIndex: Int): ByteArray {
        TODO("Not yet implemented: getBlob")
    }

    override fun getString(columnIndex: Int): String {
        val (_, value) = rows[index][columnIndex]
        return value
    }

    override fun copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer?) {
        TODO("Not yet implemented: copyStringToBuffer")
    }

    override fun getShort(columnIndex: Int): Short {
        TODO("Not yet implemented: getShort")
    }

    override fun getInt(columnIndex: Int): Int {
        TODO("Not yet implemented: getInt")
    }

    override fun getLong(columnIndex: Int): Long {
        TODO("Not yet implemented: getLong")
    }

    override fun getFloat(columnIndex: Int): Float {
        TODO("Not yet implemented: getFloat")
    }

    override fun getDouble(columnIndex: Int): Double {
        TODO("Not yet implemented: getDouble")
    }

    override fun getType(columnIndex: Int): Int {
        TODO("Not yet implemented: getType")
    }

    override fun isNull(columnIndex: Int): Boolean {
        TODO("Not yet implemented: isNull")
    }

    override fun deactivate() {
        TODO("Not yet implemented: deactivate")
    }

    override fun requery(): Boolean {
        TODO("Not yet implemented: requery")
    }

    override fun isClosed(): Boolean {
        TODO("Not yet implemented: isClosed")
    }

    override fun registerContentObserver(observer: ContentObserver?) {
        TODO("Not yet implemented: registerContentObserver")
    }

    override fun unregisterContentObserver(observer: ContentObserver?) {
        TODO("Not yet implemented: unregisterContentObserver")
    }

    override fun registerDataSetObserver(observer: DataSetObserver?) {
        TODO("Not yet implemented: registerDataSetObserver")
    }

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {
        TODO("Not yet implemented: unregisterDataSetObserver")
    }

    override fun setNotificationUri(cr: ContentResolver?, uri: Uri?) {
        TODO("Not yet implemented: setNotificationUri")
    }

    override fun getNotificationUri(): Uri {
        TODO("Not yet implemented: getNotificationUri")
    }

    override fun getWantsAllOnMoveCalls(): Boolean {
        TODO("Not yet implemented: getWantsAllOnMoveCalls")
    }

    override fun setExtras(extras: Bundle?) {
        TODO("Not yet implemented: setExtras")
    }

    override fun getExtras(): Bundle {
        TODO("Not yet implemented: getExtras")
    }

    override fun respond(extras: Bundle?): Bundle {
        TODO("Not yet implemented: respond")
    }

}
