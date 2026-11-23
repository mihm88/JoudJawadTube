package com.joudjawadtube.player.data.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks active playback watch time against a configurable minute limit and
 * signals when the viewing limit has been reached.
 */
class WatchTimerManager_Antigravity {

    private var baseLimitMinutes: Int = 0
    private var totalLimitSeconds: Long = 0L

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _remainingSeconds = MutableStateFlow<Long?>(null)
    val remainingSeconds: StateFlow<Long?> = _remainingSeconds.asStateFlow()

    private val _isTimeUp = MutableStateFlow(false)
    val isTimeUp: StateFlow<Boolean> = _isTimeUp.asStateFlow()

    fun setLimitMinutes(limitMinutes: Int) {
        val sanitized = limitMinutes.coerceAtLeast(0)
        synchronized(this) {
            if (sanitized == baseLimitMinutes) {
                return
            }
            baseLimitMinutes = sanitized
            totalLimitSeconds = sanitized * 60L
            recomputeLocked()
        }
    }

    fun onPlaybackTick(deltaSeconds: Long, isPlaying: Boolean) {
        if (!isPlaying || deltaSeconds <= 0L) return
        synchronized(this) {
            if (totalLimitSeconds <= 0L || _isTimeUp.value) return
            val nextElapsed = _elapsedSeconds.value + deltaSeconds
            _elapsedSeconds.value = nextElapsed
            recomputeLocked()
        }
    }

    fun extendByMinutes(extraMinutes: Int) {
        if (extraMinutes <= 0) return
        val extraSeconds = extraMinutes * 60L
        synchronized(this) {
            val anchor = maxOf(totalLimitSeconds, _elapsedSeconds.value)
            totalLimitSeconds = anchor + extraSeconds
            recomputeLocked()
        }
    }

    fun resetForNewSession() {
        synchronized(this) {
            _elapsedSeconds.value = 0L
            totalLimitSeconds = baseLimitMinutes * 60L
            recomputeLocked()
        }
    }

    private fun recomputeLocked() {
        if (totalLimitSeconds <= 0L) {
            _remainingSeconds.value = null
            _isTimeUp.value = false
        } else {
            val remaining = (totalLimitSeconds - _elapsedSeconds.value).coerceAtLeast(0L)
            _remainingSeconds.value = remaining
            _isTimeUp.value = _elapsedSeconds.value >= totalLimitSeconds
        }
    }

    companion object {
        val shared: WatchTimerManager_Antigravity by lazy { WatchTimerManager_Antigravity() }
    }
}
