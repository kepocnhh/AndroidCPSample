package test.android.cp.module.enter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import test.android.cp.App
import test.android.cp.util.showToast

@Composable
internal fun EnterScreen(
    onEnter: (ByteArray) -> Unit,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Enter]") }
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
    Box(modifier = Modifier.fillMaxSize()) {

    }
}
