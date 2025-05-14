package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lodecab.recmeal.BuildConfig
import com.lodecab.recmeal.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class RecipeDetailsViewModel @Inject constructor(
    private val apiService: SpoonacularApiService,
    private val recipeRepository: RecipeRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _recipeDetails = MutableStateFlow<RecipeDetails?>(null)
    val recipeDetails: StateFlow<RecipeDetails?> = _recipeDetails.asStateFlow()

    private val _customRecipe = MutableStateFlow<CustomRecipe?>(null)
    val customRecipe: StateFlow<CustomRecipe?> = _customRecipe.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    fun fetchRecipeDetails(recipeId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getRecipeDetails(recipeId, BuildConfig.SPOONACULAR_API_KEY)
                _recipeDetails.value = response
                checkIfFavorite(recipeId)
            } catch (e: Exception) {
                _error.value = "Failed to load recipe details: ${e.message}"
                Log.e("RecipeDetailsViewModel", "Error fetching recipe details: ${e.message}", e)
            }
        }
    }

    fun fetchCustomRecipe(firestoreDocId: String) {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("custom_recipes")
                    .document(firestoreDocId)
                    .get()
                    .await()
                val recipe = snapshot.toObject(CustomRecipe::class.java)?.copy(id = snapshot.id)
                _customRecipe.value = recipe
                if (recipe != null) {
                    checkIfFavorite(recipe.id.hashCode())
                }
            } catch (e: Exception) {
                _error.value = "Failed to load custom recipe: ${e.message}"
                Log.e("RecipeDetailsViewModel", "Error fetching custom recipe: ${e.message}", e)
            }
        }
    }

    private fun checkIfFavorite(recipeId: Int) {
        viewModelScope.launch {
            try {
                val favorite = recipeRepository.getFavorite(recipeId)
                _isFavorite.value = favorite != null
            } catch (e: Exception) {
                Log.e("RecipeDetailsViewModel", "Error checking favorite: ${e.message}", e)
            }
        }
    }

    fun toggleFavorite(recipe: Any) {
        viewModelScope.launch {
            try {
                when (recipe) {
                    is RecipeDetails -> {
                        val recipeSummary = RecipeSummary(
                            id = recipe.id,
                            title = recipe.title,
                            image = recipe.image,
                            usedIngredientCount = 0, // Fetch from API if needed
                            missedIngredientCount = 0 // Fetch from API if needed
                        )
                        if (_isFavorite.value) {
                            recipeRepository.deleteFavorite(recipe.id)
                            _isFavorite.value = false
                        } else {
                            recipeRepository.insertFavorite(recipeSummary)
                            _isFavorite.value = true
                        }
                    }
                    is CustomRecipe -> {
                        val recipeSummary = RecipeSummary(
                            id = recipe.id.hashCode(),
                            title = recipe.title,
                            image = null,
                            usedIngredientCount = 0,
                            missedIngredientCount = 0
                        )
                        if (_isFavorite.value) {
                            recipeRepository.deleteFavorite(recipeSummary.id)
                            _isFavorite.value = false
                        } else {
                            recipeRepository.insertFavorite(recipeSummary)
                            _isFavorite.value = true
                        }
                    }
                }
            } catch (e: Exception) {
                _error.value = "Failed to toggle favorite: ${e.message}"
                Log.e("RecipeDetailsViewModel", "Error toggling favorite: ${e.message}", e)
            }
        }
    }

    fun scheduleRecipe(date: String, recipe: RecipeDetails) {
        viewModelScope.launch {
            try {
                val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
                val mealPlanRecipe = MealPlanRecipeEntity(
                    date = date,
                    rawId = recipe.id.toString(),
                    title = recipe.title,
                    recipeImage = recipe.image,
                    isCustom = false,
                    firestoreDocId = null
                )
                recipeRepository.insertMealPlan(mealPlan)
                recipeRepository.insertMealPlanRecipe(date, mealPlanRecipe)
            } catch (e: Exception) {
                _error.value = "Failed to schedule recipe: ${e.message}"
                Log.e("RecipeDetailsViewModel", "Error scheduling recipe: ${e.message}", e)
            }
        }
    }

    fun scheduleCustomRecipe(date: String, recipe: CustomRecipe) {
        viewModelScope.launch {
            try {
                val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
                val mealPlanRecipe = MealPlanRecipeEntity(
                    date = date,
                    rawId = recipe.id,
                    title = recipe.title,
                    recipeImage = null,
                    isCustom = true,
                    firestoreDocId = recipe.id
                )
                recipeRepository.insertMealPlan(mealPlan)
                recipeRepository.insertMealPlanRecipe(date, mealPlanRecipe)
            } catch (e: Exception) {
                _error.value = "Failed to schedule custom recipe: ${e.message}"
                Log.e("RecipeDetailsViewModel", "Error scheduling custom recipe: ${e.message}", e)
            }
        }
    }
}