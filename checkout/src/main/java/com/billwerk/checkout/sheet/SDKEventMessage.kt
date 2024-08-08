package com.billwerk.checkout.sheet

import androidx.annotation.Keep

// Based on ESessionState from Checkout Web
enum class SessionState {
    PAYMENT_METHOD_ALREADY_ADDED,
    INVOICE_ALREADY_PAID,
    INVOICE_PROCESSING,
    SUCCESS,
    SESSION_EXPIRED
}

// Based on ESDKEventType from Checkout Web
enum class SDKEventType {
    Accept,
    Error,
    Cancel,
    Close,
    Open,
    Init,
}

enum class SDKUserEventType {
    card_input_change,
}

// Based on ISDKEventReply from Checkout Web
@Keep
class EventReply(val isWebView: Boolean?, val isWebViewChanged: Boolean?, val userAgent: String?)

// Based on SDKMessage from Checkout Web
@Keep
class SDKMessage(
    val id: String,
    val invoice: String,
    val customer: String,
    val subscription: String,
    val payment_method: String,
    val error: String
)

/// Based on ISDKEventMessage from Checkout Web
@Keep
class SDKEventMessage(val event: SDKEventType, val sessionState: String?, val data: SDKMessage?)

@Keep
class SDKUserEventMessage(val event: SDKUserEventType)