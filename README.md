# ReepayCheckoutSheet Android SDK Module

CheckoutSheet is a prebuilt UI that contains the steps to pay in Reepay Checkout window - collecting payment details and confirming the payment. The checkout is placed into a sheet or a fullscreen cover that displays on top of your app's UI.

See official [documentation](https://optimize-docs.billwerk.com/docs/checkout-sdk-for-android)

## Table of contents

<!--ts-->

- [Features](#features)
- [Requirements](#requirements)
- [Getting started](#getting-started)
  - [Example](#example)

<!--te-->

## Features

- **Payment security**: We are [PCI compliant](https://docs.reepay.com/docs/pci-certified), which makes it simple for you to collect sensitive data such as credit card numbers. This means the sensitive data is sent directly to Reepay instead of passing through your server.

- **SCA-ready**: The SDK automatically performs native 3D Secure authentication to comply with [Strong Customer Authentication](https://stripe.com/docs/strong-customer-authentication) regulation in Europe.

## Requirements

The ReepayCheckoutSheet module is compatible with apps targeting Android 8.0 (API level 26) or above.

## Getting started

### Install from Jitpack

Link to Jitpack repository can be found [here](https://jitpack.io/#reepay/reepay-android-checkout-sheet)

**Step 1.**
Add Jitpack to root `settings.gradle` file

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**Step 2.**
Add dependency to `build.gradle.kts`. Replace `TAG` with the desired version number

```gradle
dependencies {
    ...
    implementation("com.github.reepay:reepay-android-checkout-sheet:TAG")
}
```

### Install locally

**Step 1.**
Download this repository

**Step 2.**
Register this module in the root `settings.gradle.kts` file:

```gradle
include(":checkout")

...

project(":checkout").projectDir =
    file("/PATH/TO/SDK/reepay-android-checkout-sheet/checkout")
```

**Step 3.**
Add dependency in `build.gradle.kts`

```gradle
dependencies {
    ...
    implementation(project(":checkout"))
}
```

### Configuration

#### Basic setup

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    ...

    // Instantiate checkout sheet
    val checkoutSheet = CheckoutSheet(this)

    // Create configuration
    val config = CheckoutSheetConfig(
        sessionId = "", // Session id from Reepay checkout
        acceptURL = "", // Required for session status to work correctly
        cancelURL = "", // Required for session status to work correctly
        sheetStyle = SheetStyle.LARGE, // Sets the height of the sheets
        dismissible = true // If enabled, an X button will appear in the top-left corner of the sheet
    )

    // Open checkoutSheet (for example, inside a setOnClickListener)
    checkoutSheet.open(config)
}

```

#### Listen for events

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {

    // Set up checkout sheet as shown above

    ...

    listenForEvents();
}


private fun listenForEvents() {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            CheckoutEvent.events.collect { eventType ->
                // Log the event here
            }
        }
    }
}
```

### Example

- [Reepay Checkout demo app](https://github.com/reepay/reepay-checkout-demo-app-android-kotlin)
  - This demo app demonstrates how to build a checkout flow using CheckoutSheet, an embeddable component that supports card payments with a single integration.
