package com.lodecab.recmeal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.SpoonacularApiService
import com.lodecab.recmeal.data.IngredientSuggestion
import com.lodecab.recmeal.data.RecipeSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    private val apiService: SpoonacularApiService,
    @Named("spoonacularApiKey") private val apiKey: String  // Inject the API key
) : ViewModel() {

    private val _recipes = MutableStateFlow<List<RecipeSummary>>(emptyList())
    val recipes: StateFlow<List<RecipeSummary>> = _recipes.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _ingredientSuggestions = MutableStateFlow<List<IngredientSuggestion>>(emptyList())
    val ingredientSuggestions: StateFlow<List<IngredientSuggestion>> = _ingredientSuggestions.asStateFlow()

    fun searchRecipes(ingredients: String) {
        viewModelScope.launch {
            try {
                val response = apiService.findRecipesByIngredients(
                    ingredients = ingredients,
                    apiKey = apiKey  // Use injected key
                )
                _recipes.value = response
            } catch (e: Exception) {
                _error.value = "Failed to load recipes: ${e.message}"
            }
        }
    }

    fun fetchIngredientSuggestions(query: String) {
        viewModelScope.launch {
            try {
                val suggestions = apiService.autocompleteIngredients(
                    query = query,
                    apiKey = apiKey  // Use injected key
                )
                _ingredientSuggestions.value = suggestions
            } catch (e: Exception) {
                _error.value = "Failed to load ingredient suggestions: ${e.message}"
            }
        }
    }

    fun clearIngredientSuggestions() {
        _ingredientSuggestions.value = emptyList()
    }

    fun clearRecipes() {
        _recipes.value = emptyList()
    }

    fun clearError() {
        _error.value = null
    }
    private val _navigationError = MutableStateFlow<String?>(null)
    val navigationError: StateFlow<String?> = _navigationError.asStateFlow()

    fun setNavigationError(message: String) {
        _navigationError.value = message
    }

    fun clearNavigationError() {
        _navigationError.value = null
    }
}