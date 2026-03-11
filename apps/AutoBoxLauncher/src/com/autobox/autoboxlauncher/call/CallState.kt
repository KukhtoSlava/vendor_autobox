package com.autobox.autoboxlauncher.call

sealed class CallState {
    object Idle : CallState()

    data class Incoming(
        val callerName: String?,
        val callerNumber: String?,
    ) : CallState()

    data class Active(
        val callerName: String?,
        val callerNumber: String?,
        val startTimeMs: Long = System.currentTimeMillis(),
    ) : CallState()
}
