package com.example.studymateai.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class AdManager(private val context: Context) {
    // Interstitial Ad Properties
    private var interstitialAd: InterstitialAd? = null

    // Banner Ad Properties
    private var bannerAdView: AdView? = null


    fun loadInterstitialAd(adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitialAd = ad
                Log.d("AdManager", "Ad loaded successfully")
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitialAd = null
                Log.e("AdManager", "Failed to load ad: ${error.message}")
            }
        })
    }

    fun isAdLoaded(): Boolean {
        return interstitialAd != null
    }

    fun showInterstitialAd(onAdDismissed: () -> Unit, onAdFailed: () -> Unit) {
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd("ca-app-pub-1428496463629890/8616846219")
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    loadInterstitialAd("ca-app-pub-1428496463629890/8616846219")
                    onAdFailed()
                }
            }
            ad.show(context as Activity)
        } else {
            Log.d("AdManager", "Ad not loaded")
            onAdFailed()
        }
    }
    // Banner Ad Methods
    @Composable
    fun BannerAd(
        modifier: Modifier = Modifier,
        adUnitId: String = "ca-app-pub-1428496463629890/7092011553"
    ) {
        val context = LocalContext.current
        var loadError by remember { mutableStateOf(false) }
        var adView by remember { mutableStateOf<AdView?>(null) }

        DisposableEffect(Unit) {
            val newAdView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        loadError = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
            adView = newAdView
            bannerAdView = newAdView

            onDispose {
                newAdView.destroy()
                bannerAdView = null
            }
        }

        if (!loadError && adView != null) {
            AndroidView(
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        adView?.let { addView(it) }
                    }
                },
                modifier = modifier.fillMaxWidth()
            )
        } else {
            Box(modifier = modifier.height(50.dp).fillMaxWidth())
        }
    }

    // Clean up all ads
    fun destroyAllAds() {
        interstitialAd = null
        bannerAdView?.destroy()
        bannerAdView = null
    }
}