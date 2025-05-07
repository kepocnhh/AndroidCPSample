package test.android.cp.module.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import sp.kx.bytes.toHEX
import test.android.cp.App

@Composable
internal fun MainScreen(
    publicKey: ByteArray,
    onLock: () -> Unit,
) {
    val context = LocalContext.current
    val logger = remember { App.injection.loggers.create("[Main]") }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BasicText(
                modifier = Modifier.fillMaxWidth(),
                text = App.injection.secrets.sha256(publicKey).toHEX(),
                style = TextStyle(fontFamily = FontFamily.Monospace),
            )
            BasicText(
                modifier = Modifier.fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        onLock()
                    }
                    .wrapContentSize(),
                text = "lock",
            )
        }
    }
}
