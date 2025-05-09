package test.android.cp.module.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import sp.kx.logics.Logics
import test.android.cp.entity.AuthorizedPackage
import test.android.cp.entity.Keys
import test.android.cp.provider.Injection
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

internal class AuthLogics(
    private val injection: Injection,
) : Logics(injection.contexts.main) {
    sealed interface Event {
        class OnAuth(val result: Result<Pair<Keys, ByteArray>>) : Event
        class OnEnter(
            val id: UUID,
            val authorizedPackage: AuthorizedPackage,
            val secretKey: SecretKey,
            val publicKey: PublicKey,
            val encryptedSecretKey: ByteArray,
            val encryptedPayload: ByteArray,
        ) : Event
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

    fun enter(authorizedPackage: AuthorizedPackage, authority: String) = launch {
        logger.debug("on enter: ${authorizedPackage.name} ${authorizedPackage.activity} $authority")
        val event = withContext(injection.contexts.default) {
            val authorityEncoded = injection.secrets.toBase64(authority)
            val payload = ByteArray(4 + authorityEncoded.size + 8 + 16)
            var index = 0
            payload.write(index = index, authorityEncoded.size)
            index += 4
            System.arraycopy(authorityEncoded, 0, payload, index, authorityEncoded.size)
            index += authorityEncoded.size
            val time = injection.times.now()
            logger.debug("request time: ${Date(time.inWholeMilliseconds)}")
            payload.write(index = index, time.inWholeMilliseconds)
            index += 8
            val id = injection.secrets.newUUID()
            logger.debug("request id: $id")
            payload.write(index = index, id)
            val secretKey = injection.secrets.newSecretKey()
            val publicKey = injection.secrets.toPublicKey(authorizedPackage.publicKey)
            Event.OnEnter(
                id = id,
                authorizedPackage = authorizedPackage,
                secretKey = secretKey,
                publicKey = publicKey,
                encryptedSecretKey = injection.secrets.toBase64(injection.secrets.encrypt(publicKey, secretKey.encoded)),
                encryptedPayload = injection.secrets.toBase64(injection.secrets.encrypt(secretKey, payload)),
            )
        }
        _events.emit(event)
    }
}
