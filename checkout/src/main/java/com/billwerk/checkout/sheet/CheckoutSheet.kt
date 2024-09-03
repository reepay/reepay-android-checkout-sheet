package com.billwerk.checkout

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.billwerk.checkout.sheet.CheckoutSheetConstants
import com.billwerk.checkout.sheet.SDKEventType
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


/**
 * Configuration class for checkout sheet.
 *
 * @property sessionId Billwerk+ checkout session id
 * @property sheetStyle Style of the checkout sheet. Sets the default height of the sheet. Default: [SheetStyle.MEDIUM].
 * @property dismissible If set to `true`, the sheet will render a close button and be dismissible by pressing outside the checkout sheet hit box.
 * @property hideHeader If set to `true`, the sheet will be rendered without the header
 * @property closeButtonIcon (Optional) Overrides the default icon for the close button. Argument is the id of the string. Image must be square
 * @property closeButtonText (Optional) Text shown next to the close button. Argument is the id of the string
 */
data class CheckoutSheetConfig(
    val sessionId: String,
    val sheetStyle: SheetStyle = SheetStyle.MEDIUM,
    val dismissible: Boolean = true,
    val hideHeader: Boolean = false,
    var closeButtonIcon: Int? = null,
    var closeButtonText: Int? = null
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
        // Validate session ID
        if (!SessionValidator.validateToken(config.sessionId)) {
            throw IllegalArgumentException("Invalid session ID")
        }

        if (isDialogOpen) {
            return
        }

        isDialogOpen = true

        setupSheet(config)
        CheckoutEventPublisher.postSimpleEvent(SDKEventType.Open)
    }

    /**
     * Dismisses the checkout sheet dialog
     * @param bottomSheetDialog The bottom sheet dialog to close
     */
    fun dismiss() {
        isDialogOpen = false
        this.bottomSheetDialog?.dismiss()
    }

    private fun setupSheet(config: CheckoutSheetConfig) {
        val view: View = LayoutInflater.from(context).inflate(R.layout.checkout_sheet, null)

        val loadingScreen = view.findViewById<LinearLayout>(R.id.rp_loadingScreen)
        val errorScreen = view.findViewById<LinearLayout>(R.id.rp_errorScreen)
        val bottomSheetDialog = BottomSheetDialog(context)

        this.bottomSheetDialog = bottomSheetDialog

        dismiss()

        // Configure sheet behavior
        bottomSheetDialog.apply {
            setCancelable(config.dismissible)
            when (config.sheetStyle) {
                SheetStyle.MEDIUM -> {
                    val height = DEVICE_HEIGHT / 2
                    behavior.maxHeight = height
                    loadingScreen.minimumHeight = height
                    errorScreen.minimumHeight = height
                }

                SheetStyle.LARGE -> {
                    val height = (DEVICE_HEIGHT * 0.75).toInt()
                    behavior.maxHeight = height
                    loadingScreen.minimumHeight = height
                    errorScreen.minimumHeight = height
                }

                SheetStyle.FULL_SCREEN -> {
                    behavior.maxHeight = DEVICE_HEIGHT
                    loadingScreen.minimumHeight = DEVICE_HEIGHT
                    errorScreen.minimumHeight = DEVICE_HEIGHT
                }
            }
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
                CheckoutEventPublisher.postSimpleEvent(
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
        setupWebView(view, config)
        bottomSheetDialog.show()
    }

    private fun attachClickListener(view: View) {
        view.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                dismiss()
            }
        }
    }

    private fun setupWebView(
        view: View,
        config: CheckoutSheetConfig
    ) {
        val webView = view.findViewById<WebView>(R.id.rp_webView)
        webView.addJavascriptInterface(CheckoutEventPublisher, "AndroidWebViewListener")
        val loadingScreen = view.findViewById<LinearLayout>(R.id.rp_loadingScreen)
        val errorScreen = view.findViewById<LinearLayout>(R.id.rp_errorScreen)


        loadingScreen.visibility = View.VISIBLE

        webView.apply {

            val queryparams = if (config.hideHeader) "?hideHeader=true" else ""
            var isPageError = false

            loadUrl("${CheckoutSheetConstants.DOMAIN}/#/${config.sessionId}${queryparams}")
            settings.javaScriptEnabled = true
            settings.safeBrowsingEnabled = true
            webViewClient = object : WebViewClient() {
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
                    return handleUrlOverrideLoading(view, url)
                }

                @Override
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean {
                    return handleUrlOverrideLoading(view, request.url.toString())
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
        }

    }

    private fun handleUrlOverrideLoading(view: WebView, url: String): Boolean {

        if (url.startsWith(CheckoutSheetConstants.DOMAIN)) {
            return false
        }

        if (url.startsWith(CheckoutSheetConstants.MOBILEPAY_URL_TEST)) {
            view.loadUrl(url)
            return true
        }

        if (url.startsWith(CheckoutSheetConstants.VIPPS_URL_TEST)) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            view.context.startActivity(intent)
            return true
        }


        return false
    }
}

