package com.app.reelssdk.ui.ads

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.admanager.AdManagerAdRequest
import com.google.android.gms.ads.admanager.AdManagerAdView

@Composable
fun LBandBanner(
    adUnitId: String,
    adSize: AdSize,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdManagerAdView(context).apply {
                setAdSizes(adSize)
                this.adUnitId = adUnitId

                val request = AdManagerAdRequest.Builder().build()

                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("GAM", "Ad Loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("GAM", "Ad Failed: ${error.message}")
                    }
                }

                loadAd(request)
            }
        }
    )
}

