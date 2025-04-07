package test.android.cp.module.enter

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import sp.kx.bytes.toHEX
import sp.kx.logics.Logics
import test.android.cp.provider.Injection

internal class EnterLogics(
    private val injection: Injection,
) : Logics(injection.contexts.main) {
    sealed interface Event {
        class OnEnter(val result: Result<ByteArray>) : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val logger = injection.loggers.create("[Enter]")

    fun enter(pin: String) = launch {
        val result = withContext(injection.contexts.default) {
            runCatching {
                if (pin.isBlank()) error("PIN is blank!")
                val secretKey = injection.secrets.getSecretKey(password = pin.toCharArray())
                logger.debug("secret:key: ${injection.secrets.sha256(secretKey.encoded).toHEX()}")
                val keys = injection.locals.keys ?: TODO()
                val decrypted = injection.secrets.decrypt(secretKey, keys.privateKeyEncrypted)
                logger.debug("private:key: ${injection.secrets.sha256(decrypted).toHEX()}")
                decrypted
            }
        }
        _events.emit(Event.OnEnter(result))
    }
}
