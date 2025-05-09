package test.android.cp.module.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import sp.kx.logics.Logics
import test.android.cp.entity.AuthorizedPackage
import test.android.cp.entity.EnterSalt
import test.android.cp.entity.EnterState
import test.android.cp.entity.Keys
import test.android.cp.provider.Injection
import java.security.PrivateKey
import java.security.PublicKey
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class AuthLogics(
    private val injection: Injection,
) : Logics(injection.contexts.main) {
    sealed interface Event {
        class OnAuth(val result: Result<Pair<Keys, ByteArray>>) : Event
        class OnEnterRequest(
            val authorizedPackage: AuthorizedPackage,
            val encryptedSecretKey: ByteArray,
            val encryptedPayload: ByteArray,
        ) : Event
        class OnEnter(
            val result: Result<PrivateKey>,
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
            injection.sessions.enterState = EnterState(
                id = id,
                secretKey = secretKey,
                publicKey = publicKey,
            )
            Event.OnEnterRequest(
                authorizedPackage = authorizedPackage,
                encryptedSecretKey = injection.secrets.toBase64(injection.secrets.encrypt(publicKey, secretKey.encoded)),
                encryptedPayload = injection.secrets.toBase64(injection.secrets.encrypt(secretKey, payload)),
            )
        }
        _events.emit(event)
    }

    private fun getSalt(
        enterSalt: EnterSalt,
        id: UUID,
        publicKey: PublicKey,
        secretKey: SecretKey,
    ): ByteArray {
        val salt = injection.secrets.decrypt(secretKey, enterSalt.encryptedSalt)
        val signatureData = ByteArray(8 + 16 + salt.size)
        var index = 0
        signatureData.write(index = index, enterSalt.time.inWholeMilliseconds)
        index += 8
        signatureData.write(index = index, id)
        index += 16
        System.arraycopy(signatureData, index, salt, 0, salt.size)
        check(injection.secrets.verify(publicKey, signatureData, enterSalt.signature)) { "Signature enter salt error!" }
        return salt
    }

    private fun getPrivateKey(
        id: UUID,
        publicKey: PublicKey,
        secretKey: SecretKey,
        salt: ByteArray,
        encryptedPayload: ByteArray,
        signature: ByteArray,
    ): PrivateKey {
        val payload = injection.secrets.decrypt(secretKey, encryptedPayload)
        var index = 0
        val privateKey = ByteArray(payload.readInt(index = index))
        index += 4
        System.arraycopy(payload, index, privateKey, 0, privateKey.size)
        index += privateKey.size
        val time = payload.readLong(index = index).milliseconds
        if (injection.times.now() - time > 30.seconds) error("Wrong time!") // todo
        val signatureData = ByteArray(8 + 16 + privateKey.size + salt.size)
        index = 0
        signatureData.write(index = index, time.inWholeMilliseconds)
        index += 8
        signatureData.write(index = index, id)
        index += 16
        System.arraycopy(signatureData, index, privateKey, 0, privateKey.size)
        index += privateKey.size
        System.arraycopy(signatureData, index, salt, 0, salt.size)
        check(injection.secrets.verify(publicKey, signatureData, signature)) { "Signature enter response error!" }
        return injection.secrets.toPrivateKey(privateKey)
    }

    fun onEnterResponse(
        encryptedPayload: ByteArray,
        signature: ByteArray,
    ) = launch {
        logger.debug("on enter response...")
        val result = withContext(injection.contexts.default) {
            runCatching {
                val enterSalt = injection.sessions.enterSalt ?: error("No enter salt!")
                injection.sessions.enterSalt = null
                val state = injection.sessions.enterState ?: error("No enter state!")
                injection.sessions.enterState = null
                val salt = getSalt(
                    enterSalt = enterSalt,
                    id = state.id,
                    publicKey = state.publicKey,
                    secretKey = state.secretKey,
                )
                getPrivateKey(
                    id = state.id,
                    publicKey = state.publicKey,
                    secretKey = state.secretKey,
                    salt = salt,
                    encryptedPayload = encryptedPayload,
                    signature = signature,
                )
            }
        }
        _events.emit(Event.OnEnter(result))
    }
}
