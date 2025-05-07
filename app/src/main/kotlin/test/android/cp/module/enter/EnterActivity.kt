package test.android.cp.module.enter

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sp.kx.bytes.readInt
import sp.kx.bytes.readLong
import sp.kx.bytes.readUUID
import sp.kx.bytes.toByteArray
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import test.android.cp.App
import test.android.cp.entity.EnterRequest
import test.android.cp.entity.EnterResponse
import test.android.cp.provider.Secrets
import test.android.cp.provider.Times
import test.android.cp.util.query
import test.android.cp.util.single
import java.security.PrivateKey
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class EnterActivity : ComponentActivity() {
    private val logger = App.injection.loggers.create("[Enter|Activity]")

    private fun getEnterResponse(
        privateKey: PrivateKey,
        secretKey: SecretKey,
        time: Duration,
        salt: ByteArray,
        id: UUID,
        secrets: Secrets,
    ): EnterResponse {
        val privateKeySize = privateKey.encoded.size
        val payload = ByteArray(4 + privateKeySize + 8)
        var index = 0
        payload.write(index = index, privateKeySize)
        index += 4
        System.arraycopy(privateKey.encoded, 0, payload, index, privateKeySize)
        index += privateKeySize
        payload.write(index = index, time.inWholeMilliseconds)
        val signatureData = ByteArray(8 + 16 + salt.size)
        index = 0
        signatureData.write(index = index, time.inWholeMilliseconds)
        index += 8
        signatureData.write(index = index, id)
        index += 16
        System.arraycopy(salt, 0, signatureData, index, salt.size)
        return EnterResponse(
            encryptedPayload = secrets.encrypt(secretKey, payload),
            signature = secrets.sign(privateKey, signatureData),
        )
    }

    private fun putSalt(
        time: Duration,
        id: UUID,
        salt: ByteArray,
        privateKey: PrivateKey,
        secretKey: SecretKey,
        authority: String,
        secrets: Secrets,
    ) {
        val encoded = ByteArray(8 + 16 + salt.size)
        var index = 0
        encoded.write(index = index, time.inWholeMilliseconds)
        index += 8
        encoded.write(index = index, id)
        System.arraycopy(salt, 0, encoded, 8 + 16, salt.size)
        val encryptedSalt = secrets.encrypt(secretKey, salt)
        val signature = secrets.sign(privateKey, encoded)
        val uri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("putSalt")
            .appendQueryParameter("time", time.inWholeMilliseconds.toString())
            .appendQueryParameter("encryptedSalt", secrets.toBase64String(encryptedSalt))
            .appendQueryParameter("signature", secrets.toBase64String(signature))
            .build()
        val code = contentResolver.query(uri = uri).use { cursor: Cursor? ->
            if (cursor == null) error("No cursor!")
            cursor.single {
                it.getInt(it.getColumnIndexOrThrow("code"))
            }
        }
        if (code != 200) error("Wrong code: $code!")
    }

    private fun getEnterRequest(
        requestTime: Duration,
        payload: ByteArray,
        secrets: Secrets,
    ): EnterRequest {
        var index = 0
        val authorityEncoded = ByteArray(payload.readInt(index = index))
        index += 4
        System.arraycopy(payload, index, authorityEncoded, 0, authorityEncoded.size)
        val authority = secrets.fromBase64ToString(authorityEncoded)
        logger.debug("authority: $authority")
        index += authorityEncoded.size
        val time = payload.readLong(index = index).milliseconds
        logger.debug("request time: ${Date(time.inWholeMilliseconds)}")
        if (requestTime - time > 30.seconds) error("Wrong enter request time!")
        index += 8
        val id = payload.readUUID(index = index)
        logger.debug("request id: $id")
        return EnterRequest(
            authority = authority,
            time = time,
            id = id,
        )
    }

    private fun onEnter(
        requestTime: Duration,
        privateKey: ByteArray,
        encryptedSecretKey: ByteArray,
        encryptedPayload: ByteArray,
    ) {
        val injection = App.injection
        lifecycleScope.launch {
            withContext(injection.contexts.default) {
                runCatching {
                    val pk = injection.secrets.toPrivateKey(privateKey) // todo
                    val sk = injection.secrets.toSecretKey(injection.secrets.decrypt(pk, encryptedSecretKey))
                    val enterRequest = getEnterRequest(
                        requestTime = requestTime,
                        payload = injection.secrets.decrypt(sk, encryptedPayload),
                        secrets = injection.secrets,
                    )
                    val salt = UUID.randomUUID().toByteArray()
                    putSalt(
                        time = injection.times.now(),
                        salt = salt,
                        privateKey = pk,
                        secretKey = sk,
                        authority = enterRequest.authority,
                        id = enterRequest.id,
                        secrets = injection.secrets,
                    )
                    getEnterResponse(
                        privateKey = pk,
                        secretKey = sk,
                        time = injection.times.now(),
                        salt = salt,
                        id = enterRequest.id,
                        secrets = injection.secrets,
                    )
                }
            }.fold(
                onFailure = { error ->
                    logger.warning("on enter error: $error")
                    finish()
                },
                onSuccess = { enterResponse ->
                    val intent = Intent()
                    logger.debug("encrypted payload: ${injection.secrets.sha256(enterResponse.encryptedPayload).toHEX()}")
                    intent.putExtra("encryptedPayload", enterResponse.encryptedPayload)
                    intent.putExtra("signature", enterResponse.signature)
                    setResult(RESULT_OK, intent)
                    finish()
                },
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        val view = ComposeView(this)
        setContentView(view)
        val injection = App.injection
        val encryptedSecretKey = try {
            val b64 = intent?.getByteArrayExtra("encryptedSecretKey") ?: error("No encrypted secret key!")
            injection.secrets.fromBase64(b64)
        } catch (error: Throwable) {
            logger.warning("on create error: $error")
            finish()
            return
        }
        val encryptedPayload = try {
            val b64 = intent?.getByteArrayExtra("encryptedPayload") ?: error("No encrypted payload!")
            injection.secrets.fromBase64(b64)
        } catch (error: Throwable) {
            logger.warning("on create error: $error")
            finish()
            return
        }
        val requestTime = injection.times.now()
        view.setContent {
            EnterScreen(
                onEnter = { privateKey: ByteArray ->
                    onEnter(
                        requestTime = requestTime,
                        privateKey = privateKey,
                        encryptedSecretKey = encryptedSecretKey,
                        encryptedPayload = encryptedPayload,
                    )
                },
                onExit = {
                    finish()
                },
            )
        }
    }
}
