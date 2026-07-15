# Tauri Plugin Google AdMob

Google AdMob を Tauri で使用するためのプラグイン．
スマートフォンアプリケーション向けです．

[VMASPAD/tauri-plugin-google-admob](https://github.com/VMASPAD/tauri-plugin-google-admob) のForkで，**上流に存在しなかったiOS実装を追加**しています．

## 対応状況

| プラットフォーム | 対応 |
|---|---|
| Android | ✅(上流由来 + 拡張) |
| iOS | ✅(本Forkで実装) |
| デスクトップ | ❌(スタブのみ) |

対応広告フォーマット: バナー / インタースティシャル / リワード / リワードインタースティシャル / アプリオープン + 広告ライフサイクルイベント(`adEvent`)

> [!NOTE]
> crates.io / npm には公開していないため，インストールはgit経由で行います．

## インストール

### 1. Rust側

`src-tauri/Cargo.toml` に追加:

```toml
[dependencies]
tauri-plugin-google-admob = { git = "https://github.com/nyan7natsu/tauri-plugin-admob" }
```

またはコマンドで:

```sh
cargo add tauri-plugin-google-admob --git https://github.com/nyan7natsu/tauri-plugin-admob
```

`src-tauri/src/lib.rs` でプラグインを登録:

```rust
tauri::Builder::default()
    .plugin(tauri_plugin_google_admob::init())
    // ...
```

`src-tauri/capabilities/default.json` に権限を追加:

```json
{
  "permissions": [
    "google-admob:default"
  ]
}
```

### 2. JavaScript側

pnpm の場合，先に `pnpm-workspace.yaml` でビルドスクリプトを許可してからインストール
(git依存はインストール時に `prepare` スクリプトでTSをビルドするため):

```yaml
# pnpm-workspace.yaml
allowBuilds:
  tauri-plugin-google-admob-api: true
```

```sh
pnpm add github:nyan7natsu/tauri-plugin-admob
```

npm / yarn の場合は許可設定は不要です:

```sh
npm install github:nyan7natsu/tauri-plugin-admob
```

依存を増やしたくない場合は，[`guest-js/index.ts`](guest-js/index.ts) を直接アプリにコピーしても動きます(依存は `@tauri-apps/api` のみ)．

### サブモジュール運用(iOSで使うなら推奨)

Cargoのgit依存はチェックアウト先が `~/.cargo/git/checkouts/<名前>-<ハッシュ>/<コミット>/` となり
**パスが安定しない**ため，後述するiOSの `FRAMEWORK_SEARCH_PATHS` から参照できません．
iOSで使う場合はサブモジュールとしてアプリリポジトリ内に置くのが確実です．

```sh
# アプリリポジトリのルートで
git submodule add https://github.com/nyan7natsu/tauri-plugin-admob plugins/tauri-plugin-admob
```

Rust側は git 依存の代わりに**パス依存**にします:

```toml
# src-tauri/Cargo.toml
[dependencies]
tauri-plugin-google-admob = { path = "../plugins/tauri-plugin-admob" }
```

JS側はパスの安定性と無関係なので，前述の `pnpm add github:...` のままで問題ありません
(サブモジュールに合わせたければ `guest-js/index.ts` のコピーでも可)．

iOSの `FRAMEWORK_SEARCH_PATHS`(後述)は `$(SRCROOT)` = `src-tauri/gen/apple` からの相対で書けます．
プラグインをリポジトリ直下の `plugins/` に置いた場合:

```
$(SRCROOT)/../../../plugins/tauri-plugin-admob/ios/.build/artifacts/...
```

日常運用のコマンド:

```sh
# クローン時(サブモジュールも一緒に取得)
git clone --recurse-submodules <アプリのリポジトリ>
# 既存のクローンに後から取得
git submodule update --init

# プラグインの更新を取り込む(親リポジトリには「参照コミットの変更」として記録される)
git -C plugins/tauri-plugin-admob pull origin main
git add plugins/tauri-plugin-admob
git commit -m "chore: update tauri-plugin-admob"
```

補足:

- ビルド生成物(`ios/.build/`，`.tauri/`，`target/`)はプラグイン側の `.gitignore` で
  無視されるため，ビルドしてもサブモジュールは dirty になりません
- プラグインに手を入れたい場合はサブモジュール内で直接編集 → プラグイン側リポジトリへ
  commit/push → 親リポジトリで参照コミットを更新，という流れになります

### 3. Android セットアップ

`src-tauri/gen/android/app/src/main/AndroidManifest.xml` の `<application>` 内にAdMobアプリIDを追加:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy"/>
```

テスト用アプリID: `ca-app-pub-3940256099942544~3347511713`

### 4. iOS セットアップ

最低iOSバージョンを15.0にすることを推奨(`tauri.conf.json`):

```json
{
  "bundle": {
    "iOS": { "minimumSystemVersion": "15.0" }
  }
}
```

`src-tauri/gen/apple/project.yml` のアプリターゲットに以下を追加します．
`<plugin>` はこのプラグインの場所で，[サブモジュール運用](#サブモジュール運用iosで使うなら推奨)なら
`$(SRCROOT)/../../../plugins/tauri-plugin-admob` のように相対パスで書けます:

```yaml
targets:
  <アプリ名>_iOS:
    info:
      properties:
        GADApplicationIdentifier: ca-app-pub-xxxxxxxxxxxxxxxx~yyyyyyyyyy  # AdMobアプリID
        SKAdNetworkItems:
          - SKAdNetworkIdentifier: cstr6suwn9.skadnetwork
    settings:
      base:
        FRAMEWORK_SEARCH_PATHS[sdk=iphoneos*]: $(inherited) <plugin>/ios/.build/artifacts/swift-package-manager-google-mobile-ads/GoogleMobileAds/GoogleMobileAds.xcframework/ios-arm64 <plugin>/ios/.build/artifacts/swift-package-manager-google-user-messaging-platform/UserMessagingPlatform/UserMessagingPlatform.xcframework/ios-arm64
        FRAMEWORK_SEARCH_PATHS[sdk=iphonesimulator*]: $(inherited) <plugin>/ios/.build/artifacts/swift-package-manager-google-mobile-ads/GoogleMobileAds/GoogleMobileAds.xcframework/ios-arm64_x86_64-simulator <plugin>/ios/.build/artifacts/swift-package-manager-google-user-messaging-platform/UserMessagingPlatform/UserMessagingPlatform.xcframework/ios-arm64_x86_64-simulator
        OTHER_LDFLAGS: $(inherited) -framework GoogleMobileAds -framework UserMessagingPlatform
        ENABLE_DEBUG_DYLIB: false
```

変更後に `gen/apple` で `xcodegen generate` を実行してください．

テスト用アプリID(iOS): `ca-app-pub-3940256099942544~1458002511`

> [!IMPORTANT]
> - `GADApplicationIdentifier` がないとSDKの初期化時にクラッシュします
> - `ENABLE_DEBUG_DYLIB: false` は必須です．Xcode 16以降のデバッグdylib分離が有効だと，
>   静的リンクされたSwiftコードのシンボリック参照解決が実機で失敗し，起動直後にフリーズ→abortします
>   (シミュレータでは発症しません)

## 使い方

```ts
import {
  initialize,
  showBanner,
  hideBanner,
  prepareRewarded,
  showRewarded,
  onAdEvent,
} from "tauri-plugin-google-admob-api";

// SDK初期化(最初に1回)
await initialize({});

// 広告イベントの購読
await onAdEvent((ev) => {
  console.log(ev.adType, ev.event, ev.error, ev.reward);
});

// バナー表示(位置: top/bottom + 画面端からのオフセット)
const { height } = await showBanner({
  adUnitId: "ca-app-pub-3940256099942544/2934735716", // iOSテスト用バナーID
  position: "bottom",
  adSize: "ADAPTIVE",
  offset: 0,
});
// ネイティブバナーはWebViewの上に重なるため，返ってきた height(pt/dp)ぶん
// コンテンツにpaddingを与えると被りを回避できます
document.documentElement.style.setProperty("--ad-pad-bottom", `${height}px`);

await hideBanner();

// リワード広告(prepare → show の2段階)
const { loaded } = await prepareRewarded({
  adUnitId: "ca-app-pub-3940256099942544/1712485313", // iOSテスト用リワードID
});
if (loaded) {
  const result = await showRewarded();
  if (result.reward) {
    console.log(`報酬獲得: ${result.reward.amount} ${result.reward.rewardType}`);
  }
}
```

インタースティシャル / リワードインタースティシャル / アプリオープンも同じ
`prepareXxx()` → `showXxx()` の形です．

### テスト広告ユニットID

開発中は必ず[Google公式のテスト用ID](https://developers.google.com/admob/ios/test-ads)を使用してください．

| フォーマット | Android | iOS |
|---|---|---|
| バナー | `ca-app-pub-3940256099942544/9214589741` | `ca-app-pub-3940256099942544/2934735716` |
| インタースティシャル | `ca-app-pub-3940256099942544/1033173712` | `ca-app-pub-3940256099942544/4411468910` |
| リワード | `ca-app-pub-3940256099942544/5224354917` | `ca-app-pub-3940256099942544/1712485313` |
| リワードインタースティシャル | `ca-app-pub-3940256099942544/5354046379` | `ca-app-pub-3940256099942544/6978759866` |
| アプリオープン | `ca-app-pub-3940256099942544/9257395921` | `ca-app-pub-3940256099942544/5575463023` |

### AndroidとiOSの挙動差

`showRewarded()` / `showRewardedInterstitial()` の resolve タイミングが異なります:

- **iOS**: 広告が**閉じられた時点**で resolve．報酬未獲得なら `reward` は `undefined`
- **Android**(上流実装): 報酬獲得時のみ resolve

## iOS実装に関する技術メモ

TauriのプラグインSwiftパッケージは通常 `tauri-plugin` の `.ios_path()`(内部はswift-rs)で
ビルドされますが，swift-rsは**macOSビルドプラン**でSwiftパッケージをビルドするため，
Google Mobile Ads SDK のような**バイナリxcframework依存のiOSスライスを解決できず**
`no such module 'GoogleMobileAds'` で失敗します．

そのため本Forkの `build.rs` は `swift build --triple <iosトリプル>` を直接実行し，
生成された静的ライブラリとxcframeworkスライスへのリンクフラグをcargoに渡しています．
xcframeworkはSwiftPMが `ios/.build/artifacts/` に展開するので，
アプリ側の `FRAMEWORK_SEARCH_PATHS` でそこを参照します(上記セットアップ参照)．

## ライセンス

MIT OR Apache-2.0(上流と同じ)
