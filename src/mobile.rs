use serde::de::DeserializeOwned;
use tauri::{
    plugin::{PluginApi, PluginHandle},
    AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_google_admob);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
    _app: &AppHandle<R>,
    api: PluginApi<R, C>,
) -> crate::Result<GoogleAdmob<R>> {
    #[cfg(target_os = "android")]
    let handle = api.register_android_plugin("com.plugin.google_admob", "AdmobPlugin")?;
    #[cfg(target_os = "ios")]
    let handle = api.register_ios_plugin(init_plugin_google_admob)?;
    Ok(GoogleAdmob(handle))
}

/// Access to the google-admob APIs.
pub struct GoogleAdmob<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> GoogleAdmob<R> {
    // ============== Initialize ==============

    pub fn initialize(&self, payload: InitializeRequest) -> crate::Result<InitializeResponse> {
        self.0
            .run_mobile_plugin("initialize", payload)
            .map_err(Into::into)
    }

    // ============== Banner Ads ==============

    pub fn show_banner(&self, payload: BannerAdOptions) -> crate::Result<ShowBannerResponse> {
        self.0
            .run_mobile_plugin("show_banner", payload)
            .map_err(Into::into)
    }

    pub fn hide_banner(&self) -> crate::Result<HideBannerResponse> {
        self.0
            .run_mobile_plugin::<HideBannerResponse>("hide_banner", ())
            .map_err(Into::into)
    }

    // ============== Interstitial Ads ==============

    pub fn prepare_interstitial(
        &self,
        payload: InterstitialAdOptions,
    ) -> crate::Result<PrepareInterstitialResponse> {
        self.0
            .run_mobile_plugin("prepare_interstitial", payload)
            .map_err(Into::into)
    }

    pub fn show_interstitial(&self) -> crate::Result<ShowInterstitialResponse> {
        self.0
            .run_mobile_plugin::<ShowInterstitialResponse>("show_interstitial", ())
            .map_err(Into::into)
    }

    // ============== Rewarded Ads ==============

    pub fn prepare_rewarded(
        &self,
        payload: RewardedAdOptions,
    ) -> crate::Result<PrepareRewardedResponse> {
        self.0
            .run_mobile_plugin("prepare_rewarded", payload)
            .map_err(Into::into)
    }

    pub fn show_rewarded(&self) -> crate::Result<ShowRewardedResponse> {
        self.0
            .run_mobile_plugin::<ShowRewardedResponse>("show_rewarded", ())
            .map_err(Into::into)
    }

    // ============== Rewarded Interstitial Ads ==============

    pub fn prepare_rewarded_interstitial(
        &self,
        payload: RewardedInterstitialAdOptions,
    ) -> crate::Result<PrepareRewardedInterstitialResponse> {
        self.0
            .run_mobile_plugin("prepare_rewarded_interstitial", payload)
            .map_err(Into::into)
    }

    pub fn show_rewarded_interstitial(&self) -> crate::Result<ShowRewardedInterstitialResponse> {
        self.0
            .run_mobile_plugin::<ShowRewardedInterstitialResponse>("show_rewarded_interstitial", ())
            .map_err(Into::into)
    }

    // ============== App Open Ads ==============

    pub fn prepare_app_open(
        &self,
        payload: AppOpenAdOptions,
    ) -> crate::Result<PrepareAppOpenResponse> {
        self.0
            .run_mobile_plugin("prepare_app_open", payload)
            .map_err(Into::into)
    }

    pub fn show_app_open(&self) -> crate::Result<ShowAppOpenResponse> {
        self.0
            .run_mobile_plugin::<ShowAppOpenResponse>("show_app_open", ())
            .map_err(Into::into)
    }

    // ============== Legacy (keeping for compatibility) ==============

    pub fn ping(&self, payload: PingRequest) -> crate::Result<PingResponse> {
        self.0
            .run_mobile_plugin("ping", payload)
            .map_err(Into::into)
    }
}
