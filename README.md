# RouteVPN

Android client for [AmneziaWG](https://docs.amnezia.org/documentation/amnezia-wg/) tunnels. Package ID: `io.routedns.vpn`.

> [!NOTE]
> Based on [amneziawg-android](https://github.com/amnezia-vpn/amneziawg-android) with additional features listed below.

## ✨ Additional features

- **Android 5.0+ support** — Minimum SDK lowered to 21 (Android 5.0 Lollipop).
- **Root mode** — Optional tunnel backend that uses root access to create TUN interfaces and configure routing via `iptables`/`ip route`, completely bypassing the Android VPN API. No VPN icon in the status bar, no VPN permission dialogs. All device traffic is routed through the tunnel. Can be enabled in settings. **Requires a rooted device** (SuperSU, Magisk tested).
- **Tasker plugin** — Integrates as a Tasker action plugin for automation. Select a tunnel and action (on/off/toggle) directly from Tasker.
- **Token-based intent authentication** — Replaced the `CONTROL_TUNNELS` Android permission with a simple token for intent API authentication. No need to declare permissions in the calling app's manifest — just pass the token as an intent extra. More compatible with `adb`, scripts, and automation tools.
- **Per-ABI APKs** — Separate APKs for each CPU architecture (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`). Smaller download size compared to a universal APK.
- **UI fixes:**
  - Fixed dark theme colors — buttons and accents were invisible on dark backgrounds due to incorrect `colorPrimary` for dark mode.
  - Fixed edge-to-edge layout overlap on Android 15+ — settings and other screens no longer render behind the status bar and toolbar.
  - All settings are now shown at once — removed the collapsible "Advanced" section which rendered incorrectly on older Android versions.
  - Added a "Log" action to tunnel error notifications — quickly jump to the log viewer for error details.

## 🤖 Automation

### 🔌 Tasker plugin

Requires **"Allow Tasker plugin"** to be enabled in settings.

The app registers as a [Tasker](https://tasker.joaoapps.com/) action plugin:

1. In Tasker: **Task → Add Action → Plugin → RouteVPN Tunnel Control**
2. Tap the pencil icon to configure
3. Select a tunnel and action (**Turn on** / **Turn off** / **Toggle**)
4. Tap **Save**

The plugin appears automatically in Tasker's plugin list once the app is installed. It also works with other automation apps that support the Locale plugin protocol (e.g. [MacroDroid](https://www.macrodroid.com/)).

### 📡 Intent API

Requires **"Allow remote intents"** to be enabled in settings.

External apps can control tunnels via broadcast intents. Authentication is done via a token that is generated when you enable "Allow remote intents". You can view, copy, or regenerate the token in the **"Remote control token"** settings entry.

| Action | Extras | Description |
|--------|--------|-------------|
| `io.routedns.vpn.action.SET_TUNNEL_UP` | `tunnel` (String), `token` (String) | Bring tunnel up |
| `io.routedns.vpn.action.SET_TUNNEL_DOWN` | `tunnel` (String), `token` (String) | Bring tunnel down |
| `io.routedns.vpn.action.REFRESH_TUNNEL_STATES` | — | Refresh all tunnel states (always available, no token needed) |

Example using `adb`:

```bash
adb shell am broadcast -a io.routedns.vpn.action.SET_TUNNEL_UP \
  -e tunnel "my-tunnel" \
  -e token "AbCdEf1234" \
  -n io.routedns.vpn/.model.TunnelManager\$IntentReceiver
```

## 🔨 Building

```
$ git clone --recurse-submodules https://github.com/zamibd/routevpn
$ cd routevpn
$ ./gradlew assembleRelease
```

macOS users may need [flock(1)](https://github.com/discoteq/flock).

## 🐛 Issues

Open issues in this repository for RouteVPN-specific bugs and feature requests.

## 🤝 Contributing

PRs are welcome — bug fixes, documentation improvements, refactoring.

## ⭐️ Alternatives

- [AmneziaWG](https://github.com/amnezia-vpn/amneziawg-android) (Android 7+) — upstream app.
- [WG Tunnel](https://github.com/wgtunnel/android) (Android 8+) — alternative WG/AWG client.
