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
  tauri_plugin::Builder::new(COMMANDS)
    .android_path("android")
    .ios_path("ios")
    .build();
}
