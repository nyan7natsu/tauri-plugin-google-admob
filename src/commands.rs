use tauri::{command, AppHandle, Runtime};

use crate::models::*;
use crate::GoogleAdmobExt;
use crate::Result;

// ============== Initialize ==============

#[command]
pub(crate) async fn initialize<R: Runtime>(
    app: AppHandle<R>,
    payload: InitializeRequest,
) -> Result<InitializeResponse> {
    app.google_admob().initialize(payload)
}

// ============== Banner Ads ==============

#[command]
pub(crate) async fn show_banner<R: Runtime>(
    app: AppHandle<R>,
    payload: BannerAdOptions,
) -> Result<ShowBannerResponse> {
    app.google_admob().show_banner(payload)
}

#[command]
pub(crate) async fn hide_banner<R: Runtime>(app: AppHandle<R>) -> Result<HideBannerResponse> {
    app.google_admob().hide_banner()
}

// ============== Interstitial Ads ==============

#[command]
pub(crate) async fn prepare_interstitial<R: Runtime>(
    app: AppHandle<R>,
    payload: InterstitialAdOptions,
) -> Result<PrepareInterstitialResponse> {
    app.google_admob().prepare_interstitial(payload)
}

#[command]
pub(crate) async fn show_interstitial<R: Runtime>(
    app: AppHandle<R>,
) -> Result<ShowInterstitialResponse> {
    app.google_admob().show_interstitial()
}

// ============== Rewarded Ads ==============

#[command]
pub(crate) async fn prepare_rewarded<R: Runtime>(
    app: AppHandle<R>,
    payload: RewardedAdOptions,
) -> Result<PrepareRewardedResponse> {
    app.google_admob().prepare_rewarded(payload)
}

#[command]
pub(crate) async fn show_rewarded<R: Runtime>(app: AppHandle<R>) -> Result<ShowRewardedResponse> {
    app.google_admob().show_rewarded()
}

// ============== Rewarded Interstitial Ads ==============

#[command]
pub(crate) async fn prepare_rewarded_interstitial<R: Runtime>(
    app: AppHandle<R>,
    payload: RewardedInterstitialAdOptions,
) -> Result<PrepareRewardedInterstitialResponse> {
    app.google_admob().prepare_rewarded_interstitial(payload)
}

#[command]
pub(crate) async fn show_rewarded_interstitial<R: Runtime>(
    app: AppHandle<R>,
) -> Result<ShowRewardedInterstitialResponse> {
    app.google_admob().show_rewarded_interstitial()
}

// ============== App Open Ads ==============

#[command]
pub(crate) async fn prepare_app_open<R: Runtime>(
    app: AppHandle<R>,
    payload: AppOpenAdOptions,
) -> Result<PrepareAppOpenResponse> {
    app.google_admob().prepare_app_open(payload)
}

#[command]
pub(crate) async fn show_app_open<R: Runtime>(app: AppHandle<R>) -> Result<ShowAppOpenResponse> {
    app.google_admob().show_app_open()
}

// ============== Legacy (keeping for compatibility) ==============

#[command]
pub(crate) async fn ping<R: Runtime>(
    app: AppHandle<R>,
    payload: PingRequest,
) -> Result<PingResponse> {
    app.google_admob().ping(payload)
}
