package test.android.cp.module.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import sp.kx.bytes.toHEX
import sp.kx.logics.Logics
import test.android.cp.entity.Keys
import test.android.cp.provider.Injection
import java.security.PrivateKey

internal class AuthLogics(
    private val injection: Injection,
) : Logics(injection.contexts.main) {
    sealed interface Event {
        class OnAuth(val result: Result<Pair<Keys, ByteArray>>) : Event
    }

    private val _events = MutableSharedFlow<Event>()
    val events = _events.asSharedFlow()

    private val logger = injection.loggers.create("[Auth]")

    fun auth(
        file: String,
        password: String,
        alias: String,
        pin: String,
    ) = launch {
        val result = withContext(injection.contexts.default) {
            runCatching {
                if (password.isBlank()) error("Password is blank!")
                if (pin.isBlank()) error("PIN is blank!")
                logger.debug("read \"$file\"...")
                val keyStore = injection.assets.getAsset(name = file).use {
                    logger.debug("load key store...")
                    injection.secrets.toKeyStore(it.readBytes(), password = password.toCharArray())
                }
                val privateKey = keyStore.getKey(alias, password.toCharArray()) ?: error("No \"$alias\"!")
                logger.debug("private:key: ${injection.secrets.sha256(privateKey.encoded).toHEX()}")
                check(privateKey is PrivateKey)
                val certificate = keyStore.getCertificate(alias)
                logger.debug("certificate: ${injection.secrets.sha256(certificate.encoded).toHEX()}")
                val publicKey = certificate.publicKey
                logger.debug("public:key: ${injection.secrets.sha256(publicKey.encoded).toHEX()}")
                val secretKey = injection.secrets.getSecretKey(password = pin.toCharArray())
                logger.debug("secret:key: ${injection.secrets.sha256(secretKey.encoded).toHEX()}")
                val keys = Keys(
                    publicKey = publicKey.encoded,
                    privateKeyEncrypted = injection.secrets.encrypt(secretKey, privateKey.encoded),
                )
                keys to privateKey.encoded
            }
        }
        _events.emit(Event.OnAuth(result))
    }
}
