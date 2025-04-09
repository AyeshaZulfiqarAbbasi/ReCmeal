package com.lodecab.recmeal.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import com.lodecab.recmeal.data.RecipeDetails
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.RecipeSummary
import com.lodecab.recmeal.data.SpoonacularApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    private val apiService: SpoonacularApiService,
    private val recipeRepository: RecipeRepository,
    @Named("spoonacularApiKey") private val apiKey: String  // Inject the API key
) : ViewModel() {

    private val _recipeDetails = MutableStateFlow<RecipeDetails?>(null)
    val recipeDetails: StateFlow<RecipeDetails?> = _recipeDetails.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun fetchRecipeDetails(recipeId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getRecipeDetails(recipeId, apiKey)  // Use injected key
                _recipeDetails.value = response
                val favorite = recipeRepository.getFavorite(recipeId)
                _isFavorite.value = favorite != null
            } catch (e: Exception) {
                _error.value = "Failed to load recipe details: ${e.message}"
            }
        }
    }

    fun toggleFavorite(recipe: RecipeDetails) {
        viewModelScope.launch {
            if (_isFavorite.value) {
                recipeRepository.deleteFavorite(recipe.id)
                _isFavorite.value = false
            } else {
                val recipeSummary = RecipeSummary(
                    id = recipe.id,
                    title = recipe.title,
                    image = recipe.image,
                    usedIngredientCount = 0,
                    missedIngredientCount = 0
                )
                recipeRepository.insertFavorite(recipeSummary)
                _isFavorite.value = true
            }
        }
    }

    fun scheduleRecipe(date: String, recipe: RecipeDetails) {
        viewModelScope.launch {
            recipeRepository.insertMealPlanRecipe(
                MealPlanRecipeEntity(
                    date = date,
                    recipeId = recipe.id,
                    recipeTitle = recipe.title,
                    recipeImage = recipe.image
                )
            )
        }
    }
}