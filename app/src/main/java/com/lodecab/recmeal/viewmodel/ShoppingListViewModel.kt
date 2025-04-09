package com.lodecab.recmeal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.Ingredient
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.SpoonacularApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val apiService: SpoonacularApiService,
    @Named("spoonacularApiKey") private val apiKey: String
) : ViewModel() {

    private val _shoppingList = MutableStateFlow<List<Ingredient>>(emptyList())
    val shoppingList: StateFlow<List<Ingredient>> = _shoppingList.asStateFlow()

    init {
        loadShoppingList()
    }

    private fun loadShoppingList() {
        viewModelScope.launch {
            val mealPlans = recipeRepository.getAllMealPlans().firstOrNull() ?: emptyList()
            val allIngredients = mutableListOf<Ingredient>()

            for (mealPlan in mealPlans) {
                val recipes = recipeRepository.getRecipesForDate(mealPlan.date).firstOrNull() ?: emptyList()
                for (recipe in recipes) {
                    // Fetch full recipe details from Spoonacular API to get ingredients
                    try {
                        val recipeDetails = apiService.getRecipeDetails(recipe.recipeId, apiKey)
                        allIngredients.addAll(recipeDetails.ingredients)
                    } catch (e: Exception) {
                        // Log error or handle gracefully (e.g., show cached data if available later)
                    }
                }
            }
            _shoppingList.value = allIngredients.distinctBy { "${it.name}-${it.unit}" } // Avoid duplicates
        }
    }

    fun removeItem(item: Ingredient) {
        _shoppingList.value = _shoppingList.value.filter { it != item }
    }

    // Refresh the list if needed (e.g., after meal plan changes)
    fun refresh() {
        loadShoppingList()
    }
}