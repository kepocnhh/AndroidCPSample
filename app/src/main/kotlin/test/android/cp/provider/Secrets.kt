package test.android.cp.provider

import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey

internal interface Secrets {
    fun getSecretKey(password: CharArray): SecretKey
    fun toSecretKey(encoded: ByteArray): SecretKey
    fun newSecretKey(): SecretKey
    fun toPublicKey(encoded: ByteArray): PublicKey
    fun toPrivateKey(encoded: ByteArray): PrivateKey
    fun sha256(encoded: ByteArray): ByteArray
    fun decrypt(key: SecretKey, encrypted: ByteArray): ByteArray
    fun decrypt(key: PrivateKey, encrypted: ByteArray): ByteArray
    fun toKeyStore(encoded: ByteArray, password: CharArray): KeyStore
    fun encrypt(key: SecretKey, decrypted: ByteArray): ByteArray
    fun encrypt(key: PublicKey, decrypted: ByteArray): ByteArray
    fun fromBase64(text: String): ByteArray
    fun fromBase64(bytes: ByteArray): ByteArray
    fun fromBase64ToString(bytes: ByteArray): String
    fun toBase64String(bytes: ByteArray): String
    fun toBase64(bytes: ByteArray): ByteArray
    fun toBase64(text: String): ByteArray
    fun sign(key: PrivateKey, encoded: ByteArray): ByteArray
    fun verify(key: PublicKey, encoded: ByteArray, signature: ByteArray): Boolean
    fun newUUID(): UUID
}
