package com.dynamicisland.media

import android.content.Context
import android.os.Build
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import com.dynamicisland.overlay.IslandEventBus
import com.dynamicisland.overlay.IslandState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PhoneStateMonitor(private val context: Context) {

    private val telephonyManager =
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    private val scope = CoroutineScope(Dispatchers.Main)
    private var callDurationJob: Job? = null
    private var callStartTime = 0L

    private var telephonyCallback: TelephonyCallback? = null

    init {
        registerCallback()
    }

    private fun registerCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    handleCallState(state)
                }
            }
            telephonyCallback = callback
            try {
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor, callback
                )
            } catch (e: SecurityException) {
                // READ_PHONE_STATE not granted
            }
        }
    }

    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                callDurationJob?.cancel()
                IslandEventBus.post(
                    IslandState.PhoneCall(
                        callerName = null,
                        callerNumber = "Incoming Call",
                        isIncoming = true
                    )
                )
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                callStartTime = System.currentTimeMillis()
                startCallDurationUpdates()
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                callDurationJob?.cancel()
                IslandEventBus.dismiss()
            }
        }
    }

    private fun startCallDurationUpdates() {
        callDurationJob?.cancel()
        callDurationJob = scope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - callStartTime
                val minutes = (elapsed / 1000) / 60
                val seconds = (elapsed / 1000) % 60
                IslandEventBus.post(
                    IslandState.PhoneCall(
                        callerName = null,
                        callerNumber = String.format("%d:%02d", minutes, seconds),
                        isIncoming = false
                    )
                )
                delay(1000)
            }
        }
    }

    fun release() {
        callDurationJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let {
                telephonyManager.unregisterTelephonyCallback(it)
            }
        }
    }
}
