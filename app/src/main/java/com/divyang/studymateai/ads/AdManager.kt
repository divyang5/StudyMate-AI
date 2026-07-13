package com.divyang.studymateai.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
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
import com.divyang.studymateai.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AdManager"

// Interstitials (not rewarded ads — those are user-initiated) share one cap so
// two full-screen ads can never stack up within a few minutes of each other.
private const val MIN_FULL_SCREEN_AD_INTERVAL_MS = 3 * 60_000L

/**
 * App-wide singleton: one preloaded interstitial and one preloaded rewarded ad
 * shared by every screen. Screen-local instances used to show nothing because
 * each fresh instance had no ad loaded yet.
 */
@Singleton
class AdManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var bannerAdView: AdView? = null

    // Elapsed-realtime of the last full-screen ad (interstitial or rewarded).
    private var lastFullScreenAdAt = 0L

    private val interstitialADUnit: String = BuildConfig.INTERSTITIAL_AD_UNIT
    private val bannerADUnit: String = BuildConfig.BANNER_AD_UNIT
    private val rewardedADUnit: String = BuildConfig.REWARDED_AD_UNIT

    fun loadInterstitialAd() {
        if (interstitialAd != null) return
        InterstitialAd.load(context, interstitialADUnit, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.code} ${error.message}")
                }
            })
    }

    fun loadRewardedAd() {
        if (rewardedAd != null) return
        RewardedAd.load(context, rewardedADUnit, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded ad failed to load: ${error.code} ${error.message}")
                }
            })
    }

    /**
     * Shows an interstitial if one is loaded AND the frequency cap allows it;
     * otherwise calls [onDone] immediately. The user's flow always continues.
     */
    fun showInterstitialAd(activity: Activity?, onDone: () -> Unit = {}) {
        val ad = interstitialAd
        val capped = SystemClock.elapsedRealtime() - lastFullScreenAdAt < MIN_FULL_SCREEN_AD_INTERVAL_MS
        if (activity == null || ad == null || capped) {
            if (ad == null) loadInterstitialAd()
            if (capped) Log.d(TAG, "Interstitial skipped by frequency cap")
            onDone()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd()
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${adError.code} ${adError.message}")
                interstitialAd = null
                loadInterstitialAd()
                onDone()
            }
        }
        lastFullScreenAdAt = SystemClock.elapsedRealtime()
        interstitialAd = null
        ad.show(activity)
    }

    /**
     * Rewarded gate for user-initiated actions (e.g. regenerate). Calls
     * [onProceed] with true when the user earned the reward — or when no ad is
     * available, so the feature is never blocked by missing ad fill. False
     * only when the user closed the ad early.
     */
    fun showRewardedAd(activity: Activity?, onProceed: (Boolean) -> Unit) {
        val ad = rewardedAd
        if (activity == null || ad == null) {
            loadRewardedAd()
            onProceed(true)
            return
        }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd()
                onProceed(earned)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${adError.code} ${adError.message}")
                rewardedAd = null
                loadRewardedAd()
                onProceed(true)
            }
        }
        lastFullScreenAdAt = SystemClock.elapsedRealtime()
        rewardedAd = null
        ad.show(activity) { earned = true }
    }

    // Banner Ad Methods
    @Composable
    fun BannerAd(
        modifier: Modifier = Modifier
    ) {
        val context = LocalContext.current
        var loadError by remember { mutableStateOf(false) }
        var adView by remember { mutableStateOf<AdView?>(null) }

        DisposableEffect(Unit) {
            val newAdView = AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = bannerADUnit
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        loadError = false
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e(TAG, "Banner failed to load: ${adError.code} ${adError.message}")
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
        rewardedAd = null
        bannerAdView?.destroy()
        bannerAdView = null
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdManagerEntryPoint {
    fun adManager(): AdManager
}

/** Returns the app-wide [AdManager] singleton from composables. */
@Composable
fun rememberAdManager(): AdManager {
    val appContext = LocalContext.current.applicationContext
    return remember {
        EntryPointAccessors.fromApplication(appContext, AdManagerEntryPoint::class.java).adManager()
    }
}

/** Unwraps the Activity backing a composable's context (needed to show full-screen ads). */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
