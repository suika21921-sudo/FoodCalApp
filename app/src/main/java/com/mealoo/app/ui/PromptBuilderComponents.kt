package com.mealoo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mealoo.app.R
import com.mealoo.app.data.PromptKind
import com.mealoo.app.data.PromptVariable
import com.mealoo.app.data.SavedPrompt
import com.mealoo.app.data.UserPreference
import com.mealoo.app.model.InventoryItem
import com.mealoo.app.ui.design.MealooTextField
import com.mealoo.app.ui.design.NoteTextField
import com.mealoo.app.util.PromptContextBuilder
import com.mealoo.app.util.PromptInputRequirement

@Composable
fun PromptTypeSelector(
    prompts: List<SavedPrompt>,
    selected: SavedPrompt?,
    onSelect: (SavedPrompt) -> Unit,
    modifier: Modifier = Modifier
) {
    var kindFilter by rememberSaveable { mutableStateOf<PromptKind?>(null) }

    val kinds = remember(prompts) { prompts.map { it.kind }.distinct() }
    val filtered = remember(prompts, kindFilter) {
        if (kindFilter == null) prompts else prompts.filter { it.kind == kindFilter }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (kinds.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = kindFilter == null,
                        onClick = { kindFilter = null },
                        label = { Text(stringResource(R.string.prompt_type_all)) }
                    )
                }
                items(kinds) { kind ->
                    FilterChip(
                        selected = kindFilter == kind,
                        onClick = { kindFilter = kind },
                        label = { Text(kind.name) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { prompt ->
                FilterChip(
                    selected = selected?.id == prompt.id,
                    onClick = { onSelect(prompt) },
                    label = { Text(prompt.title) }
                )
            }
        }
    }
}

@Composable
fun ContextPreviewPanel(
    userPref: UserPreference?,
    template: SavedPrompt?,
    modifier: Modifier = Modifier
) {
    val needsContext = remember(template) {
        template?.variables?.any {
            it == PromptVariable.MACRO_PREF_BLOCK ||
                it == PromptVariable.LIFESTYLE_BLOCK ||
                it == PromptVariable.GOAL ||
                it == PromptVariable.ACTIVITY_LEVEL ||
                it == PromptVariable.DIET_TYPE ||
                it == PromptVariable.MEAL_RATIO_MAP ||
                it == PromptVariable.WEIGHT_KG ||
                it == PromptVariable.HEIGHT_CM ||
                it == PromptVariable.AGE ||
                it == PromptVariable.DAILY_KCAL_TARGET ||
                it == PromptVariable.TODAY_DATE
        } == true
    }
    if (!needsContext) return

    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.prompt_context_title), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = stringResource(R.string.prompt_context_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    if (userPref == null) {
                        Text(
                            text = stringResource(R.string.prompt_context_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        return@Column
                    }

                    val lifestyle = remember(userPref) { PromptContextBuilder.buildLifestyleBlock(userPref) }
                    val macro = remember(userPref) { PromptContextBuilder.buildMacroPreferenceBlock(userPref) }

                    Text("LIFESTYLE_BLOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(lifestyle, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("MACRO_PREF_BLOCK", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(macro, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DynamicInputBlock(
    requirements: Set<PromptInputRequirement>,
    userPref: UserPreference?,
    template: SavedPrompt?,
    storageItems: List<InventoryItem>,
    selectedInventoryIds: Set<String>,
    onSelectedInventoryIdsChange: (Set<String>) -> Unit,
    servings: Int,
    onServingsChange: (Int) -> Unit,
    extraNote: String,
    onExtraNoteChange: (String) -> Unit,
    servingsError: String?,
    inventoryError: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (requirements.contains(PromptInputRequirement.PreferenceContext)) {
            ContextPreviewPanel(
                userPref = userPref,
                template = template
            )
        }
        if (requirements.contains(PromptInputRequirement.Servings)) {
            ServingsRequirementInput(
                servings = servings,
                onServingsChange = onServingsChange,
                error = servingsError
            )
        }
        if (requirements.contains(PromptInputRequirement.ExtraNote)) {
            ExtraNoteRequirementInput(
                value = extraNote,
                onValueChange = onExtraNoteChange
            )
        }
        if (requirements.contains(PromptInputRequirement.InventoryBlock)) {
            InventoryBlockRequirementInput(
                storageItems = storageItems,
                selectedInventoryIds = selectedInventoryIds,
                onSelectedInventoryIdsChange = onSelectedInventoryIdsChange,
                error = inventoryError
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServingsRequirementInput(
    servings: Int,
    onServingsChange: (Int) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    val textValue = remember(servings) { if (servings <= 0) "" else servings.toString() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.prompt_servings_title), style = MaterialTheme.typography.labelMedium)
            MealooTextField(
                value = textValue,
                onValueChange = { raw ->
                    val next = raw.filter { it.isDigit() }.toIntOrNull() ?: 0
                    onServingsChange(next)
                },
                label = null,
                placeholder = stringResource(R.string.prompt_servings_placeholder),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Start
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onServingsChange((servings - 1).coerceAtLeast(0)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("-1") }
                TextButton(
                    onClick = { onServingsChange(servings + 1) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) { Text("+1") }
                Text(
                    text = stringResource(R.string.prompt_servings_current, servings),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            if (!error.isNullOrBlank()) {
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun ExtraNoteRequirementInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )  {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.prompt_extra_note_title), style = MaterialTheme.typography.labelMedium)
            NoteTextField(
                value = value,
                onValueChange = onValueChange,
                label = null,
                placeholder = stringResource(R.string.prompt_extra_note_placeholder),
                minHeight = 110.dp,
                maxHeight = 180.dp,
                showCounter = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.prompt_extra_note_empty_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InventoryBlockRequirementInput(
    storageItems: List<InventoryItem>,
    selectedInventoryIds: Set<String>,
    onSelectedInventoryIdsChange: (Set<String>) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    )  {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.prompt_inventory_title), style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = stringResource(
                            R.string.prompt_inventory_selection_count,
                            selectedInventoryIds.size,
                            storageItems.size
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = { onSelectedInventoryIdsChange(storageItems.map { it.id }.toSet()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text(stringResource(R.string.prompt_inventory_select_all)) }
                    TextButton(
                        onClick = { onSelectedInventoryIdsChange(emptySet()) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) { Text(stringResource(R.string.prompt_inventory_clear_selection)) }
                }
            }

            if (!error.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(8.dp))

            if (storageItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.prompt_inventory_no_items),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(storageItems, key = { it.id }) { item ->
                    val checked = selectedInventoryIds.contains(item.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectedInventoryIdsChange(
                                    if (checked) selectedInventoryIds - item.id else selectedInventoryIds + item.id
                                )
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { v ->
                                onSelectedInventoryIdsChange(
                                    if (v) selectedInventoryIds + item.id else selectedInventoryIds - item.id
                                )
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "${stripZero(item.quantity)} ${item.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
object OutputSchemaProvider {
    val recipeSchema = """
        === OUTPUT CONSTRAINT ===
        Please strictly output data that conforms to the following JSON format. Do not include any Markdown markers (such as ```json) or extra explanatory text:
        {
          "type": "RecipeEntry",
          "name": "Recipe Name",
          "description": "Short Description",
          "kcalPerServing": 0.0,
          "totalWeightGram": 0.0,
          "protein": 0.0,
          "carbs": 0.0,
          "fats": 0.0,
          "timeRequired": 30,
          "ingredientsText": ["Ingredient 1 100g", "Ingredient 2 50g"],
          "ingredients": [
            {
              "name": "Ingredient Name",
              "amount": 100.0,
              "source": "DATA",
              "nutrientsPer100g": {"kcal": 0.0, "protein": 0.0, "carbs": 0.0, "fats": 0.0},
              "matchedFdcId": null,
              "cost": null
            }
          ],
          "steps": ["Step 1...", "Step 2..."],
          "category": [],
          "tagsV2": {},
          "subRecipe": [],
          "imageUrl": null,
          "localImagePath": null,
          "cost": null,
          "currency": null
        }
    """.trimIndent()

    val mealPlanSchema = """
        === OUTPUT CONSTRAINT ===
        Please strictly output data that conforms to the following JSON format. Do not include any Markdown markers or extra explanatory text:
        {
          "type": "MealPlanSummary",
          "name": "Plan Name",
          "description": "Plan Description",
          "dayCount": 1,
          "entries": [
            {
              "mealId": "BREAKFAST",
              "name": "Meal Name",
              "kcal": 0.0,
              "protein": 0.0,
              "carbs": 0.0,
              "fats": 0.0
            }
          ]
        }
    """.trimIndent()

    val eatEntrySchema = """
        === OUTPUT CONSTRAINT ===
        Please strictly output data that conforms to the following JSON format. Do not include any Markdown markers or extra explanatory text:
        {
          "type": "EatEntry",
          "name": "Food Name",
          "kcal": 0.0,
          "grams": 0.0,
          "protein": 0.0,
          "carbs": 0.0,
          "fats": 0.0,
          "cholesterol": 0.0,
          "category": null,
          "date": "YYYY-MM-DD",
          "mealId": null,
          "sourcePlanId": null,
          "fdcId": null,
          "source": null,
          "cost": null,
          "currency": null
        }
    """.trimIndent()

    val drinkEntrySchema = """
        === OUTPUT CONSTRAINT ===
        Please strictly output data that conforms to the following JSON format. Do not include any Markdown markers or extra explanatory text:
        {
          "type": "DrinkEntry",
          "name": "Drink Name",
          "kcal": 0.0,
          "sugarGrams": 0.0,
          "caffeine": 0.0,
          "waterML": 0.0,
          "alcoholVolumeML": null,
          "alcoholPercent": null,
          "category": null,
          "date": "YYYY-MM-DD",
          "mealId": null,
          "sourcePlanId": null,
          "cost": null,
          "currency": null
        }
    """.trimIndent()
}

enum class OutputFormatConstraint(val label: String, val schemaTemplate: String) {
    RECIPE("Recipe", OutputSchemaProvider.recipeSchema),
    MEAL_PLAN("Meal Plan", OutputSchemaProvider.mealPlanSchema),
    EAT_ENTRY("Eat Entry", OutputSchemaProvider.eatEntrySchema),
    DRINK_ENTRY("Drink Entry", OutputSchemaProvider.drinkEntrySchema)
}