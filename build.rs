use std::path::PathBuf;
use std::process::Command;

const COMMANDS: &[&str] = &[
  "ping",
  "register_listener",
  "remove_listener",
  "initialize",
  "show_banner",
  "hide_banner",
  "prepare_interstitial",
  "show_interstitial",
  "prepare_rewarded",
  "show_rewarded",
  "prepare_rewarded_interstitial",
  "show_rewarded_interstitial",
  "prepare_app_open",
  "show_app_open",
];

fn main() {
  // Android + permissions are handled by tauri-plugin as usual. The iOS Swift
  // package is NOT registered via `.ios_path()` because tauri-plugin delegates
  // to swift-rs, which builds Swift packages with a macOS build plan (only
  // overriding the swiftc target triple). Under a macOS plan SwiftPM never
  // selects the iOS slice of binary xcframework dependencies, so
  // `import GoogleMobileAds` fails with "no such module". We instead run
  // `swift build --triple <ios-triple>` ourselves below, which resolves the
  // xcframework slices correctly.
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .build();

  let target_os = std::env::var("CARGO_CFG_TARGET_OS").unwrap_or_default();
  if target_os == "ios" {
    build_ios_package();
  }
}

fn build_ios_package() {
  let manifest_dir = PathBuf::from(std::env::var("CARGO_MANIFEST_DIR").unwrap());
  let ios_dir = manifest_dir.join("ios");

  // Mirror what tauri-plugin's `ios_path` does: place a copy of the Tauri
  // Swift API where Package.swift expects it (../.tauri/tauri-api).
  let tauri_api_src = std::env::var("DEP_TAURI_IOS_LIBRARY_PATH").expect(
    "missing `DEP_TAURI_IOS_LIBRARY_PATH` environment variable. Make sure `tauri` is a dependency of the plugin.",
  );
  let tauri_dep_dir = manifest_dir.join(".tauri");
  let tauri_api_dst = tauri_dep_dir.join("tauri-api");
  let _ = std::fs::remove_dir_all(&tauri_api_dst);
  std::fs::create_dir_all(&tauri_dep_dir).expect("failed to create .tauri directory");
  let status = Command::new("cp")
    .arg("-R")
    .arg(&tauri_api_src)
    .arg(&tauri_api_dst)
    .status()
    .expect("failed to run cp");
  assert!(status.success(), "failed to copy tauri-api to the plugin");
  let _ = std::fs::remove_dir_all(tauri_api_dst.join(".build"));
  let _ = std::fs::remove_file(tauri_api_dst.join("Package.resolved"));
  let _ = std::fs::remove_dir_all(tauri_api_dst.join("Tests"));

  let target = std::env::var("TARGET").unwrap();
  let min_version =
    std::env::var("IPHONEOS_DEPLOYMENT_TARGET").unwrap_or_else(|_| "14.0".to_string());
  let (triple, sdk, product_dir, xcframework_slice) = match target.as_str() {
    "aarch64-apple-ios" => (
      format!("arm64-apple-ios{min_version}"),
      "iphoneos",
      "arm64-apple-ios",
      "ios-arm64",
    ),
    "aarch64-apple-ios-sim" => (
      format!("arm64-apple-ios{min_version}-simulator"),
      "iphonesimulator",
      "arm64-apple-ios-simulator",
      "ios-arm64_x86_64-simulator",
    ),
    "x86_64-apple-ios" => (
      format!("x86_64-apple-ios{min_version}-simulator"),
      "iphonesimulator",
      "x86_64-apple-ios-simulator",
      "ios-arm64_x86_64-simulator",
    ),
    other => panic!("unsupported iOS target: {other}"),
  };

  let sdk_path_output = Command::new("xcrun")
    .args(["--sdk", sdk, "--show-sdk-path"])
    .output()
    .expect("failed to run xcrun");
  assert!(sdk_path_output.status.success(), "failed to get {sdk} SDK path");
  let sdk_path = String::from_utf8_lossy(&sdk_path_output.stdout)
    .trim()
    .to_string();

  let profile = if std::env::var("DEBUG").map(|v| v == "true").unwrap_or(false) {
    "debug"
  } else {
    "release"
  };

  // A stable scratch path (instead of OUT_DIR) so the Xcode project can
  // reference the extracted xcframework slices in FRAMEWORK_SEARCH_PATHS.
  let scratch_path = ios_dir.join(".build");

  let status = Command::new("swift")
    .current_dir(&ios_dir)
    // When invoked from Xcode's "Build Rust Code" phase, SDKROOT points at the
    // iOS SDK, which breaks Package.swift manifest compilation (it targets the
    // host macOS). The target SDK is passed explicitly via --sdk instead.
    .env_remove("SDKROOT")
    .args(["build", "-c", profile, "--sdk", &sdk_path, "--triple", &triple])
    .arg("--scratch-path")
    .arg(&scratch_path)
    .status()
    .expect("failed to run swift build");
  assert!(
    status.success(),
    "swift build failed for the tauri-plugin-google-admob iOS package"
  );

  let products = scratch_path.join(product_dir).join(profile);
  println!("cargo:rustc-link-search=native={}", products.display());
  println!("cargo:rustc-link-lib=static=tauri-plugin-google-admob");

  // The GMA SDK ships as static xcframeworks; link the slice matching the
  // target so the cdylib/final link can resolve GAD*/UMP* symbols.
  let artifacts = scratch_path.join("artifacts");
  let frameworks = [
    (
      "GoogleMobileAds",
      artifacts
        .join("swift-package-manager-google-mobile-ads")
        .join("GoogleMobileAds")
        .join("GoogleMobileAds.xcframework"),
    ),
    (
      "UserMessagingPlatform",
      artifacts
        .join("swift-package-manager-google-user-messaging-platform")
        .join("UserMessagingPlatform")
        .join("UserMessagingPlatform.xcframework"),
    ),
  ];
  for (name, xcframework) in frameworks {
    println!(
      "cargo:rustc-link-search=framework={}",
      xcframework.join(xcframework_slice).display()
    );
    println!("cargo:rustc-link-lib=framework={name}");
  }

  println!(
    "cargo:rerun-if-changed={}",
    ios_dir.join("Sources").join("AdmobPlugin.swift").display()
  );
  println!(
    "cargo:rerun-if-changed={}",
    ios_dir.join("Package.swift").display()
  );
}
