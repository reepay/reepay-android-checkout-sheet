package com.billwerk.checkout

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

enum class Event {
    ACCEPT, CANCEL
}

class CheckoutEvent {
    private object CheckoutEventBus {
        private val _events = MutableSharedFlow<String>()
        val events = _events.asSharedFlow()

        suspend fun emitEvent(event: String) {
            _events.emit(event)
        }
    }

    companion object {

        /** Broadcasts the events emitted from the Reepay checkout session  */
        val events get(): SharedFlow<String> = CheckoutEventBus.events

        /** Emits the checkout session event */
        fun emitEvent(event: Event) {
            CoroutineScope(Dispatchers.Main).launch {
                CheckoutEventBus.emitEvent(getEventType(event))
            }
        }

        private fun getEventType(event: Event): String {
            when (event) {
                Event.ACCEPT -> return "Reepay.Event.Accept"
                Event.CANCEL -> return "Reepay.Event.Cancel"
            }
        }
    }
}