package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import com.lodecab.recmeal.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    val mealPlans: Flow<List<MealPlanEntity>> = recipeRepository.getMealPlans()

    fun getRecipesForDate(date: String): Flow<List<MealPlanRecipeEntity>> {
        return recipeRepository.getRecipesForDate(date)
    }

    fun addRecipeToMealPlan(date: String, recipeId: Int, recipeTitle: String, recipeImage: String?, isCustom: Boolean, firestoreDocId: String?) {
        viewModelScope.launch {
            try {
                val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
                recipeRepository.insertMealPlan(mealPlan)

                val mealPlanRecipe = MealPlanRecipeEntity(
                    date = date,
                    id = recipeId,
                    title = recipeTitle,
                    recipeImage = recipeImage,
                    isCustom = isCustom,
                    firestoreDocId = firestoreDocId
                )
                recipeRepository.insertMealPlanRecipe(date, mealPlanRecipe)
            } catch (e: Exception) {
                Log.e("MealPlannerViewModel", "Error adding recipe to meal plan: ${e.message}", e)
            }
        }
    }

    fun removeRecipeFromMealPlan(date: String, recipeId: Int) {
        viewModelScope.launch {
            recipeRepository.deleteMealPlanRecipe(date, recipeId)
            val remainingRecipes = recipeRepository.getRecipesForDate(date).firstOrNull() ?: emptyList()
            if (remainingRecipes.isEmpty()) {
                recipeRepository.deleteMealPlanAndRecipes(date)
            }
        }
    }
}