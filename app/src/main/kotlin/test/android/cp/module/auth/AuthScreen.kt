package test.android.cp.module.auth

import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import test.android.cp.App
import test.android.cp.BuildConfig
import test.android.cp.entity.Keys
import test.android.cp.util.showToast

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

@Composable
internal fun AuthScreen(
    onAuth: (Keys, ByteArray) -> Unit,
) {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Auth]") }
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
            val authorities = remember { getAuthorities(context = context) }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                authorities.forEach { authority ->
                    item(key = authority) {
                        BasicText(
                            modifier = Modifier.fillMaxWidth()
                                .height(48.dp)
                                .background(Color.Yellow)
                                .clickable {
                                    // todo
                                }
                                .wrapContentHeight(),
                            text = authority,
                        )
                    }
                }
            }
        }
    }
}
