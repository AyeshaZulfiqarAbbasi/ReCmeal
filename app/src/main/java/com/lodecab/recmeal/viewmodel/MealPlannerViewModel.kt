package com.lodecab.recmeal.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MealPlannerViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    val mealPlans: Flow<List<MealPlanEntity>> = recipeRepository.getAllMealPlans()

    fun getRecipesForDate(date: String): Flow<List<MealPlanRecipeEntity>> {
        return recipeRepository.getRecipesForDate(date)
    }

    fun addRecipeToMealPlan(date: String, recipeId: Int, recipeTitle: String, recipeImage: String?) {
        viewModelScope.launch {
            // Ensure the meal plan exists
            val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
            recipeRepository.insertMealPlan(mealPlan)

            // Add the recipe to the meal plan
            val mealPlanRecipe = MealPlanRecipeEntity(
                date = date,
                recipeId = recipeId,
                recipeTitle = recipeTitle,
                recipeImage = recipeImage
            )
            recipeRepository.insertMealPlanRecipe(mealPlanRecipe)
        }
    }

    fun removeRecipeFromMealPlan(date: String, recipeId: Int) {
        viewModelScope.launch {
            recipeRepository.deleteMealPlanRecipe(date, recipeId)
            // Check if there are any remaining recipes for the date
            val remainingRecipes = recipeRepository.getRecipesForDate(date).firstOrNull() ?: emptyList()
            if (remainingRecipes.isEmpty()) {
                recipeRepository.deleteMealPlanAndRecipes(date)
            }
        }
    }
}