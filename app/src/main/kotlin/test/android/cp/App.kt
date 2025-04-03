package test.android.cp

import android.app.Application
import test.android.cp.provider.FinalLoggers
import test.android.cp.provider.Logger

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        _loggers = FinalLoggers()
    }

    companion object {
        private var _loggers: Logger.Factory? = null
        val loggers: Logger.Factory get() = checkNotNull(_loggers) { "No loggers!" }
    }
}
