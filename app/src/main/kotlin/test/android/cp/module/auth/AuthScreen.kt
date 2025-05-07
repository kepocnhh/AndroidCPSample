package test.android.cp.module.auth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import sp.kx.bytes.toHEX
import sp.kx.bytes.write
import test.android.cp.App
import test.android.cp.BuildConfig
import test.android.cp.entity.AuthorizedPackage
import test.android.cp.entity.Keys
import test.android.cp.provider.Logger
import test.android.cp.provider.Secrets
import test.android.cp.util.query
import test.android.cp.util.showToast
import test.android.cp.util.single
import java.util.Date
import java.util.UUID

private fun PackageInfo.getPublicKey(
    context: Context,
    logger: Logger,
    secrets: Secrets,
): ByteArray? {
    for (provider in providers ?: return null) {
        if (!provider.exported) continue
        if (!provider.enabled) continue
        if (BuildConfig.APPLICATION_ID == packageName) continue
        if (provider.readPermission != BuildConfig.PROVIDER_PERMISSION) continue
        val authority = provider.authority ?: continue
        val uri = Uri.Builder()
            .scheme("content")
            .authority(authority)
            .appendPath("getPublicKey")
            .build()
        val value = try {
            context.contentResolver.query(uri = uri).use { cursor: Cursor? ->
                if (cursor == null) error("No cursor!")
                cursor.single {
                    it.getString(it.getColumnIndexOrThrow("publicKey"))
                }
            }
        } catch (error: Throwable) {
            logger.warning("Query $authority error: $error")
            continue
        }
        return secrets.fromBase64(value)
    }
    return null
}

private fun PackageInfo.getActivity(): String? {
    for (activity in activities ?: return null) {
        if (!activity.exported) continue
        if (!activity.enabled) continue
        if (BuildConfig.APPLICATION_ID == packageName) continue
        if (activity.permission != BuildConfig.PROVIDER_PERMISSION) continue
        return activity.name ?: continue
    }
    return null
}

private fun getAuthorizedPackages(
    context: Context,
    logger: Logger,
    secrets: Secrets,
): List<AuthorizedPackage> {
    val result = mutableListOf<AuthorizedPackage>()
    val packages = context.packageManager.getInstalledPackages(PackageManager.GET_PROVIDERS or PackageManager.GET_ACTIVITIES)
//    logger.debug("packages: ${packages.sortedBy { it.packageName }.joinToString(separator = "\n") { it.packageName }}")
    for (pcg in packages) {
        val publicKey = pcg.getPublicKey(context, logger, secrets) ?: continue
        val activity = pcg.getActivity() ?: continue
        val ap = AuthorizedPackage(
            name = pcg.packageName,
            activity = activity,
            publicKey = publicKey,
        )
        result.add(ap)
    }
    return result
}

private fun getAuthorities(context: Context): Set<String> {
    val result = mutableSetOf<String>()
    val packages = context.packageManager.getInstalledPackages(PackageManager.GET_PROVIDERS)
    for (pcg in packages) {
        val providers = pcg.providers ?: continue
        for (provider in providers) {
            if (!provider.exported) continue
            if (!provider.enabled) continue
            if (BuildConfig.APPLICATION_ID == pcg.packageName) continue
            if (provider.readPermission != BuildConfig.PROVIDER_PERMISSION) continue
            result += provider.authority
        }
    }
    return result
}

private fun getActivities(context: Context): Map<String, Set<String>> {
    val result = mutableMapOf<String, MutableSet<String>>()
    val packages = context.packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
    for (pcg in packages) {
//        println("[Foo]${pcg.packageName}: activities: ${pcg.activities?.toList()}")
        val activities = pcg.activities ?: continue
        for (activity in activities) {
//            println("[Foo]${pcg.packageName}: activity: ${activity.name}")
            if (!activity.exported) continue
            if (!activity.enabled) continue
            if (BuildConfig.APPLICATION_ID == pcg.packageName) continue
            if (activity.permission != BuildConfig.PROVIDER_PERMISSION) continue
            result.getOrPut(pcg.packageName, ::HashSet) += activity.name ?: continue
        }
    }
    return result
}

private fun onEnter(
    logger: Logger,
    authorizedPackage: AuthorizedPackage,
    launcher: ActivityResultLauncher<Intent>,
) {
    val injection = App.injection
    val intent = Intent()
    intent.setComponent(ComponentName(authorizedPackage.name, authorizedPackage.activity))
    val authority = BuildConfig.PROVIDER_AUTHORITY
    logger.debug("authority: $authority")
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
    val id = UUID.randomUUID()
    logger.debug("request id: $id")
    payload.write(index = index, id)
    val sk = injection.secrets.newSecretKey()
    val pb = injection.secrets.toPublicKey(authorizedPackage.publicKey)
    intent.putExtra("encryptedSecretKey", injection.secrets.toBase64(injection.secrets.encrypt(pb, sk.encoded)))
    intent.putExtra("encryptedPayload", injection.secrets.toBase64(injection.secrets.encrypt(sk, payload)))
    launcher.launch(intent)
}

@Composable
internal fun AuthScreen(
    onAuth: (Keys, ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val logger = remember { App.injection.loggers.create("[Auth]") }
    val secrets = remember { App.injection.secrets }
    val logics = App.logics<AuthLogics>()
    LaunchedEffect(Unit) {
        logics.events.collect { event ->
            when (event) {
                is AuthLogics.Event.OnAuth -> {
                    event.result.fold(
                        onSuccess = { (keys, privateKey: ByteArray) ->
                            onAuth(keys, privateKey)
                        },
                        onFailure = { error ->
                            logger.warning("auth error: $error")
                            context.showToast("auth error: $error")
                        },
                    )
                }
            }
        }
    }
    val fileState = remember { mutableStateOf("a202.pkcs12") } // todo
    val passwordState = remember { mutableStateOf("qwe202") } // todo
    val aliasState = remember { mutableStateOf("a202") } // todo
    val pinState = remember { mutableStateOf("0202") } // todo
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { output ->
        val encryptedPayload = output.data?.getByteArrayExtra("encryptedPayload")
        logger.debug("result: ${output.resultCode}\nencryptedPayload: ${encryptedPayload?.let(App.injection.secrets::sha256)?.toHEX()}")
        // todo check
        // todo delete salt
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicText("file")
            BasicTextField(
                modifier = Modifier.fillMaxWidth()
                    .height(36.dp)
                    .background(Color.LightGray)
                    .wrapContentHeight(),
                value = fileState.value,
                onValueChange = { fileState.value = it },
            )
            BasicText("password")
            BasicTextField(
                modifier = Modifier.fillMaxWidth()
                    .height(36.dp)
                    .background(Color.LightGray)
                    .wrapContentHeight(),
                value = passwordState.value,
                onValueChange = { passwordState.value = it },
            )
            BasicText("alias")
            BasicTextField(
                modifier = Modifier.fillMaxWidth()
                    .height(36.dp)
                    .background(Color.LightGray)
                    .wrapContentHeight(),
                value = aliasState.value,
                onValueChange = { aliasState.value = it },
            )
            BasicText("pin")
            BasicTextField(
                modifier = Modifier.fillMaxWidth()
                    .height(36.dp)
                    .background(Color.LightGray)
                    .wrapContentHeight(),
                value = pinState.value,
                onValueChange = { pinState.value = it },
            )
            BasicText(
                modifier = Modifier.fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        logics.auth(
                            file = fileState.value,
                            password = passwordState.value,
                            alias = aliasState.value,
                            pin = pinState.value,
                        )
                    }
                    .wrapContentSize(),
                text = "auth",
            )
            val aps = remember { getAuthorizedPackages(context = context, logger = logger, secrets = secrets) }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                aps.forEachIndexed { index, authorizedPackage ->
                    item(key = "$index/${authorizedPackage.name}") {
                        val text = """
                            pcg: ${authorizedPackage.name}
                            activity: ${authorizedPackage.activity}
                            public key: ${secrets.sha256(authorizedPackage.publicKey).toHEX()}
                        """.trimIndent()
                        BasicText(
                            modifier = Modifier.fillMaxWidth()
                                .background(Color.Yellow)
                                .clickable {
                                    logger.debug("on enter: ${authorizedPackage.name} ${authorizedPackage.activity}")
                                    onEnter(
                                        logger = logger,
                                        authorizedPackage = authorizedPackage,
                                        launcher = launcher,
                                    )
                                }
                                .wrapContentHeight(),
                            text = text,
                        )
                    }
                }
            }
        }
    }
}
