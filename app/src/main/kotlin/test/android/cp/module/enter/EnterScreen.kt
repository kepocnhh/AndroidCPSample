package test.android.cp.module.enter

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import test.android.cp.App
import test.android.cp.util.showToast

@Composable
internal fun EnterScreen(
    onEnter: (privateKey: ByteArray) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val logger = remember { App.injection.loggers.create("[Enter]") }
    val logics = App.logics<EnterLogics>()
    LaunchedEffect(Unit) {
        logics.events.collect { event ->
            when (event) {
                is EnterLogics.Event.OnEnter -> {
                    event.result.fold(
                        onSuccess = onEnter,
                        onFailure = { error ->
                            logger.warning("enter error: $error")
                            context.showToast("enter error: $error")
                        },
                    )
                }
            }
        }
    }
    val pinState = remember { mutableStateOf("0202") } // todo
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                        logger.debug("enter...")
                        logics.enter(pin = pinState.value)
                    }
                    .wrapContentSize(),
                text = "enter",
            )
            BasicText(
                modifier = Modifier.fillMaxWidth()
                    .height(64.dp)
                    .clickable {
                        onExit()
                    }
                    .wrapContentSize(),
                text = "exit",
            )
        }
    }
}
