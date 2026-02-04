import { invoke } from '@tauri-apps/api/core'

// ============== Types ==============

export interface InitializeOptions {
  testDeviceIds?: string[]
  tagForChildDirectedTreatment?: boolean
  tagForUnderAgeOfConsent?: boolean
}

export interface InitializeResult {
  initialized: boolean
}

export type BannerPosition = 'top' | 'bottom'

export type BannerSize =
  | 'BANNER'
  | 'LARGE_BANNER'
  | 'MEDIUM_RECTANGLE'
  | 'FULL_BANNER'
  | 'LEADERBOARD'
  | 'ADAPTIVE'

export interface BannerAdOptions {
  adUnitId: string
  position?: BannerPosition
  adSize?: BannerSize
}

export interface ShowBannerResult {
  shown: boolean
}

export interface HideBannerResult {
  hidden: boolean
}

export interface InterstitialAdOptions {
  adUnitId: string
}

export interface PrepareInterstitialResult {
  loaded: boolean
}

export interface ShowInterstitialResult {
  shown: boolean
}

export interface RewardedAdOptions {
  adUnitId: string
}

export interface PrepareRewardedResult {
  loaded: boolean
}

export interface AdReward {
  rewardType: string
  amount: number
}

export interface ShowRewardedResult {
  shown: boolean
  reward?: AdReward
}

export interface RewardedInterstitialAdOptions {
  adUnitId: string
}

export interface PrepareRewardedInterstitialResult {
  loaded: boolean
}

export interface ShowRewardedInterstitialResult {
  shown: boolean
  reward?: AdReward
}

export interface AppOpenAdOptions {
  adUnitId: string
}

export interface PrepareAppOpenResult {
  loaded: boolean
}

export interface ShowAppOpenResult {
  shown: boolean
}

export type AdType =
  | 'banner'
  | 'interstitial'
  | 'rewarded'
  | 'rewardedInterstitial'
  | 'appOpen'

export type AdEventType =
  | 'loaded'
  | 'failedToLoad'
  | 'opened'
  | 'closed'
  | 'clicked'
  | 'impression'
  | 'failedToShow'
  | 'reward'

export interface AdEvent {
  adType: AdType
  event: AdEventType
  error?: string
  reward?: AdReward
}

// ============== Test Ad Unit IDs ==============

export const TestAdUnitIds = {
  BANNER: 'ca-app-pub-3940256099942544/9214589741',
  INTERSTITIAL: 'ca-app-pub-3940256099942544/1033173712',
  REWARDED: 'ca-app-pub-3940256099942544/5224354917',
  REWARDED_INTERSTITIAL: 'ca-app-pub-3940256099942544/5354046379',
  APP_OPEN: 'ca-app-pub-3940256099942544/9257395921',
} as const

// ============== Initialize ==============

/**
 * Initialize the AdMob SDK
 * @param options - Initialization options including test device IDs
 * @returns Promise resolving to initialization result
 */
export async function initialize(
  options: InitializeOptions = {}
): Promise<InitializeResult> {
  return await invoke<InitializeResult>('plugin:google-admob|initialize', {
    payload: options,
  })
}

// ============== Banner Ads ==============

/**
 * Show a banner ad
 * @param options - Banner ad options including ad unit ID, position, and size
 * @returns Promise resolving when banner is shown
 */
export async function showBanner(
  options: BannerAdOptions
): Promise<ShowBannerResult> {
  return await invoke<ShowBannerResult>('plugin:google-admob|show_banner', {
    payload: {
      adUnitId: options.adUnitId,
      position: options.position ?? 'bottom',
      adSize: options.adSize ?? 'BANNER',
    },
  })
}

/**
 * Hide the currently showing banner ad
 * @returns Promise resolving when banner is hidden
 */
export async function hideBanner(): Promise<HideBannerResult> {
  return await invoke<HideBannerResult>('plugin:google-admob|hide_banner')
}

// ============== Interstitial Ads ==============

/**
 * Prepare (load) an interstitial ad
 * @param options - Interstitial ad options including ad unit ID
 * @returns Promise resolving when ad is loaded
 */
export async function prepareInterstitial(
  options: InterstitialAdOptions
): Promise<PrepareInterstitialResult> {
  return await invoke<PrepareInterstitialResult>(
    'plugin:google-admob|prepare_interstitial',
    {
      payload: options,
    }
  )
}

/**
 * Show a prepared interstitial ad
 * @returns Promise resolving when ad is shown
 */
export async function showInterstitial(): Promise<ShowInterstitialResult> {
  return await invoke<ShowInterstitialResult>(
    'plugin:google-admob|show_interstitial'
  )
}

// ============== Rewarded Ads ==============

/**
 * Prepare (load) a rewarded ad
 * @param options - Rewarded ad options including ad unit ID
 * @returns Promise resolving when ad is loaded
 */
export async function prepareRewarded(
  options: RewardedAdOptions
): Promise<PrepareRewardedResult> {
  return await invoke<PrepareRewardedResult>(
    'plugin:google-admob|prepare_rewarded',
    {
      payload: options,
    }
  )
}

/**
 * Show a prepared rewarded ad
 * @returns Promise resolving with reward information when ad is completed
 */
export async function showRewarded(): Promise<ShowRewardedResult> {
  return await invoke<ShowRewardedResult>('plugin:google-admob|show_rewarded')
}

// ============== Rewarded Interstitial Ads ==============

/**
 * Prepare (load) a rewarded interstitial ad
 * @param options - Rewarded interstitial ad options including ad unit ID
 * @returns Promise resolving when ad is loaded
 */
export async function prepareRewardedInterstitial(
  options: RewardedInterstitialAdOptions
): Promise<PrepareRewardedInterstitialResult> {
  return await invoke<PrepareRewardedInterstitialResult>(
    'plugin:google-admob|prepare_rewarded_interstitial',
    {
      payload: options,
    }
  )
}

/**
 * Show a prepared rewarded interstitial ad
 * @returns Promise resolving with reward information when ad is completed
 */
export async function showRewardedInterstitial(): Promise<ShowRewardedInterstitialResult> {
  return await invoke<ShowRewardedInterstitialResult>(
    'plugin:google-admob|show_rewarded_interstitial'
  )
}

// ============== App Open Ads ==============

/**
 * Prepare (load) an app open ad
 * @param options - App open ad options including ad unit ID
 * @returns Promise resolving when ad is loaded
 */
export async function prepareAppOpen(
  options: AppOpenAdOptions
): Promise<PrepareAppOpenResult> {
  return await invoke<PrepareAppOpenResult>(
    'plugin:google-admob|prepare_app_open',
    {
      payload: options,
    }
  )
}

/**
 * Show a prepared app open ad
 * @returns Promise resolving when ad is shown
 */
export async function showAppOpen(): Promise<ShowAppOpenResult> {
  return await invoke<ShowAppOpenResult>('plugin:google-admob|show_app_open')
}

// ============== Legacy (keeping for compatibility) ==============

export async function ping(value: string): Promise<string | null> {
  return await invoke<{ value?: string }>('plugin:google-admob|ping', {
    payload: {
      value,
    },
  }).then((r) => (r.value ? r.value : null))
}
