<div align="center">
  <img src="assets/icon.png" alt="SimpleShare Icon" width="110" />
<h1 align="center">SimpleShare</h1>
<div align="center">

  <a href="">![GitHub Release](https://img.shields.io/github/v/release/NightMean/Android_SimpleShare?style=flat)</a>
  <a href="">![GitHub Issues](https://img.shields.io/github/issues/NightMean/Android_SimpleShare?style=flat)</a>
  <a href="">![GitHub Pull Requests](https://img.shields.io/github/issues-pr/NightMean/Android_SimpleShare?color=blue)</a>
  <a href="">![GitHub License](https://img.shields.io/github/license/NightMean/Android_SimpleShare?style=flat)</a>
  </div>
<p align="center">Android file browser to quickly share files to your favorite app on Android 9 (Pie) and newer</p>
</div>

## Overview

**SimpleShare** bypasses the clumsy system share menu, letting you easily share files directly to your chosen app (e.g., Google Photos, WhatsApp, Google Drive) in a single tap.

## Features
*   **Direct-to-App Sharing**: Bypass the system menu and share instantly to your pre-selected app.
*   **Specific Actions**: Deep link sharing directly into specific chats or app modes.
*   **File Browser**: Thumbnail previews,
 Drag-to-select gesture, File visibility modes, Persistent file selection across folders and powerful search.
*   **Customizable**: Configure your default startup folder, Automatically check for sufficient storage space before sharing (For apps that might copy shared files to cache).
*   **Modern Design**: Built with Jetpack Compose for a fluid, responsive experience.

## Screenshots

<p align="center">
  <img src="assets/Screenshots/SimpleShare_Startup_Page.png" width="19%" alt="Startup Page" />
  <img src="assets/Screenshots/SimpleShare_FileBrowser_List.png" width="19%" alt="File Browser List" />
  <img src="assets/Screenshots/SimpleShare_FileBrowser_Gallery.png" width="19%" alt="File Browser Gallery" />
  <img src="assets/Screenshots/SimpleShare_Settings.png" width="19%" alt="Settings" />
  <img src="assets/Screenshots/SimpleShare_Target_Share_App.png" width="19%" alt="Target App Selection" />  
</p>

## Download
You can download **SimpleShare** directly from the GitHub releases page or by tapping on the badge below.
<p align="center">  
  <a href="https://github.com/NightMean/Android_SimpleShare/releases/latest">
    <img src="assets/Download_Get_it_on_Github.png" alt="Get it on GitHub" height="60">
  </a>
</p>


## Dependencies

*   [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern toolkit for building native UI.
*   [Glide](https://github.com/bumptech/glide) - Fast and efficient image loading framework.
*   [AndroidX](https://developer.android.com/jetpack/androidx) - Core Android libraries and lifecycle components.

## Building

Requires JDK 17 and Android SDK 35. Use the included Gradle wrapper:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Localization

The app is English-only by design; UI strings are not extracted to resources yet.
Translations are welcome once strings are externalized.


## Donations
To support me you can use link below:

<a href="https://www.buymeacoffee.com/nightmean" target="_blank"><img src="https://cdn.buymeacoffee.com/buttons/v2/default-yellow.png" alt="Buy Me A Coffee" style="width: 200px !important;" ></a>

# License

This project is licensed under the [GNU General Public License Version 3](https://www.gnu.org/licenses/gpl-3.0.html). For details, see [LICENSE](LICENSE)
