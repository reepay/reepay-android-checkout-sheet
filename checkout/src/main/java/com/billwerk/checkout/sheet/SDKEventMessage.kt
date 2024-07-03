package com.billwerk.checkout.sheet

enum class SessionState {
    PAYMENT_METHOD_ALREADY_ADDED,
    INVOICE_ALREADY_PAID,
    INVOICE_PROCESSING,
    SUCCESS,
    SESSION_EXPIRED
}

enum class SDKEventType {
    Accept,
    Error,
    Cancel,
    Close,
    Open,
    Init,
}

class SDKMessage(
    val id: String,
    val invoice: String,
    val customer: String,
    val subscription: String,
    val payment_method: String,
    val error: String
)

class SDKEventMessage(val event: String, val sessionState: String?, val data: SDKMessage?) {

}