package com.plugin.google_admob

import android.app.Activity
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import app.tauri.plugin.Plugin
import com.google.android.gms.ads.*
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import java.util.Date

// ============== Invoke Arguments ==============

@InvokeArg
class InitializeArgs {
    var testDeviceIds: List<String>? = null
    var tagForChildDirectedTreatment: Boolean? = null
    var tagForUnderAgeOfConsent: Boolean? = null
}

@InvokeArg
class BannerAdArgs {
    lateinit var adUnitId: String
    var position: String = "bottom"
    var adSize: String = "BANNER"
    var offset: Double = 0.0
}

@InvokeArg
class InterstitialAdArgs {
    lateinit var adUnitId: String
}

@InvokeArg
class RewardedAdArgs {
    lateinit var adUnitId: String
}

@InvokeArg
class RewardedInterstitialAdArgs {
    lateinit var adUnitId: String
}

@InvokeArg
class AppOpenAdArgs {
    lateinit var adUnitId: String
}

@InvokeArg
class PingArgs {
    var value: String? = null
}

// ============== Plugin ==============

@TauriPlugin
class AdmobPlugin(private val activity: Activity) : Plugin(activity) {
    
    companion object {
        private const val TAG = "AdmobPlugin"
    }
    
    // Ad instances
    private var bannerAdView: AdView? = null
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var appOpenAdLoadTime: Long = 0
    
    // State
    private var isInitialized = false
    private var isBannerShowing = false
    
    // ============== Initialize ==============
    
    @Command
    fun initialize(invoke: Invoke) {
        val args = invoke.parseArgs(InitializeArgs::class.java)
        
        activity.runOnUiThread {
            try {
                // Configure test devices
                val testDeviceIds = args.testDeviceIds ?: emptyList()
                val configBuilder = RequestConfiguration.Builder()
                
                if (testDeviceIds.isNotEmpty()) {
                    configBuilder.setTestDeviceIds(testDeviceIds)
                }
                
                args.tagForChildDirectedTreatment?.let { tagForChild ->
                    val tagValue = if (tagForChild) {
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                    } else {
                        RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
                    }
                    configBuilder.setTagForChildDirectedTreatment(tagValue)
                }
                
                args.tagForUnderAgeOfConsent?.let { tagForUnderAge ->
                    val tagValue = if (tagForUnderAge) {
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                    } else {
                        RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
                    }
                    configBuilder.setTagForUnderAgeOfConsent(tagValue)
                }
                
                MobileAds.setRequestConfiguration(configBuilder.build())
                
                // Initialize Mobile Ads SDK
                MobileAds.initialize(activity) { initializationStatus ->
                    Log.d(TAG, "AdMob SDK initialized: $initializationStatus")
                    isInitialized = true
                }
                
                val ret = JSObject()
                ret.put("initialized", true)
                invoke.resolve(ret)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob: ${e.message}")
                invoke.reject("Failed to initialize AdMob: ${e.message}")
            }
        }
    }
    
    // ============== Banner Ads ==============
    
    @Command
    fun show_banner(invoke: Invoke) {
        val args = invoke.parseArgs(BannerAdArgs::class.java)
        
        activity.runOnUiThread {
            try {
                // Remove existing banner if any
                hideBannerInternal()

                val size = getAdSize(args.adSize)

                // Create new AdView
                bannerAdView = AdView(activity).apply {
                    adUnitId = args.adUnitId
                    setAdSize(size)
                    
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "Banner ad loaded")
                            emitAdEvent("banner", "loaded", null, null)
                        }
                        
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "Banner ad failed to load: ${error.message}")
                            emitAdEvent("banner", "failedToLoad", error.message, null)
                        }
                        
                        override fun onAdOpened() {
                            Log.d(TAG, "Banner ad opened")
                            emitAdEvent("banner", "opened", null, null)
                        }
                        
                        override fun onAdClicked() {
                            Log.d(TAG, "Banner ad clicked")
                            emitAdEvent("banner", "clicked", null, null)
                        }
                        
                        override fun onAdClosed() {
                            Log.d(TAG, "Banner ad closed")
                            emitAdEvent("banner", "closed", null, null)
                        }
                        
                        override fun onAdImpression() {
                            Log.d(TAG, "Banner ad impression")
                            emitAdEvent("banner", "impression", null, null)
                        }
                    }
                }
                
                // Create container for banner
                val offsetPx = (args.offset * activity.resources.displayMetrics.density).toInt()
                val bannerContainer = FrameLayout(activity).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = when (args.position.lowercase()) {
                            "top" -> {
                                topMargin = offsetPx
                                Gravity.TOP or Gravity.CENTER_HORIZONTAL
                            }
                            else -> {
                                bottomMargin = offsetPx
                                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                            }
                        }
                    }
                    addView(bannerAdView)
                }
                
                // Add to activity's content view
                val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)
                rootView.addView(bannerContainer)
                
                // Load the ad
                bannerAdView?.loadAd(AdRequest.Builder().build())
                isBannerShowing = true
                
                val ret = JSObject()
                ret.put("shown", true)
                ret.put("height", size.height)
                invoke.resolve(ret)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show banner: ${e.message}")
                invoke.reject("Failed to show banner: ${e.message}")
            }
        }
    }
    
    @Command
    fun hide_banner(invoke: Invoke) {
        activity.runOnUiThread {
            try {
                hideBannerInternal()
                
                val ret = JSObject()
                ret.put("hidden", true)
                invoke.resolve(ret)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide banner: ${e.message}")
                invoke.reject("Failed to hide banner: ${e.message}")
            }
        }
    }
    
    private fun hideBannerInternal() {
        bannerAdView?.let { adView ->
            val parent = adView.parent as? ViewGroup
            parent?.let { parentView ->
                val grandParent = parentView.parent as? ViewGroup
                grandParent?.removeView(parentView)
            }
            adView.destroy()
            bannerAdView = null
        }
        isBannerShowing = false
    }
    
    private fun getAdSize(size: String): AdSize {
        return when (size.uppercase()) {
            "LARGE_BANNER" -> AdSize.LARGE_BANNER
            "MEDIUM_RECTANGLE" -> AdSize.MEDIUM_RECTANGLE
            "FULL_BANNER" -> AdSize.FULL_BANNER
            "LEADERBOARD" -> AdSize.LEADERBOARD
            "ADAPTIVE" -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                activity,
                AdSize.FULL_WIDTH
            )
            else -> AdSize.BANNER
        }
    }
    
    // ============== Interstitial Ads ==============
    
    @Command
    fun prepare_interstitial(invoke: Invoke) {
        val args = invoke.parseArgs(InterstitialAdArgs::class.java)
        
        activity.runOnUiThread {
            InterstitialAd.load(
                activity,
                args.adUnitId,
                AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "Interstitial ad loaded")
                        interstitialAd = ad
                        setupInterstitialCallbacks()
                        emitAdEvent("interstitial", "loaded", null, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", true)
                        invoke.resolve(ret)
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Interstitial ad failed to load: ${error.message}")
                        interstitialAd = null
                        emitAdEvent("interstitial", "failedToLoad", error.message, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", false)
                        invoke.resolve(ret)
                    }
                }
            )
        }
    }
    
    private fun setupInterstitialCallbacks() {
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial ad dismissed")
                interstitialAd = null
                emitAdEvent("interstitial", "closed", null, null)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial ad failed to show: ${error.message}")
                interstitialAd = null
                emitAdEvent("interstitial", "failedToShow", error.message, null)
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial ad showed")
                emitAdEvent("interstitial", "opened", null, null)
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "Interstitial ad clicked")
                emitAdEvent("interstitial", "clicked", null, null)
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "Interstitial ad impression")
                emitAdEvent("interstitial", "impression", null, null)
            }
        }
    }
    
    @Command
    fun show_interstitial(invoke: Invoke) {
        activity.runOnUiThread {
            val ad = interstitialAd
            if (ad != null) {
                ad.show(activity)
                
                val ret = JSObject()
                ret.put("shown", true)
                invoke.resolve(ret)
            } else {
                Log.e(TAG, "Interstitial ad not ready")
                invoke.reject("Interstitial ad not ready. Call prepareInterstitial first.")
            }
        }
    }
    
    // ============== Rewarded Ads ==============
    
    @Command
    fun prepare_rewarded(invoke: Invoke) {
        val args = invoke.parseArgs(RewardedAdArgs::class.java)
        
        activity.runOnUiThread {
            RewardedAd.load(
                activity,
                args.adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        Log.d(TAG, "Rewarded ad loaded")
                        rewardedAd = ad
                        setupRewardedCallbacks()
                        emitAdEvent("rewarded", "loaded", null, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", true)
                        invoke.resolve(ret)
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                        rewardedAd = null
                        emitAdEvent("rewarded", "failedToLoad", error.message, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", false)
                        invoke.resolve(ret)
                    }
                }
            )
        }
    }
    
    private var pendingRewardedInvoke: Invoke? = null
    private var earnedReward: JSObject? = null
    
    private fun setupRewardedCallbacks() {
        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed")
                rewardedAd = null
                emitAdEvent("rewarded", "closed", null, null)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded ad failed to show: ${error.message}")
                rewardedAd = null
                emitAdEvent("rewarded", "failedToShow", error.message, null)
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad showed")
                emitAdEvent("rewarded", "opened", null, null)
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "Rewarded ad clicked")
                emitAdEvent("rewarded", "clicked", null, null)
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "Rewarded ad impression")
                emitAdEvent("rewarded", "impression", null, null)
            }
        }
    }
    
    @Command
    fun show_rewarded(invoke: Invoke) {
        activity.runOnUiThread {
            val ad = rewardedAd
            if (ad != null) {
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    
                    val rewardObj = JSObject()
                    rewardObj.put("rewardType", rewardItem.type)
                    rewardObj.put("amount", rewardItem.amount)
                    emitAdEvent("rewarded", "reward", null, rewardObj)
                    
                    val ret = JSObject()
                    ret.put("shown", true)
                    val reward = JSObject()
                    reward.put("rewardType", rewardItem.type)
                    reward.put("amount", rewardItem.amount)
                    ret.put("reward", reward)
                    invoke.resolve(ret)
                }
            } else {
                Log.e(TAG, "Rewarded ad not ready")
                invoke.reject("Rewarded ad not ready. Call prepareRewarded first.")
            }
        }
    }
    
    // ============== Rewarded Interstitial Ads ==============
    
    @Command
    fun prepare_rewarded_interstitial(invoke: Invoke) {
        val args = invoke.parseArgs(RewardedInterstitialAdArgs::class.java)
        
        activity.runOnUiThread {
            RewardedInterstitialAd.load(
                activity,
                args.adUnitId,
                AdRequest.Builder().build(),
                object : RewardedInterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedInterstitialAd) {
                        Log.d(TAG, "Rewarded interstitial ad loaded")
                        rewardedInterstitialAd = ad
                        setupRewardedInterstitialCallbacks()
                        emitAdEvent("rewardedInterstitial", "loaded", null, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", true)
                        invoke.resolve(ret)
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "Rewarded interstitial ad failed to load: ${error.message}")
                        rewardedInterstitialAd = null
                        emitAdEvent("rewardedInterstitial", "failedToLoad", error.message, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", false)
                        invoke.resolve(ret)
                    }
                }
            )
        }
    }
    
    private fun setupRewardedInterstitialCallbacks() {
        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded interstitial ad dismissed")
                rewardedInterstitialAd = null
                emitAdEvent("rewardedInterstitial", "closed", null, null)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded interstitial ad failed to show: ${error.message}")
                rewardedInterstitialAd = null
                emitAdEvent("rewardedInterstitial", "failedToShow", error.message, null)
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded interstitial ad showed")
                emitAdEvent("rewardedInterstitial", "opened", null, null)
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "Rewarded interstitial ad clicked")
                emitAdEvent("rewardedInterstitial", "clicked", null, null)
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "Rewarded interstitial ad impression")
                emitAdEvent("rewardedInterstitial", "impression", null, null)
            }
        }
    }
    
    @Command
    fun show_rewarded_interstitial(invoke: Invoke) {
        activity.runOnUiThread {
            val ad = rewardedInterstitialAd
            if (ad != null) {
                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    
                    val rewardObj = JSObject()
                    rewardObj.put("rewardType", rewardItem.type)
                    rewardObj.put("amount", rewardItem.amount)
                    emitAdEvent("rewardedInterstitial", "reward", null, rewardObj)
                    
                    val ret = JSObject()
                    ret.put("shown", true)
                    val reward = JSObject()
                    reward.put("rewardType", rewardItem.type)
                    reward.put("amount", rewardItem.amount)
                    ret.put("reward", reward)
                    invoke.resolve(ret)
                }
            } else {
                Log.e(TAG, "Rewarded interstitial ad not ready")
                invoke.reject("Rewarded interstitial ad not ready. Call prepareRewardedInterstitial first.")
            }
        }
    }
    
    // ============== App Open Ads ==============
    
    @Command
    fun prepare_app_open(invoke: Invoke) {
        val args = invoke.parseArgs(AppOpenAdArgs::class.java)
        
        activity.runOnUiThread {
            AppOpenAd.load(
                activity,
                args.adUnitId,
                AdRequest.Builder().build(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "App open ad loaded")
                        appOpenAd = ad
                        appOpenAdLoadTime = Date().time
                        setupAppOpenCallbacks()
                        emitAdEvent("appOpen", "loaded", null, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", true)
                        invoke.resolve(ret)
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e(TAG, "App open ad failed to load: ${error.message}")
                        appOpenAd = null
                        emitAdEvent("appOpen", "failedToLoad", error.message, null)
                        
                        val ret = JSObject()
                        ret.put("loaded", false)
                        invoke.resolve(ret)
                    }
                }
            )
        }
    }
    
    private fun setupAppOpenCallbacks() {
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "App open ad dismissed")
                appOpenAd = null
                emitAdEvent("appOpen", "closed", null, null)
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "App open ad failed to show: ${error.message}")
                appOpenAd = null
                emitAdEvent("appOpen", "failedToShow", error.message, null)
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "App open ad showed")
                emitAdEvent("appOpen", "opened", null, null)
            }
            
            override fun onAdClicked() {
                Log.d(TAG, "App open ad clicked")
                emitAdEvent("appOpen", "clicked", null, null)
            }
            
            override fun onAdImpression() {
                Log.d(TAG, "App open ad impression")
                emitAdEvent("appOpen", "impression", null, null)
            }
        }
    }
    
    @Command
    fun show_app_open(invoke: Invoke) {
        activity.runOnUiThread {
            val ad = appOpenAd
            if (ad != null && isAppOpenAdValid()) {
                ad.show(activity)
                
                val ret = JSObject()
                ret.put("shown", true)
                invoke.resolve(ret)
            } else {
                Log.e(TAG, "App open ad not ready or expired")
                invoke.reject("App open ad not ready or expired. Call prepareAppOpen first.")
            }
        }
    }
    
    private fun isAppOpenAdValid(): Boolean {
        // App open ads are valid for 4 hours
        val dateDifference = Date().time - appOpenAdLoadTime
        val numMilliSecondsPerHour = 3600000L
        return dateDifference < (numMilliSecondsPerHour * 4)
    }
    
    // ============== Helper Methods ==============
    
    private fun emitAdEvent(adType: String, event: String, error: String?, reward: JSObject?) {
        val eventData = JSObject()
        eventData.put("adType", adType)
        eventData.put("event", event)
        if (error != null) {
            eventData.put("error", error)
        }
        if (reward != null) {
            eventData.put("reward", reward)
        }
        trigger("adEvent", eventData)
    }
    
    // ============== Legacy Ping Command ==============
    
    @Command
    fun ping(invoke: Invoke) {
        val args = invoke.parseArgs(PingArgs::class.java)
        
        val ret = JSObject()
        ret.put("value", args.value ?: "pong")
        invoke.resolve(ret)
    }
    
    // ============== Lifecycle ==============
    
    override fun onDestroy() {
        super.onDestroy()
        hideBannerInternal()
        interstitialAd = null
        rewardedAd = null
        rewardedInterstitialAd = null
        appOpenAd = null
    }
}
