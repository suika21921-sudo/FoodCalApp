package com.mealoo.app.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await

/** Firestore community repository compiled only into the opt-in cloud flavor. */
class CommunityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val isAvailable: Boolean = true
    val unavailableReason: String? = null

    suspend fun isRecipeShared(recipeId: String): Boolean {
        return try {
            val docRef = Firebase.firestore
                .collection("community").document("recipes")
                .collection("shared").document(recipeId)
            docRef.get().await().exists()
        } catch (error: Exception) {
            Log.e("CheckShared", "Unable to check whether recipe is shared", error)
            false
        }
    }

    suspend fun loadSharedRecipes(): List<RecipeEntryDTO> {
        val snapshot = firestore.collection("community")
            .document("recipes")
            .collection("shared")
            .get()
            .await()

        return snapshot.documents.mapNotNull { document ->
            document.toObject(RecipeEntryDTO::class.java)
        }
    }
}
