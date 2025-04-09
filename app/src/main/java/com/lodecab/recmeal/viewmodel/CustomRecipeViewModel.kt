package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.BuildConfig
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.Nutrition
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.SpoonacularApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomRecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val spoonacularApiService: SpoonacularApiService
) : ViewModel() {
    private val _recipeTitle = MutableStateFlow("")
    val recipeTitle: StateFlow<String> = _recipeTitle.asStateFlow()

    private val _ingredients = MutableStateFlow<List<String>>(emptyList())
    val ingredients: StateFlow<List<String>> = _ingredients.asStateFlow()

    private val _instructions = MutableStateFlow<List<String>>(emptyList())
    val instructions: StateFlow<List<String>> = _instructions.asStateFlow()

    private val _nutritionInfo = MutableStateFlow<Nutrition?>(null)
    val nutritionInfo: StateFlow<Nutrition?> = _nutritionInfo.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _navigateToRecipeList = MutableStateFlow(false)
    val navigateToRecipeList: StateFlow<Boolean> = _navigateToRecipeList.asStateFlow()

    fun updateRecipeTitle(title: String) {
        _recipeTitle.value = title
    }

    fun addIngredient(ingredient: String) {
        if (ingredient.isNotBlank()) {
            val hasQuantity = ingredient.contains(Regex("\\d+"))
            val hasUnitOrName = ingredient.contains(Regex("\\w+\\s+\\w+"))
            if (hasQuantity && hasUnitOrName) {
                _ingredients.value = _ingredients.value + ingredient.trim()
            } else {
                _error.value = "Please include a quantity and unit (e.g., 1 cup water)"
            }
        }
    }

    fun addInstruction(instruction: String) {
        if (instruction.isNotBlank()) {
            _instructions.value = _instructions.value + instruction.trim()
        }
    }

    fun analyzeNutrition() {
        if (_ingredients.value.isEmpty()) {
            _error.value = "Please add at least one ingredient to analyze nutrition."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val ingredientList = _ingredients.value
                Log.d("CustomRecipeViewModel", "Analyzing ingredients: $ingredientList")

                var totalCalories = 0.0
                var totalProtein = 0.0
                var totalFat = 0.0
                var totalCarbohydrates = 0.0

                // Step 1: Parse ingredients to get names, amounts, and units
                val parsedIngredients = spoonacularApiService.parseIngredients(
                    apiKey = BuildConfig.SPOONACULAR_API_KEY,
                    ingredientList = ingredientList.joinToString("\n")
                )
                Log.d("CustomRecipeViewModel", "Parsed Ingredients: $parsedIngredients")

                // Step 2: For each parsed ingredient, fetch nutritional data
                for (parsedIngredient in parsedIngredients) {
                    try {
                        // Search for the ingredient to get its ID
                        val searchResponse = spoonacularApiService.searchIngredients(
                            apiKey = BuildConfig.SPOONACULAR_API_KEY,
                            query = parsedIngredient.name
                        )
                        val ingredientId = searchResponse.results.firstOrNull()?.id
                        if (ingredientId == null) {
                            Log.w("CustomRecipeViewModel", "Ingredient not found: ${parsedIngredient.name}")
                            continue
                        }

                        // Fetch nutritional info for the ingredient
                        val infoResponse = spoonacularApiService.getIngredientInfo(
                            id = ingredientId,
                            apiKey = BuildConfig.SPOONACULAR_API_KEY,
                            amount = parsedIngredient.amount,
                            unit = parsedIngredient.unit
                        )
                        Log.d("CustomRecipeViewModel", "Ingredient Info for ${parsedIngredient.name}: $infoResponse")

                        // Aggregate nutritional data
                        infoResponse.nutrition?.nutrients?.forEach { nutrient ->
                            val nutrientName = nutrient.name.trim().lowercase()
                            Log.d("CustomRecipeViewModel", "Nutrient: $nutrientName, Amount: ${nutrient.amount}, Unit: ${nutrient.unit}")
                            when (nutrientName) {
                                "calories", "energy", "kcal" -> totalCalories += nutrient.amount
                                "protein" -> totalProtein += nutrient.amount
                                "fat", "total fat", "lipid" -> totalFat += nutrient.amount
                                "carbohydrates", "carbs", "total carbohydrate", "carbohydrate" -> totalCarbohydrates += nutrient.amount
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CustomRecipeViewModel", "Error fetching nutrition for ${parsedIngredient.name}: ${e.message}", e)
                    }
                }

                Log.d("CustomRecipeViewModel", "Calculated Nutrition - Calories: $totalCalories, Protein: $totalProtein, Fat: $totalFat, Carbohydrates: $totalCarbohydrates")

                _nutritionInfo.value = Nutrition(
                    calories = totalCalories.toInt(),
                    protein = totalProtein.toInt(),
                    fat = totalFat.toInt(),
                    carbohydrates = totalCarbohydrates.toInt()
                )
            } catch (e: Exception) {
                _error.value = "Failed to analyze nutrition: ${e.message}"
                Log.e("CustomRecipeViewModel", "Nutrition analysis error: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveRecipe() {
        if (_recipeTitle.value.isBlank()) {
            _error.value = "Please enter a recipe title."
            return
        }
        if (_ingredients.value.isEmpty()) {
            _error.value = "Please add at least one ingredient."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val nutrition = _nutritionInfo.value?.takeIf { it != Nutrition(0, 0, 0, 0) }
                val customRecipe = CustomRecipe(
                    title = _recipeTitle.value,
                    ingredients = _ingredients.value,
                    instructions = _instructions.value,
                    nutrition = nutrition
                )
                recipeRepository.insertCustomRecipe(customRecipe)
                _recipeTitle.value = ""
                _ingredients.value = emptyList()
                _instructions.value = emptyList()
                _nutritionInfo.value = null
                _navigateToRecipeList.value = true
            } catch (e: Exception) {
                _error.value = "Failed to save recipe: ${e.message}"
                Log.e("CustomRecipeViewModel", "Failed to save recipe: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onNavigationHandled() {
        _navigateToRecipeList.value = false
    }
}