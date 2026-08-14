package com.mealoo.app.data

import android.content.Context
import com.mealoo.app.db.AppDatabase
import com.mealoo.app.db.RecipeEntity
import kotlinx.coroutines.flow.Flow

class RecipeRepository(private val context: Context) {

    private val dao = AppDatabase.getInstance(context).recipeDao()

    // ✅ 新增：取得「所有」食譜的 Flow (用於主列表)
    val allRecipes: Flow<List<RecipeEntity>> = dao.getAllFlow()

    suspend fun addRecipe(recipe: RecipeEntity) {
        dao.insert(recipe)
    }

    suspend fun removeRecipe(recipe: RecipeEntity) {
        dao.delete(recipe)
    }
}