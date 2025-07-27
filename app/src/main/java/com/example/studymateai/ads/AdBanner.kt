package com.example.studymateai.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = "ca-app-pub-1428496463629890/7092011553"
) {
    val context = LocalContext.current
    val adView = remember { AdView(context) }
    var loadError by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        adView.setAdSize( AdSize.BANNER)
        adView.adUnitId = adUnitId

        adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                loadError = true
            }
        }

        adView.loadAd(AdRequest.Builder().build())

        onDispose {
            adView.destroy()
        }
    }

    if (!loadError) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    addView(adView)
                }
            },
            modifier = modifier.fillMaxWidth()
        )
    } else {
        Box(modifier = modifier.height(50.dp).fillMaxWidth())
    }
}