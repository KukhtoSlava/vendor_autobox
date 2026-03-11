package com.autobox.autoboxlauncher.call

import android.telecom.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object CallRepository {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var activeCall: Call? = null
    private var service: CarInCallService? = null

    // Called by CarInCallService on bind/unbind
    fun attachService(s: CarInCallService) { service = s }
    fun detachService() { service = null }

    fun onCallAdded(call: Call) {
        activeCall = call
        applyState(call)
    }

    fun onCallRemoved(call: Call) {
        if (activeCall == call) {
            activeCall = null
            _callState.value = CallState.Idle
        }
    }

    fun onCallStateChanged(call: Call) {
        if (activeCall == call) applyState(call)
    }

    fun answer() {
        activeCall?.answer(0)
    }

    fun decline() {
        activeCall?.reject(false, null)
    }

    fun hangUp() {
        activeCall?.disconnect()
    }

    fun setMuted(muted: Boolean) {
        service?.setMuted(muted)
    }

    fun setSpeakerOn(on: Boolean) {
        service?.setAudioRoute(
            if (on) android.telecom.CallAudioState.ROUTE_SPEAKER
            else android.telecom.CallAudioState.ROUTE_EARPIECE
        )
    }

    private fun applyState(call: Call) {
        val name = call.details.callerDisplayName
            ?.toString()?.takeIf { it.isNotBlank() }
        val number = call.details.handle?.schemeSpecificPart

        _callState.value = when (call.state) {
            Call.STATE_RINGING -> CallState.Incoming(
                callerName = name,
                callerNumber = number,
            )
            Call.STATE_ACTIVE,
            Call.STATE_DIALING,
            Call.STATE_CONNECTING -> {
                val prev = _callState.value
                val start = if (prev is CallState.Active) prev.startTimeMs
                            else System.currentTimeMillis()
                CallState.Active(callerName = name, callerNumber = number, startTimeMs = start)
            }
            else -> CallState.Idle
        }
    }
}
