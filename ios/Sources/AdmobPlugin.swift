import GoogleMobileAds
import Tauri
import UIKit
import WebKit

// ============== Invoke Arguments ==============

struct InitializeArgs: Decodable {
  var testDeviceIds: [String]?
  var tagForChildDirectedTreatment: Bool?
  var tagForUnderAgeOfConsent: Bool?
}

struct BannerAdArgs: Decodable {
  let adUnitId: String
  var position: String?
  var adSize: String?
  var offset: Double?
}

struct AdUnitArgs: Decodable {
  let adUnitId: String
}

struct PingArgs: Decodable {
  var value: String?
}

// ============== Responses ==============

struct InitializeResponse: Encodable {
  let initialized: Bool
}

struct ShowBannerResponse: Encodable {
  let shown: Bool
  let height: Double
}

struct HideBannerResponse: Encodable {
  let hidden: Bool
}

struct LoadedResponse: Encodable {
  let loaded: Bool
}

struct ShownResponse: Encodable {
  let shown: Bool
}

struct RewardPayload: Encodable {
  let rewardType: String
  let amount: Int
}

struct ShownWithRewardResponse: Encodable {
  let shown: Bool
  let reward: RewardPayload?
}

struct PingResponse: Encodable {
  let value: String?
}

struct AdEventPayload: Encodable {
  let adType: String
  let event: String
  var error: String? = nil
  var reward: RewardPayload? = nil
}

// ============== Full Screen Delegate ==============

private class FullScreenEventsDelegate: NSObject, FullScreenContentDelegate {
  private let adType: String
  private weak var plugin: AdmobPlugin?
  private let onDismiss: () -> Void
  private let onFailToShow: (String) -> Void

  init(
    adType: String,
    plugin: AdmobPlugin,
    onDismiss: @escaping () -> Void,
    onFailToShow: @escaping (String) -> Void
  ) {
    self.adType = adType
    self.plugin = plugin
    self.onDismiss = onDismiss
    self.onFailToShow = onFailToShow
  }

  func adWillPresentFullScreenContent(_ ad: FullScreenPresentingAd) {
    plugin?.emitAdEvent(adType, "opened")
  }

  func adDidRecordImpression(_ ad: FullScreenPresentingAd) {
    plugin?.emitAdEvent(adType, "impression")
  }

  func adDidRecordClick(_ ad: FullScreenPresentingAd) {
    plugin?.emitAdEvent(adType, "clicked")
  }

  func ad(_ ad: FullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) {
    plugin?.emitAdEvent(adType, "failedToShow", error: error.localizedDescription)
    onFailToShow(error.localizedDescription)
  }

  func adDidDismissFullScreenContent(_ ad: FullScreenPresentingAd) {
    plugin?.emitAdEvent(adType, "closed")
    onDismiss()
  }
}

// ============== Plugin ==============

class AdmobPlugin: Plugin, BannerViewDelegate {
  private var webview: WKWebView?

  // Ad instances
  private var bannerView: BannerView?
  private var interstitialAd: InterstitialAd?
  private var rewardedAd: RewardedAd?
  private var rewardedInterstitialAd: RewardedInterstitialAd?
  private var appOpenAd: AppOpenAd?
  private var appOpenAdLoadTime: Date?

  // Strong references to full screen delegates (ads hold them weakly)
  private var interstitialDelegate: FullScreenEventsDelegate?
  private var rewardedDelegate: FullScreenEventsDelegate?
  private var rewardedInterstitialDelegate: FullScreenEventsDelegate?
  private var appOpenDelegate: FullScreenEventsDelegate?

  // Rewarded flows resolve when the ad is dismissed, carrying the earned reward
  private var pendingRewardedInvoke: Invoke?
  private var earnedReward: RewardPayload?
  private var pendingRewardedInterstitialInvoke: Invoke?
  private var earnedInterstitialReward: RewardPayload?

  @objc public override func load(webview: WKWebView) {
    self.webview = webview
  }

  private func rootViewController() -> UIViewController? {
    if let vc = webview?.window?.rootViewController {
      return vc
    }
    return UIApplication.shared.connectedScenes
      .compactMap { $0 as? UIWindowScene }
      .flatMap { $0.windows }
      .first(where: { $0.isKeyWindow })?
      .rootViewController
  }

  func emitAdEvent(
    _ adType: String, _ event: String, error: String? = nil, reward: RewardPayload? = nil
  ) {
    try? trigger(
      "adEvent", data: AdEventPayload(adType: adType, event: event, error: error, reward: reward))
  }

  // ============== Initialize ==============

  @objc public func initialize(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(InitializeArgs.self)

    DispatchQueue.main.async {
      let config = MobileAds.shared.requestConfiguration

      if let testDeviceIds = args.testDeviceIds, !testDeviceIds.isEmpty {
        config.testDeviceIdentifiers = testDeviceIds
      }
      if let tagForChild = args.tagForChildDirectedTreatment {
        config.tagForChildDirectedTreatment = NSNumber(value: tagForChild)
      }
      if let tagForUnderAge = args.tagForUnderAgeOfConsent {
        config.tagForUnderAgeOfConsent = NSNumber(value: tagForUnderAge)
      }

      MobileAds.shared.start { _ in
        Logger.info("AdMob SDK initialized")
      }

      invoke.resolve(InitializeResponse(initialized: true))
    }
  }

  // ============== Banner Ads ==============

  @objc public func show_banner(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(BannerAdArgs.self)

    DispatchQueue.main.async {
      guard let rootVC = self.rootViewController() else {
        invoke.reject("Failed to show banner: no root view controller")
        return
      }

      // Remove existing banner if any
      self.hideBannerInternal()

      let containerView: UIView = rootVC.view
      let contentWidth = containerView.frame.inset(by: containerView.safeAreaInsets).width

      let size = self.adSize(for: args.adSize ?? "BANNER", width: contentWidth)
      let banner = BannerView(adSize: size)
      banner.adUnitID = args.adUnitId
      banner.rootViewController = rootVC
      banner.delegate = self
      banner.translatesAutoresizingMaskIntoConstraints = false

      containerView.addSubview(banner)
      let offset = CGFloat(args.offset ?? 0)
      var constraints = [
        banner.centerXAnchor.constraint(equalTo: containerView.centerXAnchor)
      ]
      if (args.position ?? "bottom").lowercased() == "top" {
        constraints.append(
          banner.topAnchor.constraint(
            equalTo: containerView.safeAreaLayoutGuide.topAnchor, constant: offset))
      } else {
        constraints.append(
          banner.bottomAnchor.constraint(
            equalTo: containerView.safeAreaLayoutGuide.bottomAnchor, constant: -offset))
      }
      NSLayoutConstraint.activate(constraints)

      banner.load(Request())
      self.bannerView = banner

      invoke.resolve(ShowBannerResponse(shown: true, height: size.size.height))
    }
  }

  @objc public func hide_banner(_ invoke: Invoke) {
    DispatchQueue.main.async {
      self.hideBannerInternal()
      invoke.resolve(HideBannerResponse(hidden: true))
    }
  }

  private func hideBannerInternal() {
    bannerView?.removeFromSuperview()
    bannerView = nil
  }

  private func adSize(for size: String, width: CGFloat) -> AdSize {
    switch size.uppercased() {
    case "LARGE_BANNER": return AdSizeLargeBanner
    case "MEDIUM_RECTANGLE": return AdSizeMediumRectangle
    case "FULL_BANNER": return AdSizeFullBanner
    case "LEADERBOARD": return AdSizeLeaderboard
    case "ADAPTIVE": return currentOrientationAnchoredAdaptiveBanner(width: width)
    default: return AdSizeBanner
    }
  }

  // BannerViewDelegate

  public func bannerViewDidReceiveAd(_ bannerView: BannerView) {
    emitAdEvent("banner", "loaded")
  }

  public func bannerView(_ bannerView: BannerView, didFailToReceiveAdWithError error: Error) {
    emitAdEvent("banner", "failedToLoad", error: error.localizedDescription)
  }

  public func bannerViewDidRecordImpression(_ bannerView: BannerView) {
    emitAdEvent("banner", "impression")
  }

  public func bannerViewDidRecordClick(_ bannerView: BannerView) {
    emitAdEvent("banner", "clicked")
  }

  public func bannerViewWillPresentScreen(_ bannerView: BannerView) {
    emitAdEvent("banner", "opened")
  }

  public func bannerViewDidDismissScreen(_ bannerView: BannerView) {
    emitAdEvent("banner", "closed")
  }

  // ============== Interstitial Ads ==============

  @objc public func prepare_interstitial(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(AdUnitArgs.self)

    DispatchQueue.main.async {
      InterstitialAd.load(with: args.adUnitId, request: Request()) { ad, error in
        if let error = error {
          self.interstitialAd = nil
          self.emitAdEvent("interstitial", "failedToLoad", error: error.localizedDescription)
          invoke.resolve(LoadedResponse(loaded: false))
          return
        }

        self.interstitialAd = ad
        self.interstitialDelegate = FullScreenEventsDelegate(
          adType: "interstitial",
          plugin: self,
          onDismiss: { [weak self] in self?.interstitialAd = nil },
          onFailToShow: { [weak self] _ in self?.interstitialAd = nil }
        )
        ad?.fullScreenContentDelegate = self.interstitialDelegate
        self.emitAdEvent("interstitial", "loaded")
        invoke.resolve(LoadedResponse(loaded: true))
      }
    }
  }

  @objc public func show_interstitial(_ invoke: Invoke) {
    DispatchQueue.main.async {
      guard let ad = self.interstitialAd else {
        invoke.reject("Interstitial ad not ready. Call prepareInterstitial first.")
        return
      }
      ad.present(from: self.rootViewController())
      invoke.resolve(ShownResponse(shown: true))
    }
  }

  // ============== Rewarded Ads ==============

  @objc public func prepare_rewarded(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(AdUnitArgs.self)

    DispatchQueue.main.async {
      RewardedAd.load(with: args.adUnitId, request: Request()) { ad, error in
        if let error = error {
          self.rewardedAd = nil
          self.emitAdEvent("rewarded", "failedToLoad", error: error.localizedDescription)
          invoke.resolve(LoadedResponse(loaded: false))
          return
        }

        self.rewardedAd = ad
        self.rewardedDelegate = FullScreenEventsDelegate(
          adType: "rewarded",
          plugin: self,
          onDismiss: { [weak self] in
            guard let self = self else { return }
            self.rewardedAd = nil
            self.pendingRewardedInvoke?.resolve(
              ShownWithRewardResponse(shown: true, reward: self.earnedReward))
            self.pendingRewardedInvoke = nil
            self.earnedReward = nil
          },
          onFailToShow: { [weak self] message in
            guard let self = self else { return }
            self.rewardedAd = nil
            self.pendingRewardedInvoke?.reject("Rewarded ad failed to show: \(message)")
            self.pendingRewardedInvoke = nil
            self.earnedReward = nil
          }
        )
        ad?.fullScreenContentDelegate = self.rewardedDelegate
        self.emitAdEvent("rewarded", "loaded")
        invoke.resolve(LoadedResponse(loaded: true))
      }
    }
  }

  @objc public func show_rewarded(_ invoke: Invoke) {
    DispatchQueue.main.async {
      guard let ad = self.rewardedAd else {
        invoke.reject("Rewarded ad not ready. Call prepareRewarded first.")
        return
      }

      self.pendingRewardedInvoke = invoke
      self.earnedReward = nil

      ad.present(from: self.rootViewController()) { [weak self] in
        guard let self = self else { return }
        let reward = RewardPayload(
          rewardType: ad.adReward.type, amount: ad.adReward.amount.intValue)
        self.earnedReward = reward
        self.emitAdEvent("rewarded", "reward", reward: reward)
      }
    }
  }

  // ============== Rewarded Interstitial Ads ==============

  @objc public func prepare_rewarded_interstitial(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(AdUnitArgs.self)

    DispatchQueue.main.async {
      RewardedInterstitialAd.load(with: args.adUnitId, request: Request()) { ad, error in
        if let error = error {
          self.rewardedInterstitialAd = nil
          self.emitAdEvent(
            "rewardedInterstitial", "failedToLoad", error: error.localizedDescription)
          invoke.resolve(LoadedResponse(loaded: false))
          return
        }

        self.rewardedInterstitialAd = ad
        self.rewardedInterstitialDelegate = FullScreenEventsDelegate(
          adType: "rewardedInterstitial",
          plugin: self,
          onDismiss: { [weak self] in
            guard let self = self else { return }
            self.rewardedInterstitialAd = nil
            self.pendingRewardedInterstitialInvoke?.resolve(
              ShownWithRewardResponse(shown: true, reward: self.earnedInterstitialReward))
            self.pendingRewardedInterstitialInvoke = nil
            self.earnedInterstitialReward = nil
          },
          onFailToShow: { [weak self] message in
            guard let self = self else { return }
            self.rewardedInterstitialAd = nil
            self.pendingRewardedInterstitialInvoke?.reject(
              "Rewarded interstitial ad failed to show: \(message)")
            self.pendingRewardedInterstitialInvoke = nil
            self.earnedInterstitialReward = nil
          }
        )
        ad?.fullScreenContentDelegate = self.rewardedInterstitialDelegate
        self.emitAdEvent("rewardedInterstitial", "loaded")
        invoke.resolve(LoadedResponse(loaded: true))
      }
    }
  }

  @objc public func show_rewarded_interstitial(_ invoke: Invoke) {
    DispatchQueue.main.async {
      guard let ad = self.rewardedInterstitialAd else {
        invoke.reject("Rewarded interstitial ad not ready. Call prepareRewardedInterstitial first.")
        return
      }

      self.pendingRewardedInterstitialInvoke = invoke
      self.earnedInterstitialReward = nil

      ad.present(from: self.rootViewController()) { [weak self] in
        guard let self = self else { return }
        let reward = RewardPayload(
          rewardType: ad.adReward.type, amount: ad.adReward.amount.intValue)
        self.earnedInterstitialReward = reward
        self.emitAdEvent("rewardedInterstitial", "reward", reward: reward)
      }
    }
  }

  // ============== App Open Ads ==============

  @objc public func prepare_app_open(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(AdUnitArgs.self)

    DispatchQueue.main.async {
      AppOpenAd.load(with: args.adUnitId, request: Request()) { ad, error in
        if let error = error {
          self.appOpenAd = nil
          self.emitAdEvent("appOpen", "failedToLoad", error: error.localizedDescription)
          invoke.resolve(LoadedResponse(loaded: false))
          return
        }

        self.appOpenAd = ad
        self.appOpenAdLoadTime = Date()
        self.appOpenDelegate = FullScreenEventsDelegate(
          adType: "appOpen",
          plugin: self,
          onDismiss: { [weak self] in self?.appOpenAd = nil },
          onFailToShow: { [weak self] _ in self?.appOpenAd = nil }
        )
        ad?.fullScreenContentDelegate = self.appOpenDelegate
        self.emitAdEvent("appOpen", "loaded")
        invoke.resolve(LoadedResponse(loaded: true))
      }
    }
  }

  @objc public func show_app_open(_ invoke: Invoke) {
    DispatchQueue.main.async {
      guard let ad = self.appOpenAd, self.isAppOpenAdValid() else {
        invoke.reject("App open ad not ready or expired. Call prepareAppOpen first.")
        return
      }
      ad.present(from: self.rootViewController())
      invoke.resolve(ShownResponse(shown: true))
    }
  }

  private func isAppOpenAdValid() -> Bool {
    // App open ads are valid for 4 hours
    guard let loadTime = appOpenAdLoadTime else { return false }
    return Date().timeIntervalSince(loadTime) < 4 * 3600
  }

  // ============== Legacy Ping Command ==============

  @objc public func ping(_ invoke: Invoke) throws {
    let args = try invoke.parseArgs(PingArgs.self)
    invoke.resolve(PingResponse(value: args.value ?? "pong"))
  }
}

@_cdecl("init_plugin_google_admob")
func initPluginGoogleAdmob() -> Plugin {
  return AdmobPlugin()
}
