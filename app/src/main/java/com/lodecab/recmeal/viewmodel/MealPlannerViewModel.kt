package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import com.lodecab.recmeal.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    val mealPlans: Flow<List<MealPlanEntity>> = recipeRepository.getMealPlans()

    private val recipesByDate = mutableMapOf<String, MutableStateFlow<List<MealPlanRecipeEntity>>>()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun getRecipesForDate(date: String): StateFlow<List<MealPlanRecipeEntity>> {
        return recipesByDate.getOrPut(date) {
            MutableStateFlow<List<MealPlanRecipeEntity>>(emptyList()).also { stateFlow ->
                viewModelScope.launch {
                    recipeRepository.getRecipesForDate(date).collect { recipes ->
                        stateFlow.value = recipes
                        Log.d("MealPlannerViewModel", "Recipes for date $date: $recipes")
                    }
                }
            }
        }.asStateFlow()
    }

    fun addRecipeToMealPlan(date: String, recipeId: String, recipeTitle: String, recipeImage: String?, isCustom: Boolean, firestoreDocId: String?) {
        viewModelScope.launch {
            try {
                val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
                recipeRepository.insertMealPlan(mealPlan)

                val mealPlanRecipe = MealPlanRecipeEntity(
                    date = date,
                    rawId = recipeId,
                    title = recipeTitle,
                    recipeImage = recipeImage,
                    isCustom = isCustom,
                    firestoreDocId = firestoreDocId
                )
                recipeRepository.insertMealPlanRecipe(date, mealPlanRecipe)
            } catch (e: Exception) {
                Log.e("MealPlannerViewModel", "Error adding recipe to meal plan: ${e.message}", e)
                _error.value = "Error adding recipe to meal plan: ${e.message}"
            }
        }
    }

    fun removeRecipeFromMealPlan(date: String, recipeId: String) {
        viewModelScope.launch {
            recipeRepository.deleteMealPlanRecipe(date, recipeId) { success, errorMessage ->
                if (success) {
                    Log.d("MealPlannerViewModel", "Recipe deleted successfully from meal plan: $date, $recipeId")
                    // Since getRecipesForDate now uses a snapshot listener, the UI should update automatically
                    viewModelScope.launch {
                        val remainingRecipes = getRecipesForDate(date).firstOrNull() ?: emptyList()
                        if (remainingRecipes.isEmpty()) {
                            recipeRepository.deleteMealPlanAndRecipes(date) { delSuccess, delError ->
                                if (!delSuccess) {
                                    Log.e("MealPlannerViewModel", "Failed to delete meal plan: $delError")
                                    _error.value = delError
                                }
                            }
                        }
                    }
                } else {
                    Log.e("MealPlannerViewModel", "Failed to delete recipe: $errorMessage")
                    _error.value = errorMessage ?: "Failed to delete recipe from meal plan"
                }
            }
        }
    }
}