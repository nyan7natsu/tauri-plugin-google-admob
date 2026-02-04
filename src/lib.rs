use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::GoogleAdmob;
#[cfg(mobile)]
use mobile::GoogleAdmob;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the google-admob APIs.
pub trait GoogleAdmobExt<R: Runtime> {
    fn google_admob(&self) -> &GoogleAdmob<R>;
}

impl<R: Runtime, T: Manager<R>> crate::GoogleAdmobExt<R> for T {
    fn google_admob(&self) -> &GoogleAdmob<R> {
        self.state::<GoogleAdmob<R>>().inner()
    }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("google-admob")
        .invoke_handler(tauri::generate_handler![
            commands::ping,
            commands::initialize,
            commands::show_banner,
            commands::hide_banner,
            commands::prepare_interstitial,
            commands::show_interstitial,
            commands::prepare_rewarded,
            commands::show_rewarded,
            commands::prepare_rewarded_interstitial,
            commands::show_rewarded_interstitial,
            commands::prepare_app_open,
            commands::show_app_open,
        ])
        .setup(|app, api| {
            #[cfg(mobile)]
            let google_admob = mobile::init(app, api)?;
            #[cfg(desktop)]
            let google_admob = desktop::init(app, api)?;
            app.manage(google_admob);
            Ok(())
        })
        .build()
}
