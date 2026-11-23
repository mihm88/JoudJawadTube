package com.joudjawadtube.player

import com.joudjawadtube.player.data.settings.WatchTimerManager_Antigravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchTimerManagerTest_Antigravity {

    @Test
    fun noTickingWhenLimitIsZeroOrPaused() {
        val manager = WatchTimerManager_Antigravity()
        manager.setLimitMinutes(0)
        manager.onPlaybackTick(deltaSeconds = 10L, isPlaying = true)
        assertEquals(0L, manager.elapsedSeconds.value)
        assertNull(manager.remainingSeconds.value)
        assertFalse(manager.isTimeUp.value)

        manager.setLimitMinutes(15)
        assertEquals(900L, manager.remainingSeconds.value)
        manager.onPlaybackTick(deltaSeconds = 30L, isPlaying = false)
        assertEquals(0L, manager.elapsedSeconds.value)
        assertEquals(900L, manager.remainingSeconds.value)
        assertFalse(manager.isTimeUp.value)
    }

    @Test
    fun tickingReachesLimitAndFiresTimeUp() {
        val manager = WatchTimerManager_Antigravity()
        manager.setLimitMinutes(1)

        manager.onPlaybackTick(deltaSeconds = 45L, isPlaying = true)
        assertEquals(45L, manager.elapsedSeconds.value)
        assertEquals(15L, manager.remainingSeconds.value)
        assertFalse(manager.isTimeUp.value)

        manager.onPlaybackTick(deltaSeconds = 15L, isPlaying = true)
        assertEquals(60L, manager.elapsedSeconds.value)
        assertEquals(0L, manager.remainingSeconds.value)
        assertTrue(manager.isTimeUp.value)

        // Further ticks once time is up do not advance elapsedSeconds
        manager.onPlaybackTick(deltaSeconds = 5L, isPlaying = true)
        assertEquals(60L, manager.elapsedSeconds.value)
    }

    @Test
    fun extendByMinutesClearsTimeUpAndAddsTime() {
        val manager = WatchTimerManager_Antigravity()
        manager.setLimitMinutes(1)
        manager.onPlaybackTick(deltaSeconds = 60L, isPlaying = true)
        assertTrue(manager.isTimeUp.value)

        manager.extendByMinutes(15)
        assertFalse(manager.isTimeUp.value)
        assertEquals(900L, manager.remainingSeconds.value)

        manager.onPlaybackTick(deltaSeconds = 300L, isPlaying = true)
        assertEquals(360L, manager.elapsedSeconds.value)
        assertEquals(600L, manager.remainingSeconds.value)
        assertFalse(manager.isTimeUp.value)
    }

    @Test
    fun resetForNewSessionResetsElapsedAndRestoresBaseLimit() {
        val manager = WatchTimerManager_Antigravity()
        manager.setLimitMinutes(10)
        manager.onPlaybackTick(deltaSeconds = 600L, isPlaying = true)
        assertTrue(manager.isTimeUp.value)

        manager.extendByMinutes(15)
        assertFalse(manager.isTimeUp.value)

        manager.resetForNewSession()
        assertEquals(0L, manager.elapsedSeconds.value)
        assertEquals(600L, manager.remainingSeconds.value)
        assertFalse(manager.isTimeUp.value)
    }
}
