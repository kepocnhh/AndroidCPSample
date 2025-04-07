package test.android.cp.module.router

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import test.android.cp.App
import test.android.cp.module.auth.AuthScreen
import test.android.cp.module.enter.EnterScreen

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
                TODO("RouterScreen:state: $state")
            } else {
                EnterScreen(
                    onEnter = logics::enter,
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
