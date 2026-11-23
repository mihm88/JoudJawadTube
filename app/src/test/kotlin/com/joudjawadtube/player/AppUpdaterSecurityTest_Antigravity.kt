package com.joudjawadtube.player

import com.joudjawadtube.player.data.updater.AppUpdater_Antigravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdaterSecurityTest_Antigravity {

    @Test
    fun sanitizeApkFileName_preventsPathTraversal() {
        assertEquals("evil.apk", AppUpdater_Antigravity.sanitizeApkFileName("../../evil.apk"))
        assertEquals("evil.apk", AppUpdater_Antigravity.sanitizeApkFileName("..\\..\\evil.apk"))
        assertEquals("app.apk", AppUpdater_Antigravity.sanitizeApkFileName("sub/dir/app.apk"))
        assertEquals("app.apk", AppUpdater_Antigravity.sanitizeApkFileName("/abs/path/app.apk"))
    }

    @Test
    fun sanitizeApkFileName_handlesBlankAndDotSegments() {
        assertEquals("JoudJawadTube-update.apk", AppUpdater_Antigravity.sanitizeApkFileName(""))
        assertEquals("JoudJawadTube-update.apk", AppUpdater_Antigravity.sanitizeApkFileName("   "))
        assertEquals("JoudJawadTube-update.apk", AppUpdater_Antigravity.sanitizeApkFileName("."))
        assertEquals("JoudJawadTube-update.apk", AppUpdater_Antigravity.sanitizeApkFileName(".."))
        assertEquals("JoudJawadTube-update.apk", AppUpdater_Antigravity.sanitizeApkFileName("../../.."))
    }

    @Test
    fun sanitizeApkFileName_replacesInvalidCharsAndEnsuresApkExtension() {
        assertEquals("Joud_Jawad_v1.2.4_.apk", AppUpdater_Antigravity.sanitizeApkFileName("Joud Jawad(v1.2.4).apk"))
        assertEquals("release-build.apk", AppUpdater_Antigravity.sanitizeApkFileName("release-build"))
        assertEquals("JoudJawadTube-v1.2.4.apk", AppUpdater_Antigravity.sanitizeApkFileName("JoudJawadTube-v1.2.4.apk"))
        assertFalse(AppUpdater_Antigravity.sanitizeApkFileName("../bad/file name").contains("/"))
    }

    @Test
    fun sha256Hex_computesExpectedDigest() {
        // Standard NIST SHA-256 test vectors
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            AppUpdater_Antigravity.sha256Hex(ByteArray(0))
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AppUpdater_Antigravity.sha256Hex("abc".toByteArray(Charsets.UTF_8))
        )
        assertEquals(64, AppUpdater_Antigravity.sha256Hex("cert-bytes".toByteArray(Charsets.UTF_8)).length)
        assertNotEquals(
            AppUpdater_Antigravity.sha256Hex("cert-a".toByteArray(Charsets.UTF_8)),
            AppUpdater_Antigravity.sha256Hex("cert-b".toByteArray(Charsets.UTF_8))
        )
    }

    @Test
    fun compareVersions_ordersSemanticVersionsCorrectly() {
        assertTrue(AppUpdater_Antigravity.compareVersions("v1.2.4", "1.2.3") > 0)
        assertTrue(AppUpdater_Antigravity.compareVersions("V2.0.0", "v1.9.9") > 0)
        assertTrue(AppUpdater_Antigravity.compareVersions("1.2.3.1", "1.2.3") > 0)
        assertEquals(0, AppUpdater_Antigravity.compareVersions("v1.2.3", "1.2.3"))
        assertEquals(0, AppUpdater_Antigravity.compareVersions("v1.2.3-rc1+build42", "1.2.3"))
        assertTrue(AppUpdater_Antigravity.compareVersions("1.2.2", "v1.2.3") < 0)
    }
}
