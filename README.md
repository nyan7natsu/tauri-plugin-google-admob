# Tauri Plugin Google AdMob

A comprehensive Tauri plugin for integrating Google AdMob advertisements into your mobile applications. This plugin supports all major ad formats including banner, interstitial, rewarded, rewarded interstitial, and app open ads.

[![Crate](https://img.shields.io/crates/v/tauri-plugin-google-admob.svg)](https://crates.io/crates/tauri-plugin-google-admob)
[![License](https://img.shields.io/badge/License-MIT%20OR%20Apache--2.0-blue.svg)](https://github.com/Atlas-OS/tauri-plugin-google-admob)

## Features

- 🎯 **Banner Ads**: Display banner advertisements with customizable positioning
- 📱 **Interstitial Ads**: Full-screen ads that appear at natural app transition points
- 🎁 **Rewarded Ads**: Users earn rewards for viewing video advertisements
- 🏆 **Rewarded Interstitial Ads**: Full-screen ads with reward mechanics
- 🚀 **App Open Ads**: Monetize app launch and resume events
- 🔄 **Event System**: Listen to ad lifecycle events (loaded, displayed, clicked, etc.)
- 🎨 **Customizable**: Configure ad sizes, positions, and behavior
- 🛡️ **Type Safe**: Full TypeScript support with comprehensive type definitions

## Installation

Add the plugin to your Tauri application:

### Rust (Cargo.toml)
```toml
[dependencies]
tauri-plugin-google-admob = "1.0.0"
```

### JavaScript/TypeScript
```bash
npm install tauri-plugin-google-admob
# or
yarn add tauri-plugin-google-admob
# or
pnpm add tauri-plugin-google-admob
```

## Setup

### 1. Register the Plugin

In your Tauri application's `src-tauri/src/main.rs`:

```rust
use tauri_plugin_google_admob::GoogleAdmobPlugin;

fn main() {
    tauri::Builder::default()
        .plugin(GoogleAdmobPlugin::init())
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

### 2. Add Permissions

Add the required permissions to your `src-tauri/capabilities/default.json`:

```json
{
  "identifier": "default",
  "windows": ["main"],
  "permissions": [
    "core:default",
    "google-admob:allow-initialize",
    "google-admob:allow-show-banner",
    "google-admob:allow-hide-banner",
    "google-admob:allow-prepare-interstitial",
    "google-admob:allow-show-interstitial",
    "google-admob:allow-prepare-rewarded",
    "google-admob:allow-show-rewarded",
    "google-admob:allow-prepare-rewarded-interstitial",
    "google-admob:allow-show-rewarded-interstitial",
    "google-admob:allow-prepare-app-open",
    "google-admob:allow-show-app-open"
  ]
}
```

### 3. Android Configuration

Add the Google Mobile Ads SDK to your Android configuration:

**android/build.gradle.kts:**
```kotlin
    composeOptions {
        kotlinCompilerExtensionVersion = "2.1.0" 
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
dependencies {
    implementation("com.google.android.gms:play-services-ads:24.9.0")
}
```

**android/src/main/AndroidManifest.xml:**
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- Add these permissions -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <application>
        <!-- Add your AdMob App ID -->
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-3940256099942544~3347511713"/>
    </application>
</manifest>
```

## Usage

### Initialize AdMob

```typescript
import { initialize } from 'tauri-plugin-google-admob';

// Initialize with your AdMob configuration
await initialize({
  app_id: 'ca-app-pub-3940256099942544~3347511713', // Your AdMob App ID
  test_device_ids: ['DEVICE_ID_1', 'DEVICE_ID_2'], // Optional test devices
  is_debug: true, // Enable debug mode for development
  tag_for_child_directed_treatment: null,
  tag_for_under_age_of_consent: null
});
```

### Banner Ads

```typescript
import { showBanner, hideBanner } from 'tauri-plugin-google-admob';

// Show a banner ad
await showBanner({
  ad_id: 'ca-app-pub-3940256099942544/6300978111', // Test banner ad unit ID
  position: 'BOTTOM', // 'TOP' | 'BOTTOM'
  size: 'BANNER' // 'BANNER' | 'LARGE_BANNER' | 'MEDIUM_RECTANGLE' | 'FULL_BANNER' | 'LEADERBOARD' | 'SMART_BANNER'
});

// Hide the banner
await hideBanner();
```

### Interstitial Ads

```typescript
import { prepareInterstitial, showInterstitial } from 'tauri-plugin-google-admob';

// Prepare the interstitial ad
await prepareInterstitial({
  ad_id: 'ca-app-pub-3940256099942544/1033173712' // Test interstitial ad unit ID
});

// Show when ready
await showInterstitial();
```

### Rewarded Ads

```typescript
import { prepareRewarded, showRewarded } from 'tauri-plugin-google-admob';

// Prepare the rewarded ad
await prepareRewarded({
  ad_id: 'ca-app-pub-3940256099942544/5224354917' // Test rewarded ad unit ID
});

// Show and handle rewards
await showRewarded();
```

### Rewarded Interstitial Ads

```typescript
import { prepareRewardedInterstitial, showRewardedInterstitial } from 'tauri-plugin-google-admob';

// Prepare the rewarded interstitial ad
await prepareRewardedInterstitial({
  ad_id: 'ca-app-pub-3940256099942544/5354046379' // Test rewarded interstitial ad unit ID
});

// Show when ready
await showRewardedInterstitial();
```

### App Open Ads

```typescript
import { prepareAppOpen, showAppOpen } from 'tauri-plugin-google-admob';

// Prepare the app open ad
await prepareAppOpen({
  ad_id: 'ca-app-pub-3940256099942544/9257395921', // Test app open ad unit ID
  orientation: 'PORTRAIT' // 'PORTRAIT' | 'LANDSCAPE'
});

// Show when app becomes active
await showAppOpen();
```

### Listening to Ad Events

```typescript
import { listen } from '@tauri-apps/api/event';

// Listen to ad events
await listen('admob://banner_loaded', (event) => {
  console.log('Banner ad loaded:', event.payload);
});

await listen('admob://banner_failed_to_load', (event) => {
  console.log('Banner ad failed to load:', event.payload);
});

await listen('admob://interstitial_loaded', (event) => {
  console.log('Interstitial ad loaded');
});

await listen('admob://rewarded_earned_reward', (event) => {
  console.log('User earned reward:', event.payload);
});
```

## API Reference

### Types

```typescript
// Configuration
export interface InitializeRequest {
  app_id: string;
  test_device_ids?: string[];
  is_debug?: boolean;
  tag_for_child_directed_treatment?: boolean | null;
  tag_for_under_age_of_consent?: boolean | null;
}

// Banner Ad Options
export interface BannerAdOptions {
  ad_id: string;
  position: AdPosition;
  size: BannerAdSize;
}

export type AdPosition = 'TOP' | 'BOTTOM';
export type BannerAdSize = 'BANNER' | 'LARGE_BANNER' | 'MEDIUM_RECTANGLE' | 'FULL_BANNER' | 'LEADERBOARD' | 'SMART_BANNER';

// Other Ad Options
export interface InterstitialAdOptions {
  ad_id: string;
}

export interface RewardedAdOptions {
  ad_id: string;
}

export interface RewardedInterstitialAdOptions {
  ad_id: string;
}

export interface AppOpenAdOptions {
  ad_id: string;
  orientation: AdOrientation;
}

export type AdOrientation = 'PORTRAIT' | 'LANDSCAPE';
```

### Commands

| Command | Parameters | Description |
|---------|------------|-------------|
| `initialize` | `InitializeRequest` | Initialize AdMob with your app configuration |
| `showBanner` | `BannerAdOptions` | Display a banner advertisement |
| `hideBanner` | - | Hide the currently displayed banner |
| `prepareInterstitial` | `InterstitialAdOptions` | Load an interstitial ad for later display |
| `showInterstitial` | - | Show the prepared interstitial ad |
| `prepareRewarded` | `RewardedAdOptions` | Load a rewarded ad for later display |
| `showRewarded` | - | Show the prepared rewarded ad |
| `prepareRewardedInterstitial` | `RewardedInterstitialAdOptions` | Load a rewarded interstitial ad |
| `showRewardedInterstitial` | - | Show the prepared rewarded interstitial ad |
| `prepareAppOpen` | `AppOpenAdOptions` | Load an app open ad for later display |
| `showAppOpen` | - | Show the prepared app open ad |

### Events

The plugin emits various events during the ad lifecycle:

| Event | Description |
|-------|-------------|
| `admob://banner_loaded` | Banner ad successfully loaded |
| `admob://banner_failed_to_load` | Banner ad failed to load |
| `admob://banner_opened` | Banner ad was clicked/opened |
| `admob://banner_closed` | Banner ad was closed |
| `admob://interstitial_loaded` | Interstitial ad successfully loaded |
| `admob://interstitial_failed_to_load` | Interstitial ad failed to load |
| `admob://interstitial_showed` | Interstitial ad was displayed |
| `admob://interstitial_failed_to_show` | Interstitial ad failed to show |
| `admob://interstitial_dismissed` | Interstitial ad was dismissed |
| `admob://rewarded_loaded` | Rewarded ad successfully loaded |
| `admob://rewarded_failed_to_load` | Rewarded ad failed to load |
| `admob://rewarded_showed` | Rewarded ad was displayed |
| `admob://rewarded_failed_to_show` | Rewarded ad failed to show |
| `admob://rewarded_dismissed` | Rewarded ad was dismissed |
| `admob://rewarded_earned_reward` | User earned a reward |

## Test Ad Unit IDs

For testing purposes, use these Google-provided test ad unit IDs:

| Ad Format | Test Ad Unit ID |
|-----------|----------------|
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |
| Rewarded Interstitial | `ca-app-pub-3940256099942544/5354046379` |
| App Open | `ca-app-pub-3940256099942544/9257395921` |

## Example Application

Check out the complete example in the [`examples/tauri-app`](examples/tauri-app) directory for a full implementation showing all ad types and event handling.

## Troubleshooting

### Common Issues

**1. Plugin commands not found**
- Ensure you've added all required permissions to your capabilities file
- Verify the plugin is registered in your Rust main function

**2. Ads not loading**
- Check your internet connection
- Verify your AdMob App ID and ad unit IDs are correct
- Make sure you've added the required permissions to AndroidManifest.xml
- For production, ensure your app is approved by AdMob

**3. Android build issues**
- Ensure you have the correct Google Mobile Ads SDK version
- Check that your Android API level is compatible (minimum API 19)

**4. TypeScript errors**
- Make sure you've installed the plugin's npm package
- Import types from 'tauri-plugin-google-admob'

### Debug Mode

Enable debug mode during development to get detailed logging:

```typescript
await initialize({
  app_id: 'your-app-id',
  is_debug: true // Enable debug logging
});
```

## Contributing

Contributions are welcome! Please read our [contributing guidelines](CONTRIBUTING.md) before submitting pull requests.

## License

This project is licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or http://www.apache.org/licenses/LICENSE-2.0)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or http://opensource.org/licenses/MIT)

at your option.

## Acknowledgments

- [Google Mobile Ads SDK](https://developers.google.com/admob/android/quick-start)
- [Tauri](https://tauri.app/) - Build smaller, faster, and more secure desktop applications
