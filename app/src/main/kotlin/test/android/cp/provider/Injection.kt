package test.android.cp.provider

internal data class Injection(
    val contexts: Contexts,
    val loggers: Logger.Factory,
    val locals: Locals,
    val sessions: Sessions,
    val secrets: Secrets,
    val assets: Assets,
    val times: Times,
)
