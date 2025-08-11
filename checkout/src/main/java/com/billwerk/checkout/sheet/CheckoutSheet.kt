package com.billwerk.checkout.sheet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.net.toUri
import com.billwerk.checkout.R

/**
 * Configuration class for checkout sheet.
 *
 * @property sessionId Billwerk+ checkout session id
 * @property sheetStyle Style of the checkout sheet. Sets the default height of the sheet. Default: [SheetStyle.MEDIUM].
 * @property dismissible If set to `true`, the sheet will render a close button and be dismissible by pressing outside the checkout sheet hit box.
 * @property hideHeader If set to `true`, the sheet will be rendered without the header
 * @property hideFooterCancel If set to `true`, the sheet will be rendered without cancel button in the footer
 * @property closeButtonIcon (Optional) Overrides the default icon for the close button. Argument is the id of the string. Image must be square
 * @property closeButtonText (Optional) Text shown next to the close button. Argument is the id of the string
 */
data class CheckoutSheetConfig(
    val sessionId: String,
    val sheetStyle: SheetStyle = SheetStyle.MEDIUM,
    val dismissible: Boolean = true,
    val hideHeader: Boolean = false,
    val hideFooterCancel: Boolean = false,
    var closeButtonIcon: Int? = null,
    var closeButtonText: Int? = null,
)

enum class SheetStyle {
    MEDIUM, LARGE, FULL_SCREEN
}

/**
 * The main class of the Checkout SDK used to create a sheet that displays a Reepay checkout session.
 *
 * @property context The context in which the sheet should be inflated in
 */
class CheckoutSheet(private val context: Context) {
    private val DEVICE_HEIGHT = Resources.getSystem().displayMetrics.heightPixels

    private var isDialogOpen = false

    private var bottomSheetDialog: BottomSheetDialog? = null

    /**
     * Opens the checkout sheet based on the provided configuration
     *
     *  @param config The configuration that the checkout sheet should be loaded with
     */
    fun open(config: CheckoutSheetConfig) {
        setupSheet(config)
        CheckoutEventPublisher.Companion.postSimpleEvent(SDKEventType.Open)
    }

    /**
     * Opens the checkout sheet with the specified return URL. The purpose of this method is to ensure that the checkout sheet correctly resumes after an app switch (for example, Vipps EPayment)
     *
     * @param config The configuration that the checkout sheet should be loaded with
     * @param returnUrl The return URL returned by the Billwerk+ Checkout Session
     */
    fun presentCheckoutReturnUrl(config: CheckoutSheetConfig, returnUrl: String) {
        setupSheet(config, returnUrl)
    }

    /**
     * Dismisses the checkout sheet dialog
     * @param bottomSheetDialog The bottom sheet dialog to close
     */
    fun dismiss() {
        isDialogOpen = false
        this.bottomSheetDialog?.dismiss()
    }

    private fun setupSheet(config: CheckoutSheetConfig, returnUrl: String? = null) {
        // Validate session ID
        if (!SessionValidator.Companion.validateToken(config.sessionId)) {
            throw IllegalArgumentException("Invalid session ID")
        }

        if (isDialogOpen) {
            return
        }

        isDialogOpen = true

        val view: View = LayoutInflater.from(context).inflate(R.layout.checkout_sheet, null)

        val loadingScreen = view.findViewById<LinearLayout>(R.id.rp_loadingScreen)
        val errorScreen = view.findViewById<LinearLayout>(R.id.rp_errorScreen)
        val bottomSheetDialog = BottomSheetDialog(context)

        this.bottomSheetDialog = bottomSheetDialog

        dismiss()

        // Configure sheet behavior
        bottomSheetDialog.apply {
            setCancelable(config.dismissible)

            val sheetHeight = getSheetHeight(config.sheetStyle)

            behavior.maxHeight = sheetHeight
            loadingScreen.minimumHeight = sheetHeight
            errorScreen.minimumHeight = sheetHeight

            behavior.peekHeight = DEVICE_HEIGHT
            behavior.isDraggable = false
            behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        dismiss()
                    }

                    if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                        loadingScreen.visibility = View.GONE
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    return
                }
            })
            bottomSheetDialog.setOnDismissListener {
                CheckoutEventPublisher.Companion.postSimpleEvent(
                    SDKEventType.Close
                )
            }
        }

        if (config.closeButtonText != null) {
            val closeButtonText = view.findViewById<TextView>(R.id.rp_button_close_text)
            closeButtonText.visibility = View.VISIBLE
            closeButtonText.setText(config.closeButtonText!!)
        }

        if (config.dismissible) {
            // Attach close button
            val closeBtn = view.findViewById<ImageButton>(R.id.rp_button_close)
            val closeBtnText = view.findViewById<TextView>(R.id.rp_button_close_text)

            if (config.closeButtonIcon != null) {
                closeBtn.setImageResource(config.closeButtonIcon!!)
            }

            attachClickListener(closeBtn)
            attachClickListener(closeBtnText)
        }

        // Display the checkout sheet and its contents
        bottomSheetDialog.setContentView(view)
        setupWebView(view, config, returnUrl)
        bottomSheetDialog.show()
    }

    private fun getSheetHeight(sheetStyle: SheetStyle): Int {
        if (sheetStyle == SheetStyle.MEDIUM) {
            return DEVICE_HEIGHT / 2
        }

        if (sheetStyle == SheetStyle.LARGE) {
            return (DEVICE_HEIGHT * 0.75).toInt()
        }

        return DEVICE_HEIGHT
    }

    private fun attachClickListener(view: View) {
        view.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                dismiss()
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(
        view: View,
        config: CheckoutSheetConfig,
        returnUrl: String? = null
    ) {
        val webView = view.findViewById<WebView>(R.id.rp_webView)
        val loadingScreen = view.findViewById<LinearLayout>(R.id.rp_loadingScreen)
        val errorScreen = view.findViewById<LinearLayout>(R.id.rp_errorScreen)

        loadingScreen.visibility = View.VISIBLE

        val queryParams = getQueryParams(config)
        val url = returnUrl ?: "${CheckoutSheetConstants.DOMAIN}/#/${config.sessionId}$queryParams"

        webView.addJavascriptInterface(CheckoutEventPublisher.Companion, "AndroidWebViewListener")
        webView.scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = getWebViewClient(webView, errorScreen, loadingScreen)

        // Configure WebView settings
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.safeBrowsingEnabled = true
        settings.userAgentString = getCustomUserAgent()

        // Enable Google Pay within WebView
        if (WebViewFeature.isFeatureSupported(WebViewFeature.PAYMENT_REQUEST)) {
            try {
                println("lol: ${getCustomUserAgent()}")
                WebSettingsCompat.setPaymentRequestEnabled(settings, true);
            } catch (exception: UnsupportedOperationException) {
                println("[CheckoutSheet] Google Pay not supported on Android WebView")
            }
        }

        injectDeviceFontSizePreference(webView)

        webView.loadUrl(url)
    }

    private fun getWebViewClient(
        webView: WebView,
        errorScreen: LinearLayout,
        loadingScreen: LinearLayout
    ): WebViewClient {
        val webViewClient = object : WebViewClient() {
            private var isPageError = false

            @Override
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isPageError = false
                webView.visibility = View.GONE
                errorScreen.visibility = View.GONE
                loadingScreen.visibility = View.VISIBLE
            }

            @Deprecated("Deprecated in API level 24")
            @Override
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrlOverrideLoading(view, url.toUri())
            }

            @Override
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return handleUrlOverrideLoading(view, request.url)
            }

            @Override
            override fun onPageFinished(view: WebView, url: String) {
                if (isPageError) {
                    webView.visibility = View.GONE
                    errorScreen.visibility = View.VISIBLE
                } else {
                    webView.visibility = View.VISIBLE
                }
            }

            @Override
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                isPageError = true
            }
        }
        return webViewClient
    }

    private fun getQueryParams(config: CheckoutSheetConfig): String {
        return buildString {
            if (config.hideHeader) append("?hideHeader=true")
            if (config.hideFooterCancel) {
                if (isNotEmpty()) append("&hideFooterCancel=true")
                else append("?hideFooterCancel=true")
            }
        }.takeIf { it.isNotEmpty() } ?: ""
    }

    private fun getCustomUserAgent(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val version = context.getString(R.string.library_version)
        val customUserAgent =
            "ReepayCheckoutSheet/${version} (${manufacturer} ${model}; Android version ${Build.VERSION.RELEASE}) AndroidSystemWebView"
        return customUserAgent
    }

    private fun handleUrlOverrideLoading(view: WebView, uri: Uri): Boolean {
        if (uri.toString().startsWith(CheckoutSheetConstants.DOMAIN)) {
            return false
        }

        val isCustomUrlScheme = !uri.scheme.toString().startsWith("http")

        if (isCustomUrlScheme) {
            val intent = Intent(Intent.ACTION_VIEW, uri)
            view.context.startActivity(intent)
            return true
        } else {
            view.loadUrl(uri.toString())
            return true
        }
    }

    private fun injectDeviceFontSizePreference(view: WebView) {
        val fontScale = context.resources.configuration.fontScale
        val scaleFactorEvent = """
            window.dispatchEvent(new CustomEvent('webview-font-size', {
              detail: { scaleFactor: $fontScale }
            }));""".trimIndent()

        view.evaluateJavascript(
            scaleFactorEvent, null
        )
    }
}

