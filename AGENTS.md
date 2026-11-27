# AGENTS.md

Instructions for coding agents working in this repository.

## Android skills

Use the installed [Android Skills](https://github.com/android/skills) plugin when a task matches one of its skills. Do not copy those skills into this repository.

## What this is

Android library that shows a Frisbii checkout session in a Material bottom sheet. The sheet loads `https://checkout.frisbii.com` in a WebView. Card data is sent to Frisbii, not through the host app.

Published on JitPack as `com.github.reepay:reepay-android-checkout-sheet`. The sample app is [reepay-checkout-demo-app-android-kotlin](https://github.com/reepay/reepay-checkout-demo-app-android-kotlin).

Packages stay `com.billwerk.checkout` and `com.billwerk.checkout.sheet`. Do not rename them unless asked.

## Layout

The library module is `:checkout`. Public API is in `checkout/src/main/java/com/billwerk/checkout/sheet/`.

- `CheckoutSheet.open` shows the sheet. `presentCheckoutReturnUrl` resumes it after an app switch (for example Vipps). `dismiss` closes it.
- `CheckoutSheetConfig` and `SheetStyle` (`MEDIUM`, `LARGE`, `FULL_SCREEN`) control height, dismissal, header, and footer cancel.
- `SessionValidator` rejects an invalid session id before the sheet opens.
- Host apps collect `CheckoutEventPublisher.events` and `userEvents`. `SDKEventType` is Accept, Error, Cancel, Close, Open, Init.
- The WebView JavaScript bridge is `AndroidWebViewListener` on `CheckoutEventPublisher`. Keep `@Keep` on the Gson event types.

UI is `BottomSheetDialog` plus `checkout/src/main/res/layout/checkout_sheet.xml`, not Compose. The checkout host is `CheckoutSheetConstants.DOMAIN`.

## Build and publish

Read the toolchain from the Gradle files and keep it as declared there. Do not bump AGP, Kotlin, Gradle, SDK levels, the JVM target, or the Java toolchain unless the task is that upgrade.

- Plugin versions: root `build.gradle.kts` and the `buildscript` block in `checkout/build.gradle.kts`
- `minSdk`, `compileSdk`, the JVM target, and the Java toolchain: `checkout/build.gradle.kts`
- Gradle wrapper: `gradle/wrapper/gradle-wrapper.properties`
- Kotlin style: `kotlin.code.style` in `gradle.properties`

For an SDK release, bump `version` in `checkout/build.gradle.kts` and tag the release with that version. Update the demo app dependency only after `com.github.reepay:reepay-android-checkout-sheet` resolves from JitPack at the new tag.

JitPack publishes with `./gradlew :checkout:publishToMavenLocal` (`jitpack.yml`).

```bash
./gradlew :checkout:assembleRelease
```

There is no test source set yet. `consumer-rules.pro` is packaged with the AAR. Update it when the host app must keep additional classes.

`checkout/build/outputs/aar/checkout-release.aar` is already tracked. Do not edit that file by hand. Regenerate it only when the change is meant to update the checked-in artifact.

## Do not

- Commit `local.properties`, keystores, or new `build/` output.
- Change the checkout host, the JavaScript bridge name, or the event names unless the task asks for that. Host apps and checkout web depend on them.
