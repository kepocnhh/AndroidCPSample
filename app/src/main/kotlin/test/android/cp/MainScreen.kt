package test.android.cp

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.security.MessageDigest

@Composable
internal fun MainScreen() {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Main|Foo]") }
    val info = remember {
        context.packageManager.getPackageInfo(BuildConfig.APPLICATION_ID, PackageManager.GET_SIGNING_CERTIFICATES)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = "APPLICATION_ID: ${BuildConfig.APPLICATION_ID}",
            )
            BasicText(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = "BUILD_TYPE: ${BuildConfig.BUILD_TYPE}",
            )
            BasicText(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                text = "FLAVOR: ${BuildConfig.FLAVOR}",
            )
            val signers = info.signingInfo!!.apkContentsSigners
            for (i in signers.indices) {
                BasicText(
                    modifier = Modifier.fillMaxWidth(),
                    text = "$i/${signers.size - 1}:",
                )
                val bytes = signers[i].toByteArray()
                val hex = md.digest(bytes).joinToString(separator = "") { String.format("%02x", it) }
                BasicText(
                    modifier = Modifier.fillMaxWidth(),
                    text = hex,
                    style = TextStyle(fontFamily = FontFamily.Monospace),
                )
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
