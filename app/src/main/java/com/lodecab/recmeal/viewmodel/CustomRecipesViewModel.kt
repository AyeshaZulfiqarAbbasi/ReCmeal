package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.MealPlanRecipeEntity
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.utils.NetworkUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val customRecipe: StateFlow<CustomRecipe?> = _customRecipe.asStateFlow()

    private var customRecipesListener: ListenerRegistration? = null

    init {
        // Set up the listener directly since settings are now configured in RecmealApplication
        setupCustomRecipesListener()
    }

    private fun setupCustomRecipesListener() {
        val userId = firebaseAuth.currentUser?.uid ?: return
        customRecipesListener?.remove()
        customRecipesListener = firestore.collection("users")
            .document(userId)
            .collection("custom_recipes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CustomRecipesViewModel", "Listen failed for custom recipes: ${e.message}", e)
                    _error.value = "Failed to listen to custom recipes: ${e.message}"
                    return@addSnapshotListener
                }
                val recipes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CustomRecipe::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                _customRecipes.value = recipes
                Log.d("CustomRecipesViewModel", "Updated custom recipes: $recipes")
            }
    }

    fun getCustomRecipe(firestoreDocId: String): Flow<CustomRecipe?> {
        return flow {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("User not authenticated")
            try {
                Log.d("CustomRecipeViewModel", "Fetching custom recipe for userId: $userId, firestoreDocId: $firestoreDocId")
                val recipe = recipeRepository.getCustomRecipe(firestoreDocId, userId)
                _customRecipe.value = recipe
                _error.value = null
                emit(recipe)
                Log.d("CustomRecipeViewModel", "Fetched custom recipe: ${recipe?.title ?: "null"}")
            } catch (e: Exception) {
                Log.e("CustomRecipeViewModel", "Error fetching custom recipe $firestoreDocId: ${e.message}", e)
                _customRecipe.value = null
                _error.value = "Failed to load recipe: ${e.message}"
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
                    rawId = recipe.id,
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
            val startTime = System.currentTimeMillis()
            Log.d("CustomRecipesViewModel", "Start deletion of recipe $recipeId at ${startTime}")

            try {
                // Step 1: Delete the custom recipe with a 30-second timeout
                Log.d("CustomRecipesViewModel", "Attempting to delete custom recipe $recipeId")
                val deleteResult = withTimeoutOrNull(30000L) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("custom_recipes")
                        .document(recipeId)
                        .delete()
                        .await()
                }
                if (deleteResult == null) {
                    throw Exception("Failed to delete custom recipe: Operation timed out after 30 seconds.")
                }
                Log.d("CustomRecipesViewModel", "Custom recipe $recipeId deleted successfully at ${System.currentTimeMillis()} (took ${System.currentTimeMillis() - startTime} ms)")

                // Step 2: Clean up meal plan recipes with a separate 30-second timeout
                Log.d("CustomRecipesViewModel", "Starting meal plan cleanup for recipe $recipeId")
                val mealPlansSnapshot = withTimeoutOrNull(30000L) {
                    firestore.collection("users")
                        .document(userId)
                        .collection("meal_plans")
                        .get()
                        .await()
                }
                if (mealPlansSnapshot != null) {
                    for (mealPlanDoc in mealPlansSnapshot.documents) {
                        val date = mealPlanDoc.id
                        Log.d("CustomRecipesViewModel", "Checking meal plan $date for recipe $recipeId")
                        val recipesSnapshot = withTimeoutOrNull(30000L) {
                            firestore.collection("users")
                                .document(userId)
                                .collection("meal_plans")
                                .document(date)
                                .collection("recipes")
                                .whereEqualTo("firestoreDocId", recipeId)
                                .get()
                                .await()
                        }
                        if (recipesSnapshot != null) {
                            for (recipeDoc in recipesSnapshot.documents) {
                                Log.d("CustomRecipesViewModel", "Deleting meal plan recipe ${recipeDoc.id}")
                                recipeDoc.reference.delete().await()
                            }
                        }
                    }
                    Log.d("CustomRecipesViewModel", "Meal plan cleanup completed for recipe $recipeId at ${System.currentTimeMillis()} (took ${System.currentTimeMillis() - startTime} ms)")
                } else {
                    Log.w("CustomRecipesViewModel", "Meal plans snapshot timed out or null for recipe $recipeId")
                }

                Log.d("CustomRecipesViewModel", "Total deletion process for recipe $recipeId completed at ${System.currentTimeMillis()} (took ${System.currentTimeMillis() - startTime} ms)")
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
                    _error.value = "Failed to fetch custom recipes: Operation timed out."
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

    override fun onCleared() {
        super.onCleared()
        customRecipesListener?.remove()
    }

    fun clearError() {
        _error.value = null
    }
}