package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class CustomRecipesViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val recipeRepository: RecipeRepository,
    private val networkUtils: NetworkUtils
) : ViewModel() {

    private val _customRecipes = MutableStateFlow<List<CustomRecipe>>(emptyList())
    val customRecipes: StateFlow<List<CustomRecipe>> = _customRecipes.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _customRecipe = MutableStateFlow<CustomRecipe?>(null)
    val customRecipe: Flow<CustomRecipe?> = _customRecipe.asStateFlow()

    init {
        fetchCustomRecipes()
    }

    fun getCustomRecipe(firestoreDocId: String): Flow<CustomRecipe?> {
        return flow {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User not authenticated")
            try {
                Log.d("CustomRecipeViewModel", "Fetching custom recipe for userId: $userId, firestoreDocId: $firestoreDocId")
                val recipe = recipeRepository.getCustomRecipe(firestoreDocId, userId)
                _customRecipe.value = recipe
                emit(recipe)
                Log.d("CustomRecipeViewModel", "Fetched custom recipe: ${recipe?.title ?: "null"}")
            } catch (e: Exception) {
                Log.e("CustomRecipeViewModel", "Error fetching custom recipe $firestoreDocId: ${e.message}", e)
                _customRecipe.value = null
                emit(null)
            }
        }
    }

    fun addRecipeToMealPlan(date: String, recipe: CustomRecipe) {
        viewModelScope.launch {
            try {
                val mealPlan = MealPlanEntity(date = date, name = "Meal Plan for $date")
                recipeRepository.insertMealPlan(mealPlan)

                val mealPlanRecipe = MealPlanRecipeEntity(
                    date = date,
                    rawId = recipe.id, // Change from 'id' to 'rawId'
                    title = recipe.title,
                    recipeImage = null,
                    isCustom = true,
                    firestoreDocId = recipe.id
                )
                Log.d("CustomRecipesViewModel", "Inserting meal plan recipe: $mealPlanRecipe")
                recipeRepository.insertMealPlanRecipe(date, mealPlanRecipe)
                Log.d("CustomRecipesViewModel", "Meal plan recipe inserted successfully")
            } catch (e: Exception) {
                _error.value = if (!networkUtils.isNetworkAvailable()) {
                    "You are offline. Recipe will sync when the network is available."
                } else {
                    "Error adding recipe to meal plan: ${e.message}"
                }
                Log.e("CustomRecipesViewModel", "Error adding recipe to meal plan: ${e.message}", e)
            }
        }
    }

    fun deleteCustomRecipe(recipeId: String) {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            try {
                val result = withTimeoutOrNull(10000L) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("custom_recipes")
                        .document(recipeId)
                        .delete()
                        .await()
                }
                if (result == null) {
                    throw Exception("Failed to delete custom recipe: Operation timed out. Please check your network connection.")
                }
                fetchCustomRecipes()
            } catch (e: Exception) {
                _error.value = "Error deleting custom recipe: ${e.message}"
                Log.e("CustomRecipesViewModel", "Error deleting custom recipe: ${e.message}", e)
            }
        }
    }

    private fun fetchCustomRecipes() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            try {
                val snapshot = withTimeoutOrNull(10000L) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("custom_recipes")
                        .get()
                        .await()
                }
                if (snapshot == null) {
                    _error.value = "Failed to fetch custom recipes: Operation timed out. Please check your network connection."
                    return@launch
                }
                val recipes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CustomRecipe::class.java)?.copy(id = doc.id)
                }
                _customRecipes.value = recipes
            } catch (e: Exception) {
                _error.value = "Error fetching custom recipes: ${e.message}"
                Log.e("CustomRecipesViewModel", "Error fetching custom recipes: ${e.message}", e)
            }
        }
    }
}