package test.android.cp

import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import test.android.cp.util.map
import test.android.cp.util.query
import java.security.MessageDigest

@Composable
internal fun MainScreen() {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Main|Foo]") }
    val info = remember {
        context.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNING_CERTIFICATES)
    }
    val apps = remember {
        context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA).sortedBy { it.packageName }
    }
    val packages = remember {
        context.packageManager.getInstalledPackages(PackageManager.GET_PROVIDERS).sortedBy { it.packageName }
    }
    val md = remember { MessageDigest.getInstance("SHA1") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            BasicText(
                text = "APPLICATION_ID: ${BuildConfig.APPLICATION_ID}",
            )
            BasicText(
                text = "BUILD_TYPE: ${BuildConfig.BUILD_TYPE}",
            )
            BasicText(
                text = "FLAVOR: ${BuildConfig.FLAVOR}",
            )
            val signature = info.signingInfo?.apkContentsSigners?.single() ?: TODO()
            val bytes = signature.toByteArray()
            val hex = md.digest(bytes).joinToString(separator = "") { String.format("%02x", it) }
            BasicText(
                text = hex,
                style = TextStyle(fontFamily = FontFamily.Monospace),
            )
            LazyColumn (
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
//                val list = apps
                val list = packages
                for (i in list.indices) {
                    val it = list[i]
//                    if (!it.packageName.startsWith("test")) continue // todo
                    val providers = it.providers ?: continue
                    for (j in providers.indices) {
                        val provider = providers[j]
                        if (!provider.exported) continue
                        if (!provider.enabled) continue
//                        val readPermission = provider.readPermission ?: continue // todo
                        if (provider.readPermission != BuildConfig.PROVIDER_PERMISSION) continue
                        if (BuildConfig.APPLICATION_ID == it.packageName) continue // todo
                        val b = context.packageManager
                            .getPackageInfo(it.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                            .signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray() ?: continue
//                        if (!bytes.contentEquals(b)) continue // todo
                        val h = md.digest(b).joinToString(separator = "") { String.format("%02x", it) }
                        val authority = provider.authority ?: continue
                        val uri = Uri.Builder()
                            .scheme("content")
                            .authority(authority)
                            .appendPath("app")
                            .build()
//                        val appId: String? = null
                        val appId = try {
                            context.contentResolver.query(uri = uri, projection = arrayOf("appId")).use { cursor: Cursor? ->
                                if (cursor == null) error("No cursor!")
                                cursor.map {
                                    it.getString(it.getColumnIndexOrThrow("appId"))
                                }
                            }.single()
                        } catch (error: Throwable) {
                            logger.warning("Query $authority error: $error")
                            continue
                        }
                        item(key = "$i/$j") {
                            BasicText(text = "$i/$j:")
                            val text = """
                                appId: $appId
                                pcg: ${it.packageName}
                                sig: $h
                                authority: $authority
                                enabled: ${provider.enabled}
                                exported: ${provider.exported}
                                readPermission: ${provider.readPermission}
                                writePermission: ${provider.writePermission}
                                grantUriPermissions: ${provider.grantUriPermissions}
                                uriPermissionPatterns: ${provider.uriPermissionPatterns?.map { it.path }}
                                pathPermissions: ${provider.pathPermissions?.map { it.path }}
                            """.trimIndent()
                            BasicText(text = text)
                        }
                    }
                }
            }
            BasicText(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        // todo
                    }
                    .wrapContentSize(),
                text = "click",
            )
        }
    }
}
