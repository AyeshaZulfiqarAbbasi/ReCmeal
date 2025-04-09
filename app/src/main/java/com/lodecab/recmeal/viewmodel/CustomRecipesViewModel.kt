package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CustomRecipesViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _customRecipes = MutableStateFlow<List<CustomRecipe>>(emptyList())
    val customRecipes: StateFlow<List<CustomRecipe>> = _customRecipes.asStateFlow()

    init {
        fetchCustomRecipes()
    }

    fun addCustomRecipe(recipe: CustomRecipe) {
        viewModelScope.launch {
            try {
                recipeRepository.insertCustomRecipe(recipe)
                fetchCustomRecipes()
            } catch (e: Exception) {
                Log.e("CustomRecipesViewModel", "Error adding custom recipe: ${e.message}", e)
            }
        }
    }

    fun addRecipeToMealPlan(date: String, recipe: CustomRecipe) {
        viewModelScope.launch {
            try {
                recipeRepository.addRecipeToMealPlan(date, recipe)
            } catch (e: Exception) {
                Log.e("CustomRecipesViewModel", "Error adding recipe to meal plan: ${e.message}", e)
            }
        }
    }

    fun deleteCustomRecipe(recipeId: String) {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            try {
                firestore.collection("users")
                    .document(userId)
                    .collection("custom_recipes")
                    .document(recipeId)
                    .delete()
                    .await()
                fetchCustomRecipes()
            } catch (e: Exception) {
                Log.e("CustomRecipesViewModel", "Error deleting custom recipe: ${e.message}", e)
            }
        }
    }

    private fun fetchCustomRecipes() {
        viewModelScope.launch {
            val userId = firebaseAuth.currentUser?.uid ?: return@launch
            try {
                val snapshot = firestore.collection("users")
                    .document(userId)
                    .collection("custom_recipes")
                    .get()
                    .await()
                val recipes = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CustomRecipe::class.java)?.copy(id = doc.id)
                }
                _customRecipes.value = recipes
            } catch (e: Exception) {
                Log.e("CustomRecipesViewModel", "Error fetching custom recipes: ${e.message}", e)
            }
        }
    }
}