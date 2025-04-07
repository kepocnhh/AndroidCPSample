package test.android.cp.provider

import java.security.KeyStore
import javax.crypto.SecretKey

internal interface Secrets {
    fun getSecretKey(password: CharArray): SecretKey
    fun sha256(encoded: ByteArray): ByteArray
    fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    fun toKeyStore(encoded: ByteArray, password: CharArray): KeyStore
    fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray
}
