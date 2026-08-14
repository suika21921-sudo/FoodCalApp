package com.mealoo.app.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mealoo.app.model.RecipeEntry
import com.mealoo.app.model.RecipeIngredient
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.math.BigDecimal

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String?,
    val kcal: Double,
    val totalWeightGram: Double,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val tags: String,
    val timeRequired: Int?,
    val ingredients: String,
    val steps: String,
    val ingredientsTextJson: String,
    val description: String?,
    val localImagePath: String?,
    val categoryJson: String,
    val cost: String?,
    val currency: String?,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

fun RecipeEntry.toEntity(isFavorite: Boolean = false): RecipeEntity {
    val json = Json { ignoreUnknownKeys = true }
    return RecipeEntity(
        id = this.id,
        name = this.name,
        imageUrl = this.imageUrl,
        kcal = this.kcalPerServing,
        totalWeightGram = this.totalWeightGram,
        protein = this.protein,
        carbs = this.carbs,
        fats = this.fats,
        tags = json.encodeToString(this.tagsV2),
        timeRequired = this.timeRequired,
        ingredients = json.encodeToString(this.ingredients),
        steps = json.encodeToString(this.steps),
        ingredientsTextJson = json.encodeToString(this.ingredientsText),
        description = this.description,
        localImagePath = this.localImagePath,
        categoryJson = json.encodeToString(this.category),
        cost = this.cost?.toString(),
        currency = this.currency,
        isFavorite = isFavorite
    )
}

fun RecipeEntity.toModel(): RecipeEntry {
    val json = Json { ignoreUnknownKeys = true }
    return com.mealoo.app.model.RecipeEntry(
        id = this.id,
        name = this.name,
        imageUrl = this.imageUrl,
        kcalPerServing = this.kcal,
        totalWeightGram = this.totalWeightGram,
        protein = this.protein,
        carbs = this.carbs,
        fats = this.fats,
        tagsV2 = try { json.decodeFromString(this.tags) } catch (e: Exception) { emptyMap() },
        timeRequired = this.timeRequired,
        ingredients = try { json.decodeFromString(this.ingredients) } catch (e: Exception) { emptyList() },
        steps = try { json.decodeFromString(this.steps) } catch (e: Exception) { emptyList() },
        ingredientsText = try { json.decodeFromString(this.ingredientsTextJson) } catch (e: Exception) { emptyList() },
        subRecipe = emptyList(),
        description = this.description,
        localImagePath = this.localImagePath,
        category = try { json.decodeFromString(this.categoryJson) } catch (e: Exception) { emptyList() },
        cost = this.cost?.let { BigDecimal(it) },
        currency = this.currency
    )
}