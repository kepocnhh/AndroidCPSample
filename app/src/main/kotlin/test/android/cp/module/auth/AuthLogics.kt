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

    private fun getKeys(
        publicKey: PublicKey,
        privateKey: ByteArray,
        password: CharArray,
    ): Keys {
        val secretKey = injection.secrets.getSecretKey(password = password)
        logger.debug("secret:key: ${injection.secrets.sha256(secretKey.encoded).toHEX()}")
        return Keys(
            publicKey = publicKey.encoded,
            privateKeyEncrypted = injection.secrets.encrypt(secretKey, privateKey),
        )
    }

    fun auth(
        file: String,
        keyStorePassword: String,
        alias: String,
        pin: String,
    ) = launch {
        val result = withContext(injection.contexts.default) {
            runCatching {
                if (keyStorePassword.isBlank()) error("KeyStore password is blank!")
                if (pin.isBlank()) error("PIN is blank!")
                logger.debug("read \"$file\"...")
                val keyStore = injection.assets.getAsset(name = file).use {
                    logger.debug("load key store...")
                    injection.secrets.toKeyStore(it.readBytes(), password = keyStorePassword.toCharArray())
                }
                val privateKey = keyStore.getKey(alias, keyStorePassword.toCharArray())?.encoded ?: error("No \"$alias\"!")
                logger.debug("private:key: ${injection.secrets.sha256(privateKey).toHEX()}")
                val certificate = keyStore.getCertificate(alias)
                logger.debug("certificate: ${injection.secrets.sha256(certificate.encoded).toHEX()}")
                val publicKey = certificate.publicKey
                logger.debug("public:key: ${injection.secrets.sha256(publicKey.encoded).toHEX()}")
                val password = injection.secrets.sha256(pin.toByteArray())
                getKeys(
                    publicKey = publicKey,
                    privateKey = privateKey,
                    password = password.toHEX().toCharArray(),
                ) to privateKey
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
        logger.debug("salt: ${injection.secrets.sha256(salt).toHEX()}")
        val signatureData = ByteArray(8 + 16 + salt.size)
        var index = 0
        signatureData.write(index = index, enterSalt.time.inWholeMilliseconds)
        index += 8
        logger.debug("salt time: ${Date(enterSalt.time.inWholeMilliseconds)} ${enterSalt.time.inWholeMilliseconds}")
        signatureData.write(index = index, id)
        index += 16
        logger.debug("salt id: $id")
        System.arraycopy(salt, 0, signatureData, index, salt.size)
        logger.debug("salt signature data: ${injection.secrets.sha256(signatureData).toHEX()}")
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
    ): Pair<ByteArray, ByteArray> {
        val payload = injection.secrets.decrypt(secretKey, encryptedPayload)
        var index = 0
        val privateKey = ByteArray(payload.readInt(index = index))
        index += 4
        System.arraycopy(payload, index, privateKey, 0, privateKey.size)
        index += privateKey.size
        val password = ByteArray(payload.readInt(index = index))
        index += 4
        System.arraycopy(payload, index, password, 0, password.size)
        index += password.size
        val time = payload.readLong(index = index).milliseconds
        if (injection.times.now() - time > 30.seconds) error("Wrong time!") // todo
        val signatureData = ByteArray(8 + 16 + password.size + salt.size)
        index = 0
        signatureData.write(index = index, time.inWholeMilliseconds)
        index += 8
        logger.debug("response time: ${Date(time.inWholeMilliseconds)} ${time.inWholeMilliseconds}")
        signatureData.write(index = index, id)
        index += 16
        logger.debug("response id: $id")
        System.arraycopy(password, 0, signatureData, index, password.size)
        index += password.size
        logger.debug("response password: ${injection.secrets.sha256(password).toHEX()}")
        System.arraycopy(salt, 0, signatureData, index, salt.size)
        logger.debug("response salt: ${injection.secrets.sha256(salt).toHEX()}")
        logger.debug("response signature data: ${injection.secrets.sha256(signatureData).toHEX()}")
        check(injection.secrets.verify(publicKey, signatureData, signature)) { "Signature enter response error!" }
        return privateKey to password
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
                val (privateKey, password) = getPrivateKey(
                    id = state.id,
                    publicKey = state.publicKey,
                    secretKey = state.secretKey,
                    salt = salt,
                    encryptedPayload = encryptedPayload,
                    signature = signature,
                )
                getKeys(
                    publicKey = state.publicKey,
                    privateKey = privateKey,
                    password = password.toHEX().toCharArray(),
                ) to privateKey
            }
        }
        _events.emit(Event.OnAuth(result))
    }
}
