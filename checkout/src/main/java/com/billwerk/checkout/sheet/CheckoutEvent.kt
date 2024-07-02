package com.billwerk.checkout

import android.webkit.JavascriptInterface
import com.billwerk.checkout.sheet.SDKEventMessage
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class CheckoutEvent {
    private object CheckoutEventBus {
        private val _events = MutableSharedFlow<SDKEventMessage>()
        val events = _events.asSharedFlow()

        suspend fun emitEvent(event: SDKEventMessage) {
            _events.emit(event)
        }
    }

    companion object {

        /** Broadcasts the events emitted from the Reepay checkout session  */
        val events get(): SharedFlow<SDKEventMessage> = CheckoutEventBus.events

        /** Emits the checkout session event */
        @JavascriptInterface
        fun postMessage(jsonMessage: String) {
            var sdkEventMessage: SDKEventMessage = Gson().fromJson(jsonMessage, SDKEventMessage::class.java)
            CoroutineScope(Dispatchers.Main).launch {
                CheckoutEventBus.emitEvent(sdkEventMessage)
            }
        }

    }
}