package test.android.cp.provider

interface Logger {
    interface Factory {
        fun create(tag: String): Logger
    }

    fun debug(message: String)
}
