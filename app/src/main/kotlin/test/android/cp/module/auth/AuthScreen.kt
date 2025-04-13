package test.android.cp.module.auth

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.core.net.toUri

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
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { output ->
        logger.debug("result: ${output.resultCode}\nanswer: ${output.data?.getStringExtra("answer")}")
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
//            val authorities = remember { getAuthorities(context = context) }
            val packages = remember { getActivities(context = context) }
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                packages.forEach { (pcg, activities) ->
                    activities.forEach { activity ->
                        item(key = "$pcg:$activity") {
                            BasicText(
                                modifier = Modifier.fillMaxWidth()
                                    .height(48.dp)
                                    .background(Color.Yellow)
                                    .clickable {
                                        val intent = Intent()
                                        intent.setComponent(ComponentName(pcg, activity))
                                        intent.putExtra("issuer", BuildConfig.APPLICATION_ID)
                                        launcher.launch(intent)
                                    }
                                    .wrapContentHeight(),
                                text = "$pcg\n$activity",
                            )
                        }
                    }
                }
            }
        }
    }
}
