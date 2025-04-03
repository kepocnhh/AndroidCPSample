package test.android.cp

import android.content.Context
import android.database.Cursor
import android.net.Uri
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
import test.android.cp.entity.Person
import test.android.cp.provider.Logger
import test.android.cp.util.map
import test.android.cp.util.query
import test.android.cp.util.showToast

private fun onClick(context: Context, logger: Logger) {
    val uri = Uri.Builder()
        .scheme("content")
        .authority("test.android.cp.foo.authority.provider")
        .appendPath("persons")
        .build()
    val persons = context.contentResolver.query(uri = uri, projection = arrayOf("firstName", "lastName")).use { cursor: Cursor? ->
        if (cursor == null) error("No cursor!")
        cursor.map {
            Person(
                firstName = it.getString(it.getColumnIndexOrThrow("firstName")),
                lastName = it.getString(it.getColumnIndexOrThrow("lastName")),
            )
        }
    }
    logger.debug("persons: $persons")
    context.showToast("$persons")
}

@Composable
internal fun MainScreen() {
    val context = LocalContext.current
    val logger = remember { App.loggers.create("[Main|Bar]") }
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
                        onClick(context = context, logger = logger)
                    }
                    .wrapContentSize(),
                text = "click",
            )
        }
    }
}
