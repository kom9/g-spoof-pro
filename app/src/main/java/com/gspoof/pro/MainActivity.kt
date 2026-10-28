package com.gspoof.pro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.gspoof.pro.ui.GSpoofTheme
import com.gspoof.pro.ui.MapScreen
import com.gspoof.pro.ui.SplashScreen
import org.osmdroid.config.Configuration

class MainActivity : ComponentActivity() {

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().osmdroidBasePath =
            java.io.File(cacheDir, "osmdroid").apply { mkdirs() }
        Configuration.getInstance().osmdroidTileCache =
            java.io.File(cacheDir, "osmdroid/tiles").apply { mkdirs() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        setContent {
            GSpoofTheme {
                var ready by remember { mutableStateOf(false) }
                Crossfade(
                    targetState = ready,
                    animationSpec = tween(450),
                    label = "splash-gate"
                ) { showMap ->
                    if (showMap) MapScreen()
                    else SplashScreen(onFinish = { ready = true })
                }
            }
        }
    }
}
