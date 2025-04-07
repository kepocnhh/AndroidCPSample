package test.android.cp

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import sp.kx.logics.Logics
import sp.kx.logics.LogicsFactory
import sp.kx.logics.LogicsProvider
import sp.kx.logics.contains
import sp.kx.logics.get
import sp.kx.logics.remove
import test.android.cp.provider.Contexts
import test.android.cp.provider.FinalAssets
import test.android.cp.provider.FinalLocals
import test.android.cp.provider.FinalLoggers
import test.android.cp.provider.FinalSecrets
import test.android.cp.provider.Injection
import test.android.cp.provider.Logger
import test.android.cp.provider.Sessions

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val loggers = FinalLoggers()
        _loggers = loggers
        _injection = Injection(
            contexts = Contexts(
                main = Dispatchers.Main,
                default = Dispatchers.Default,
            ),
            loggers = loggers,
            locals = FinalLocals(context = this),
            sessions = Sessions(privateKey = null),
            secrets = FinalSecrets(),
            assets = FinalAssets(context = this),
        )
    }

    companion object {
        private var _loggers: Logger.Factory? = null
        val loggers: Logger.Factory get() = checkNotNull(_loggers) { "No loggers!" }

        private var _injection: Injection? = null
        val injection: Injection get() = checkNotNull(_injection) { "No injection!" }

        private val _logicsProvider = LogicsProvider(
            factory = object : LogicsFactory {
                override fun <T : Logics> create(type: Class<T>): T {
                    return type
                        .getConstructor(Injection::class.java)
                        .newInstance(injection)
                }
            },
        )

        @Composable
        inline fun <reified T : Logics> logics(label: String = T::class.java.name): T {
            val (contains, logic) = synchronized(App::class.java) {
                remember { _logicsProvider.contains<T>(label = label) } to _logicsProvider.get<T>(label = label)
            }
            DisposableEffect(Unit) {
                onDispose {
                    synchronized(App::class.java) {
                        if (!contains) _logicsProvider.remove<T>(label = label)
                    }
                }
            }
            return logic
        }
    }
}
