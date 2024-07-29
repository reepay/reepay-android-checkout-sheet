package com.billwerk.checkout

import android.webkit.JavascriptInterface
import androidx.annotation.Keep
import com.billwerk.checkout.sheet.SDKEventMessage
import com.billwerk.checkout.sheet.SDKEventType
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
        val events = _events.asSharedFlow()

        suspend fun emitEvent(event: SDKEventMessage) {
            _events.emit(event)
        }
    }

    @Keep
    companion object {

        /** Broadcasts the events emitted from the Reepay checkout session  */
        val events get(): SharedFlow<SDKEventMessage> = CheckoutEventBus.events

        /** Emits the checkout session event */
        @JavascriptInterface
        fun postMessage(jsonMessage: String) {
            var sdkEventMessage: SDKEventMessage =
                Gson().fromJson(jsonMessage, SDKEventMessage::class.java)
            emitEvent(sdkEventMessage)
        }

        fun postSimpleEvent(event: SDKEventType) {
            emitEvent(SDKEventMessage(event.toString(), null, null))
        }

        private fun emitEvent(message: SDKEventMessage) {
            CoroutineScope(Dispatchers.Main).launch {
                CheckoutEventBus.emitEvent(message)
            }
        }
    }
}