package test.android.cp.module.enter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import test.android.cp.BuildConfig

internal class EnterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = ComposeView(this)
        setContentView(view)
        setResult(2, Intent().also { it.putExtra("answer", BuildConfig.APPLICATION_ID) })
        view.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
                val intent = intent
                if (intent != null) {
                    BasicText(
                        text = "issuer: ${intent.getStringExtra("issuer")}",
                    )
                }
            }
        }
    }
}
