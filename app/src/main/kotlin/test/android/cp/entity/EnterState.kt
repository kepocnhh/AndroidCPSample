package test.android.cp.entity

import java.security.PublicKey
import java.util.UUID
import javax.crypto.SecretKey

internal class EnterState(
    val id: UUID,
    val secretKey: SecretKey,
    val publicKey: PublicKey,
)
