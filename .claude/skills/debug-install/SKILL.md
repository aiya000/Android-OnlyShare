---
name: debug-install
description: Install the built debug APK of this OnlyShare app on the connected device with adb. Use when the user asks to install or deploy the debug build; build it first with the `debug-build` skill if needed.
---

# debug-install

Install the debug APK on the device connected via adb.

## Environment

- The device is usually connected with **wireless adb**. The address (`<ip>:<port>`) changes between sessions and
  is not stored in the repository. It is shown on the device under
  設定 → 開発者向けオプション → ワイヤレスデバッグ
- adb does **not** reach the device from inside the Bash sandbox: the sandbox has its own network namespace,
  so `adb devices` there starts a second daemon that sees nothing and reports an empty list, even while the
  device is connected. Run every adb command with `dangerouslyDisableSandbox: true`
- The app reads the clipboard, which Android only allows for the focused app, so it can only be verified by
  actually launching it on a device

## Behavior

1. Make sure the APK exists and is fresh:

    ```
    app/build/outputs/apk/debug/app-debug.apk
    ```

    If it is missing or older than the latest source change, run the `debug-build` skill first

2. Check the device:

    ```bash
    adb devices
    ```

    - If no device is listed, run `adb connect <ip>:<port>` when the address is known from the conversation,
      otherwise ask the user to enable wireless debugging and tell you the address
3. Install:

    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```

4. Report `Success` or the adb error verbatim

## Notes

- To launch it:
  `adb shell am start -n io.github.aiya000.onlyshare.debug/io.github.aiya000.onlyshare.MainActivity`
  -- the application id carries the `.debug` suffix, the activity class does not
- The app draws nothing, so there is no UI of its own to look at. What it did shows up as the share sheet
  sitting on top, launched by this package:

    ```bash
    adb shell dumpsys activity activities | rg -i 'onlyshare|Chooser'
    ```

    A working run leaves `com.android.intentresolver.ChooserActivity` with
    `launchedFromPackage=io.github.aiya000.onlyshare.debug`, and `MainActivity` already finishing. Grepping
    logcat for the intent does **not** work -- nothing logs the `ACTION_SEND` that was handed to the chooser
- The share sheet closes by itself after a while. Re-launch the app right before taking a screenshot, rather
  than screenshotting a run from several minutes ago
- On a foldable the device has several displays, and `screencap` without `-d` picks an arbitrary one, which is
  usually the folded-away screen and comes out fully black. List the ids and pass the active one:

    ```bash
    adb shell dumpsys SurfaceFlinger --display-id
    adb exec-out screencap -p -d <display-id> > shot.png
    ```

- The clipboard **cannot** be set from the host. Since API 29 the clipboard service only answers the app that
  holds the window focus, so neither `adb shell am` nor `service call clipboard` can put anything on it. To
  test a case, ask the user to copy it on the device first -- text in any app for the text path, an image or a
  file in the gallery or a file manager for the `content://` stream path
- The debug build installs **alongside** the release build and never replaces it. It is the one with the
  orange launcher icon, labelled `OnlyShare debug`
- Never install while the user has asked to wait ("インストールは待って") -- build only
