package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.lodecab.recmeal.BuildConfig
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.Nutrition
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.SpoonacularApiService
import com.lodecab.recmeal.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class CustomRecipeViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val spoonacularApiService: SpoonacularApiService,
    private val networkUtils: NetworkUtils
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

    private val _customRecipe = MutableStateFlow<CustomRecipe?>(null)
    val customRecipe: StateFlow<CustomRecipe?> = _customRecipe.asStateFlow()

    fun fetchCustomRecipe(firestoreDocId: String) { // Should accept only firestoreDocId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")
                Log.d("CustomRecipeViewModel", "Fetching recipe for userId: $userId, firestoreDocId: $firestoreDocId")
                val recipe = recipeRepository.getCustomRecipe(firestoreDocId, userId)
                _customRecipe.value = recipe
                Log.d("CustomRecipeViewModel", "Fetched custom recipe: ${recipe?.title ?: "null"}")
            } catch (e: Exception) {
                Log.e("CustomRecipeViewModel", "Error fetching custom recipe $firestoreDocId: ${e.message}", e)
                _error.value = "Failed to load recipe: ${e.message}"
                _customRecipe.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Expose a combined flow for UI convenience (optional)
    fun observeRecipeState(): Flow<RecipeState> = combine(
        customRecipe,
        isLoading,
        error
    ) { recipe, loading, error ->
        RecipeState(recipe, loading, error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecipeState(null, false, null)
    )


// Data class to hold the combined state
data class RecipeState(
    val recipe: CustomRecipe?,
    val isLoading: Boolean,
    val error: String?,

)



    fun updateRecipeTitle(title: String) {
        _recipeTitle.value = title
    }

    fun addIngredient(ingredient: String) {
        if (ingredient.isNotBlank()) {
            val hasQuantity = ingredient.contains(Regex("\\d+"))
            val hasUnitOrName = ingredient.contains(Regex("\\w+\\s+\\w+"))
            if (hasQuantity && hasUnitOrName) {
                _ingredients.value += ingredient.trim()
            } else {
                _error.value = "Please include a quantity and unit (e.g., 1 cup water)"
            }
        }
    }

    fun addInstruction(instruction: String) {
        if (instruction.isNotBlank()) {
            _instructions.value += instruction.trim()
        }
    }

    fun analyzeNutrition() {
        if (_ingredients.value.isEmpty()) {
            _error.value = "Please add at least one ingredient to analyze nutrition."
            return
        }
        if (!networkUtils.isNetworkAvailable()) {
            _error.value = "Nutrition analysis requires an internet connection."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                Log.d("CustomRecipeViewModel", "Using API Key: ${BuildConfig.SPOONACULAR_API_KEY}")
                val ingredientList = _ingredients.value.joinToString("\n")
                Log.d("CustomRecipeViewModel", "Analyzing ingredients: $ingredientList")

                var totalCalories = 0.0
                var totalProtein = 0.0
                var totalFat = 0.0
                var totalCarbohydrates = 0.0

                // Step 1: Parse ingredients to get names, amounts, and units
                val parsedIngredients: List<com.lodecab.recmeal.data.ParsedIngredient> = spoonacularApiService.parseIngredients(
                    apiKey = BuildConfig.SPOONACULAR_API_KEY,
                    servings = 1,
                    ingredientList = ingredientList
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
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    _error.value = "Nutrition analysis failed: Invalid or expired API key. Please check your Spoonacular API key."
                } else {
                    _error.value = "Failed to analyze nutrition: HTTP ${e.code()}"
                }
                Log.e("CustomRecipeViewModel", "Nutrition analysis error: ${e.message}", e)
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

        _isLoading.value = true
        _error.value = null

        val nutrition = _nutritionInfo.value?.takeIf { it != Nutrition(0, 0, 0, 0) }
        val customRecipe = CustomRecipe(
            title = _recipeTitle.value,
            ingredients = _ingredients.value,
            instructions = _instructions.value,
            nutrition = nutrition
        )

        Log.d("CustomRecipeViewModel", "Attempting to save recipe: ${_recipeTitle.value}")
        Log.d("CustomRecipeViewModel", "Custom Recipe: $customRecipe")

        recipeRepository.insertCustomRecipe(customRecipe) { success, errorMessage ->
            if (success) {
                Log.d("CustomRecipeViewModel", "Recipe saved successfully")
                _recipeTitle.value = ""
                _ingredients.value = emptyList()
                _instructions.value = emptyList()
                _nutritionInfo.value = null
                _navigateToRecipeList.value = true
            } else {
                _error.value = errorMessage ?: "Unknown error occurred"
                Log.e("CustomRecipeViewModel", "Failed to save recipe: $errorMessage")
            }
            _isLoading.value = false
        }
    }
    fun debugCreateCollection(collectionName: String) {
        _isLoading.value = true
        recipeRepository.debugCreateCollection(collectionName) { success, errorMessage ->
            if (success) {
                _error.value = "Successfully created collection: $collectionName"
            } else {
                _error.value = errorMessage ?: "Failed to create collection: $collectionName"
            }
            _isLoading.value = false
        }
    }

    fun onNavigationHandled() {
        _navigateToRecipeList.value = false
    }
}