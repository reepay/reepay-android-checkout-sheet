package com.billwerk.checkout

import android.util.Log
import android.webkit.JavascriptInterface
import com.billwerk.checkout.sheet.SDKEventMessage
import com.billwerk.checkout.sheet.SDKEventType
import com.billwerk.checkout.sheet.SDKUserEventMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CheckoutEventPublisher {
    private object CheckoutEventBus {
        private val _events = MutableSharedFlow<SDKEventMessage>()
        private val _userEvents = MutableSharedFlow<SDKUserEventMessage>()
        val events = _events.asSharedFlow()
        val userEvents = _userEvents.asSharedFlow()

        suspend fun emitEvent(event: SDKEventMessage) {
            _events.emit(event)
        }

        suspend fun emitUserEvent(event: SDKUserEventMessage) {
            _userEvents.emit(event)
        }
    }

    companion object {

        /** Broadcasts the events emitted from the Reepay checkout session  */
        val events get(): SharedFlow<SDKEventMessage> = CheckoutEventBus.events
        val userEvents get(): SharedFlow<SDKUserEventMessage> = CheckoutEventBus.userEvents

        /** Emits the checkout session event */
        @JavascriptInterface
        fun postMessage(jsonMessage: String) {
            val message: SDKEventMessage = Gson().fromJson(jsonMessage, SDKEventMessage::class.java)
            emitEvent(message)
        }

        /** Emits the user event */
        @JavascriptInterface
        fun postUserEventMessage(jsonMessage: String) {
            val message: SDKUserEventMessage = Gson().fromJson(jsonMessage, SDKUserEventMessage::class.java)
            emitUserEvent(message)
        }

        fun postSimpleEvent(event: SDKEventType) {
            emitEvent(SDKEventMessage(event, null, null))
        }

        private fun emitEvent(message: SDKEventMessage) {
            CoroutineScope(Dispatchers.Main).launch {
                CheckoutEventBus.emitEvent(message)
            }
        }

        private fun emitUserEvent(message: SDKUserEventMessage) {
            CoroutineScope(Dispatchers.Main).launch {
                CheckoutEventBus.emitUserEvent(message)
            }
        }

        // TODO: add reply method, so checkout web can register it's a webview

    }
}