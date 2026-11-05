# 📺 Samsung & Universal Smart TV Remote Control

A modern, high-performance Android application built with **Jetpack Compose (Material 3)** to control Samsung Smart TVs and Universal TV brands via **Wi-Fi (WebSocket)**, **Bluetooth HID**, and **Infrared (IR Blaster)**.

> **Made by San App Productions**

---

## ✨ Features

- 📡 **Wi-Fi Remote Control (WebSocket):**
  - Connects to Samsung Tizen OS Smart TVs (2016 to 2025+) over `wss://<IP>:8002` (fallback to `ws://<IP>:8001`).
  - Automatic token authentication and persistence in DataStore.
  - Custom TLS SSL TrustManager for self-signed TV certificates and `Origin` header handling.

- 📶 **Universal Bluetooth HID Remote:**
  - Emulates a physical Bluetooth Remote using Android 9.0+ (`BluetoothHidDevice` API 28+).
  - Transmits 8-byte Standard Keyboard Reports and 2-byte Consumer Control Reports.
  - Compatible with **LG WebOS, Sony Bravia, Android TV, Google TV, Amazon Fire TV, and Roku TV**.

- 🚨 **Infrared (IR Blaster) Control:**
  - Native `ConsumerIrManager` support for phones with built-in IR emitters (Xiaomi, Poco, Redmi, HTC, Samsung, LG).
  - Transmits Samsung NEC 32-bit IR codes (`0x0707` and `0x00E0` at 37.9 kHz) using LSB-first bit encoding.
  - Dual pulse modulation (microsecond durations + carrier cycle counts) for 100% hardware compatibility across OEM Android ROMs.

- ⚡ **Wake-on-LAN (WoL):**
  - Transmits UDP Magic Packets (`255.255.255.255:9`) to power on offline Samsung TVs.

- 🎨 **Ergonomic Jetpack Compose UI (Material 3):**
  - **Single-View Remote Interface:** Zero clutter layout with wide, pill-shaped action buttons (`MENU`, `GUIDE`, `TOUCH`, `EXIT`).
  - **End-to-End Square D-Pad:** 340dp flush grid layout with enlarged directional click zones and circular center OK button.
  - **Dedicated Volume Controls:** `VOL -`, `MUTE`, and `VOL +` buttons directly above the D-Pad with long-press repeat support.
  - **Hardware Volume Key Interception:** Control TV volume using your phone's physical Volume Up/Down rocker buttons—even inside the Touchpad overlay.
  - **OLED Pitch Black Touchpad Overlay:** Pure `#000000` gesture surface supporting swipes (Up/Down/Left/Right), tap-to-select, and bottom `BACK` / `HOME` control buttons.
  - **Pop-Up 123 Number Pad:** Full channel number keypad in a clean AlertDialog.
  - **Multi-TV Device Switcher:** Top App Bar dropdown menu to save and switch between unlimited persistent TV connections.

---

## 🛠️ Tech Stack & Architecture

- **Language:** 100% Idiomatic Kotlin
- **UI Framework:** Jetpack Compose (Material 3)
- **Navigation:** Navigation Compose
- **Asynchronous Flow:** Kotlin Coroutines & `StateFlow`
- **Persistence:** Jetpack DataStore Preferences (Unlimited saved devices, tokens, theme settings)
- **Network:** OkHttp 4 WebSocket & Custom SSL TrustManager
- **Bluetooth:** `BluetoothHidDevice` API (API 28+)
- **Infrared:** `ConsumerIrManager` API (API 19+)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Koala or newer
- JDK 11 / 17
- Android SDK 24+ (Android 7.0 minimum; Android 9.0+ required for Bluetooth HID)

### Installation & Build
```bash
git clone https://github.com/sanchitdcoder05/remote_controller.git
cd remote_controller
./gradlew assembleDebug
```

---

## 📜 License & Credits

Designed and developed by **San App Productions**.  
Distributed under the MIT License.
