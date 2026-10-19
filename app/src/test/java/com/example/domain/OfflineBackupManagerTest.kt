package com.example.domain

import com.example.domain.backup.OfflineBackupManager
import org.junit.Assert.*
import org.junit.Test

class OfflineBackupManagerTest {

    @Test
    fun `calculateSha256 produces valid 64-char hex digest`() {
        val input = "{\"courses\":[{\"name\":\"Math\"}]}"
        val hash = OfflineBackupManager.calculateSha256(input)
        assertNotNull(hash)
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("^[0-9a-f]{64}$")))
    }

    @Test
    fun `calculateSha256 is deterministic`() {
        val input = "{\"schedule\":\"test_deterministic_payload\"}"
        val hash1 = OfflineBackupManager.calculateSha256(input)
        val hash2 = OfflineBackupManager.calculateSha256(input)
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different inputs produce different hashes`() {
        val hash1 = OfflineBackupManager.calculateSha256("Course A")
        val hash2 = OfflineBackupManager.calculateSha256("Course B")
        assertNotEquals(hash1, hash2)
    }
}
