package com.mealoo.app.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirebaseCloudBridgeTest {
    @Test
    fun standaloneBridgeNeverExposesFirebaseIdentity() {
        assertFalse(FirebaseCloudBridge.isEnabled)
        assertNull(FirebaseCloudBridge.currentUserId())
        assertTrue(FirebaseCloudBridge.unavailableReason.contains("standalone", ignoreCase = true))
        assertFalse(ExternalCloudAiHandoff.isAvailable)
    }
}
