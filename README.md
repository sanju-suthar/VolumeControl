# 🔊 Edge Volume — Screen Edge Gesture Volume Control

![GitHub release (latest by date)](https://img.shields.io/github/v/release/sanju-suthar/VolumeControl?style=flat-square)
![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B-brightgreen?style=flat-square)
![Build](https://img.shields.io/badge/Build-GitHub%20Actions-blue?style=flat-square)
![Issues](https://img.shields.io/github/issues/sanju-suthar/VolumeControl?style=flat-square)

**Edge Volume** is an ultra-lightweight, zero-bloat Android utility that allows you to adjust your device's **Media Volume** simply by swiping **Up** or **Down** along the left or right edge of your screen. Designed specifically to save physical volume buttons from wear and tear while running reliably 24/7 in the background.

---

## 📥 Download Latest Signed APK

Download the ready-to-install, signed Release APK directly from the **Releases** section:

👉 **[Download Latest APK Here](https://github.com/sanju-suthar/VolumeControl/releases/latest)**

---

## ✨ Key Features

* **👆 Smooth Edge Swipe Gestures:** Swipe **Up** to increase media volume and **Down** to lower it from anywhere on your phone, triggering the native system volume UI.
* **↔️ Independent Left & Right Edge Toggles:** Enable gestures on the Left edge, Right edge, or both simultaneously.
* **👻 100% Invisible Strip Mode:** Configure your preferred position using the translucent blue strip, then toggle **100% Invisible Edge Strip** so it never blocks your screen view.
* **🎚️ Real-Time Customization:**
  * **Edge Width:** `5 dp` to `60 dp`
  * **Edge Height:** `20%` to `100%` of screen height
  * **Swipe Speed (Sensitivity):** `1x` to `10x`
* **🔘 In-App Volume Buttons:** Includes dedicated `Volume +` and `Volume -` buttons inside the dashboard for instant control if your hardware keys are completely broken.
* **🔒 Privacy-First Architecture:** 
  * **No `AccessibilityService`** (avoids Play Protect warnings and UI lag).
  * **No `VIBRATE` or `INTERNET` permission** (100% offline, zero haptics, zero tracking).
* **🔄 24/7 Auto-Start (`BootReceiver`):** Automatically starts on device boot (`BOOT_COMPLETED` / Direct Boot) and restarts if swiped away from Recent Apps.

---

## ⚙️ Quick Setup Guide (After Installing)

1. **Grant Overlay Permission:** Open the app and tap **Button #1 (`Grant 'Display Over Other Apps' Permission`)**.
2. **Enable 24/7 Reliability:** Tap **Button #2 (`Disable Battery Optimization`)** and allow unrestricted background usage.
   * *For Realme UI / ColorOS / MIUI Users:* Go to `App Info -> Battery Usage` and enable **"Allow background activity"** and **"Allow auto-launch"**.
3. **Hide Persistent Notifications (Optional):**
   * Tap **Button #3** to turn off the app's own background service notification.
   * Tap **Button #4** to turn off Android System's *"Displaying over other apps"* notification.
4. **Go Stealth:** Once you are happy with the Edge Width and Height, turn ON the **"100% Invisible Edge Strip"** switch!

---

## 🎨 Custom App Icon Support (For Forkers / Builders)

This repository includes an automated ImageMagick pipeline inside `.github/workflows/build.yml`:
* Simply upload any image named **`icon.png`** or **`icon.jpg`** to the root of the repository.
* GitHub Actions will automatically crop, resize, and generate `ic_launcher.png` and `ic_launcher_round.png` across all Android mipmap densities (`mdpi` to `xxxhdpi`) and sign the APK using `keytool` (`V1/V2/V3` signatures).

---

## 🛠️ Tech Stack & Permissions

* **Language & SDK:** Pure Java + Native Android SDK (Zero external libraries, ultra-small APK size, MinSDK 26 / TargetSDK 34).
* **Permissions Used:**
  * `SYSTEM_ALERT_WINDOW` — To draw the touch-sensitive edge strip overlay.
  * `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` — To keep the edge overlay alive 24/7.
  * `RECEIVE_BOOT_COMPLETED` — To auto-start the service after a phone reboot.
  * `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — To prevent OEM battery savers from killing the service.

---

## 🐛 Bug Reports & Feedback

Facing an issue on your Android skin or have an idea for a new gesture (like double-tap to mute)?

1. Go to the **[Issues Tab](https://github.com/sanju-suthar/VolumeControl/issues)** and click **New Issue**.
2. Mention your **Phone Model**, **Android Version**, and describe the issue.
3. Pull Requests (PRs) are always welcome!

---

## 👤 Author

**Sanjay Suthar**
* GitHub: [@sanju-suthar](https://github.com/sanju-suthar)

⭐ *If Edge Volume saved your physical volume buttons, please give this repository a **Star**!*
