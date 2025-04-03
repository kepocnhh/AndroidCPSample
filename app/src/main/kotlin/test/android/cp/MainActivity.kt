package test.android.cp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

internal class MainActivity : ComponentActivity() {
    private fun onClick() {
        // todo
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = ComposeView(this)
        setContentView(view)
        view.setContent {
            Box(modifier = Modifier.fillMaxSize()) {
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
                                onClick()
                            }
                            .wrapContentSize(),
                        text = "click",
                    )
                }
            }
        }
    }
}
