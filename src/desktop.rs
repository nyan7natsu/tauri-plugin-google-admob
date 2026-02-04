use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
    app: &AppHandle<R>,
    _api: PluginApi<R, C>,
) -> crate::Result<GoogleAdmob<R>> {
    Ok(GoogleAdmob(app.clone()))
}

/// Access to the google-admob APIs.
pub struct GoogleAdmob<R: Runtime>(AppHandle<R>); 

impl<R: Runtime> GoogleAdmob<R> {
    // ============== Initialize ==============

    pub fn initialize(&self, _payload: InitializeRequest) -> crate::Result<InitializeResponse> {
        //
        // Desktop doesn't support AdMob, return mock response
        Ok(InitializeResponse { initialized: false })
    }

    // ============== Banner Ads ==============

    pub fn show_banner(&self, _payload: BannerAdOptions) -> crate::Result<ShowBannerResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(ShowBannerResponse { shown: false })
    }

    pub fn hide_banner(&self) -> crate::Result<HideBannerResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(HideBannerResponse { hidden: false })
    }

    // ============== Interstitial Ads ==============

    pub fn prepare_interstitial(
        &self,
        _payload: InterstitialAdOptions,
    ) -> crate::Result<PrepareInterstitialResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(PrepareInterstitialResponse { loaded: false })
    }

    pub fn show_interstitial(&self) -> crate::Result<ShowInterstitialResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(ShowInterstitialResponse { shown: false })
    }

    // ============== Rewarded Ads ==============

    pub fn prepare_rewarded(
        &self,
        _payload: RewardedAdOptions,
    ) -> crate::Result<PrepareRewardedResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(PrepareRewardedResponse { loaded: false })
    }

    pub fn show_rewarded(&self) -> crate::Result<ShowRewardedResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(ShowRewardedResponse {
            shown: false,
            reward: None,
        })
    }

    // ============== Rewarded Interstitial Ads ==============

    pub fn prepare_rewarded_interstitial(
        &self,
        _payload: RewardedInterstitialAdOptions,
    ) -> crate::Result<PrepareRewardedInterstitialResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(PrepareRewardedInterstitialResponse { loaded: false })
    }

    pub fn show_rewarded_interstitial(&self) -> crate::Result<ShowRewardedInterstitialResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(ShowRewardedInterstitialResponse {
            shown: false,
            reward: None,
        })
    }

    // ============== App Open Ads ==============

    pub fn prepare_app_open(
        &self,
        _payload: AppOpenAdOptions,
    ) -> crate::Result<PrepareAppOpenResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(PrepareAppOpenResponse { loaded: false })
    }

    pub fn show_app_open(&self) -> crate::Result<ShowAppOpenResponse> {
        // Desktop doesn't support AdMob, return mock response
        Ok(ShowAppOpenResponse { shown: false })
    }

    // ============== Legacy (keeping for compatibility) ==============

    pub fn ping(&self, payload: PingRequest) -> crate::Result<PingResponse> {
        Ok(PingResponse {
            value: payload.value,
        })
    }
}
