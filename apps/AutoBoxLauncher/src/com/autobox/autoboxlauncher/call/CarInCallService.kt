package com.autobox.autoboxlauncher.call

import android.telecom.Call
import android.telecom.InCallService

class CarInCallService : InCallService() {

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallRepository.onCallStateChanged(call)
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            CallRepository.onCallStateChanged(call)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        call.registerCallback(callCallback)
        CallRepository.attachService(this)
        CallRepository.onCallAdded(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        CallRepository.onCallRemoved(call)
        if (calls.isEmpty()) CallRepository.detachService()
    }
}
