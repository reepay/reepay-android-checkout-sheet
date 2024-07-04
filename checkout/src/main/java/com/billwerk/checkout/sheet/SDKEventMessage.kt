package com.billwerk.checkout.sheet

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

// Based on ISDKEventReply from Checkout Web
class EventReply(val isWebView: Boolean?, val isWebViewChanged: Boolean?, val userAgent: String?) {}

// Based on SDKMessage from Checkout Web
class SDKMessage(
    val id: String,
    val invoice: String,
    val customer: String,
    val subscription: String,
    val payment_method: String,
    val error: String
)

/// Based on ISDKEventMessage from Checkout Web
class SDKEventMessage(val event: String, val sessionState: String?, val data: SDKMessage?) {

}