package com.mealoo.app.data

/**
 * Standalone placeholder for the preserved community API.
 *
 * No Firebase classes are linked and no network request is attempted. The
 * community feature has no standalone UI entry point; callers receive an
 * explicit failure instead of a silent empty result.
 */
class CommunityRepository {
    val isAvailable: Boolean = false
    val unavailableReason: String = "Cloud community features are unavailable in the standalone build"

    suspend fun isRecipeShared(recipeId: String): Boolean {
        throw UnsupportedOperationException(unavailableReason)
    }

    suspend fun loadSharedRecipes(): List<RecipeEntryDTO> {
        throw UnsupportedOperationException(unavailableReason)
    }
}
