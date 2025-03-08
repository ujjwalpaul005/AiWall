package com.ujjwal.aiwall

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.ujjwal.aiwall.ui.theme.AiWallTheme


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //False - allows to draw the content "edge-to-edge"
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AiWallTheme {
                GetAndShowWallpaper()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AiWallTheme {
        GetAndShowWallpaper()
    }
}