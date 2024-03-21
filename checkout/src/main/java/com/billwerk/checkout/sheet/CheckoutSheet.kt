package com.billwerk.checkout

import android.content.Context
import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


data class CheckoutSheetConfig(
    val sessionId: String,
    val acceptURL: String?,
    val cancelURL: String?,
    val sheetStyle: SheetStyle = SheetStyle.MEDIUM,
    val dismissible: Boolean = true
)

enum class SheetStyle {
    MEDIUM, LARGE, FULL_SCREEN
}

class CheckoutSheet(private val context: Context) {

    private val deviceHeight = Resources.getSystem().displayMetrics.heightPixels
    private var isDialogOpen = false

    fun open(config: CheckoutSheetConfig) {
        // Validate session ID
        // TODO: valididate before opening
        if (!SessionValidator.validateToken(config.sessionId)) {
            throw IllegalArgumentException("Invalid session ID")
        }

        if (isDialogOpen) {
            return
        }

        isDialogOpen = true

        setupSheet(config)
    }

    private fun setupSheet(config: CheckoutSheetConfig) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.checkout_sheet, null)

        val bottomSheetDialog = BottomSheetDialog(context)

        dismiss(bottomSheetDialog)

        bottomSheetDialog.apply {
            setCancelable(config.dismissible)
            when (config.sheetStyle) {
                SheetStyle.MEDIUM -> behavior.maxHeight = deviceHeight / 2
                SheetStyle.LARGE -> behavior.maxHeight = (deviceHeight * 0.75).toInt()
                SheetStyle.FULL_SCREEN -> behavior.maxHeight = deviceHeight
                // TODO: make fullscreen even if webview is not tall enough
                // TODO: tilføj customizable height (percentage)
            }
            behavior.peekHeight = deviceHeight
            behavior.isDraggable = false
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        emitEvent(Event.CANCEL)
                        // TODO: perhaps add confirm dialog to warn
                        dismiss(bottomSheetDialog)
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })
        }

        if (config.dismissible) {
            val closeBtn = view.findViewById<ImageButton>(R.id.button_close)
            closeBtn.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    emitEvent(Event.CANCEL) // TODO: don't emit cancel event, but rather close event
                    dismiss(bottomSheetDialog)
                }
            }
        }


        // TODO: listen for event when sheet is dismissed?

        bottomSheetDialog.setContentView(view)
        setupWebView(view, bottomSheetDialog, config)

        bottomSheetDialog.show()
    }

    private fun setupWebView(
        view: View,
        bottomSheetDialog: BottomSheetDialog,
        config: CheckoutSheetConfig?
    ) {
        val webView = view.findViewById<WebView>(R.id.rp_webView)

        webView.apply {
            loadUrl("https://staging-checkout.reepay.com/#/${config?.sessionId}") // TODO: customize environment
            settings.javaScriptEnabled = true
            settings.safeBrowsingEnabled = true
            webViewClient = object : WebViewClient() {
                @Override
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url.toString()
                    when {
                        config?.acceptURL != null && url.contains(config.acceptURL) -> {
                            emitEvent(Event.ACCEPT)
                            bottomSheetDialog.behavior.maxHeight = deviceHeight
                        }

                        // TODO: if acceptUrl == null && url.contains(checkout web success page), notify payment has succeeded

                        config?.cancelURL != null && url.contains(config.cancelURL) -> {
                            emitEvent(Event.CANCEL)
                            bottomSheetDialog.behavior.maxHeight = deviceHeight
                        }

                        // TODO: if cancelUrl == null, notify payment has failed
                    }
                    return false
                }
            }
        }
    }

    private fun emitEvent(event: Event) {
        return CheckoutEvent.emitEvent(event)
    }

    private fun dismiss(bottomSheetDialog: BottomSheetDialog) {
        isDialogOpen = false
        bottomSheetDialog.dismiss()
    }
}

