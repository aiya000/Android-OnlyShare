---
name: debug-build
description: Build the debug APK of this OnlyShare app with gradle. Use when the user asks for a debug build, or before installing a debug build with the `debug-install` skill.
---

# debug-build

Build the debug APK. The project has no product flavors, so there is a single debug variant.

## Environment

- gradle needs a JDK. There is no `java` on `PATH`; this machine keeps JDK 17 in mise:

    ```bash
    mise exec java@17.0.2 -- ./gradlew ...
    ```

- The SDK location comes from `local.properties` (`sdk.dir=$HOME/Android/Sdk`), which is not tracked by git.
  Recreate it if it is missing
- gradle writes to `~/.gradle`, which the Bash sandbox denies -- the wrapper fails with
  `gradle-8.9-bin.zip.lck (Read-only file system)`. Run the build with `dangerouslyDisableSandbox: true`
- Outside the sandbox `$TMPDIR` is empty. Always give log files an **absolute** path, and delete the log
  afterwards if it was written inside the repository

## Behavior

1. Run the build in the background, logging to an absolute path:

    ```bash
    mise exec java@17.0.2 -- ./gradlew :app:assembleDebug -q > <log> 2>&1; echo "EXIT=$?" >> <log>
    ```

    - The first build downloads Gradle 8.9 itself (about 130MB) and takes a few minutes; an incremental one
      takes seconds
    - Use `:app:compileDebugKotlin` instead when only a compile check is needed

2. When it finishes, check the log for `^e: `, `error:`, `FAILED` and the `EXIT=` line
3. Report the APK path:

    ```
    app/build/outputs/apk/debug/app-debug.apk
    ```

## Notes

- The debug variant's application id is `io.github.aiya000.onlyshare.debug` (`applicationIdSuffix`), so it
  installs side by side with the release build. `src/debug/res` overrides the launcher icon background with
  orange and the label with `OnlyShare debug`, which is how the two are told apart on the device
- Kotlin block comments **nest**, so a KDoc must never contain a wildcard MIME type written with a slash and
  a star -- it opens a nested comment and the compiler reports `Syntax error: Unclosed comment` at the end of
  the file, plus unresolved references for everything after it
- Do not install automatically. Installing is the `debug-install` skill, run it only when the user asks
