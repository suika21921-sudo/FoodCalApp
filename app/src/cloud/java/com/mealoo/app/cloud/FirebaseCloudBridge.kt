package com.mealoo.app.cloud

import com.google.firebase.auth.FirebaseAuth

/** Firebase-backed identity bridge compiled only into the opt-in cloud flavor. */
object FirebaseCloudBridge {
    const val isEnabled: Boolean = true
    const val unavailableReason: String =
        "The legacy Firebase identity adapter requires a compatible signed-in Firebase user"

    fun currentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid
}
