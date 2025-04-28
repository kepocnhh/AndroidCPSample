package test.android.cp.module.enter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView

internal class EnterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = ComposeView(this)
        setContentView(view)
        view.setContent {
            EnterScreen(
                onEnter = { bytes ->
                    val intent = Intent()
                    intent.putExtra("bytes", bytes)
                    setResult(1, intent)
                    finish()
                },
                onExit = {
                    finish()
                },
            )
        }
    }
}
