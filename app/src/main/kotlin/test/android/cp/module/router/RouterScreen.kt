package test.android.cp.module.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import test.android.cp.App
import test.android.cp.module.auth.AuthScreen
import test.android.cp.module.enter.EnterScreen
import test.android.cp.module.main.MainScreen

@Composable
internal fun RouterScreen() {
    val logics = App.logics<RouterLogics>()
    val state = logics.states.collectAsState().value
    LaunchedEffect(Unit) {
        if (state == null) logics.requestState()
    }
    when (state) {
        is RouterLogics.State.Keys -> {
            if (state.authorized) {
                MainScreen(
                    publicKey = state.publicKey,
                    onLock = logics::lock,
                )
            } else {
                EnterScreen(
                    onEnter = { privateKey: ByteArray, _ ->
                        logics.enter(privateKey = privateKey)
                    },
                    onExit = logics::exit,
                )
            }
        }
        RouterLogics.State.NoKeys -> {
            AuthScreen(
                onAuth = logics::auth,
            )
        }
        null -> {
            // noop
        }
    }
}
