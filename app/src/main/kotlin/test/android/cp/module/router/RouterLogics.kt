package test.android.cp.module.router

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import sp.kx.logics.Logics
import test.android.cp.entity.Keys
import test.android.cp.provider.Injection

internal class RouterLogics(
    private val injection: Injection,
) : Logics(injection.contexts.main) {
    sealed interface State {
        data object NoKeys : State
        class Keys(val publicKey: ByteArray, val authorized: Boolean) : State
    }

    private val _states = MutableStateFlow<State?>(null)
    val states = _states.asStateFlow()

    fun requestState() = launch {
        val keys = withContext(injection.contexts.default) {
            injection.locals.keys
        }
        if (keys == null) {
            _states.value = State.NoKeys
        } else {
            val authorized = withContext(injection.contexts.default) {
                injection.sessions.privateKey != null
            }
            _states.value = State.Keys(publicKey = keys.publicKey, authorized = authorized)
        }
    }

    fun exit() = launch {
        val keys = withContext(injection.contexts.default) {
            injection.locals.keys
        }
        if (keys == null) TODO()
        withContext(injection.contexts.default) {
            injection.locals.keys = null
        }
        _states.value = State.NoKeys
    }

    fun enter(privateKey: ByteArray) = launch {
        val keys = withContext(injection.contexts.default) {
            injection.locals.keys
        }
        if (keys == null) TODO()
        withContext(injection.contexts.default) {
            injection.sessions.privateKey = privateKey
        }
        _states.value = State.Keys(publicKey = keys.publicKey, authorized = true)
    }

    fun auth(keys: Keys, privateKey: ByteArray) = launch {
        withContext(injection.contexts.default) {
            if (injection.locals.keys != null) TODO()
            injection.locals.keys = keys
        }
        withContext(injection.contexts.default) {
            injection.sessions.privateKey = privateKey
        }
        _states.value = State.Keys(publicKey = keys.publicKey, authorized = true)
    }

    fun lock() = launch {
        val keys = withContext(injection.contexts.default) {
            injection.locals.keys
        }
        if (keys == null) TODO()
        withContext(injection.contexts.default) {
            injection.sessions.privateKey = null
        }
        _states.value = State.Keys(publicKey = keys.publicKey, authorized = false)
    }
}
