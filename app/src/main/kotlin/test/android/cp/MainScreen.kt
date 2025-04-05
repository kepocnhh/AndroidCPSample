package test.android.cp

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun MainScreen() {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Main|Foo]") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
        ) {
            BasicText(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .wrapContentSize(),
                text = BuildConfig.APPLICATION_ID,
            )
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
