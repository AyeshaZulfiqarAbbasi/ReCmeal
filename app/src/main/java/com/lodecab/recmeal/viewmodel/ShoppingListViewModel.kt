package com.lodecab.recmeal.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.Ingredient
import com.lodecab.recmeal.data.RecipeRepository
import com.lodecab.recmeal.data.SpoonacularApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository,
    private val apiService: SpoonacularApiService,
    private val firebaseAuth: FirebaseAuth, // Add this
    private val firestore: FirebaseFirestore ,// Add this
    @Named("spoonacularApiKey") private val apiKey: String
) : ViewModel() {
    private val _shoppingList = MutableStateFlow<List<Ingredient>>(emptyList())
    val shoppingList: StateFlow<List<Ingredient>> = _shoppingList.asStateFlow()

    init {
        loadShoppingList()
    }

    private fun loadShoppingList() {
        viewModelScope.launch {
            val mealPlans = recipeRepository.getMealPlans().firstOrNull() ?: emptyList()
            val allIngredients = mutableListOf<Ingredient>()

            for (mealPlan in mealPlans) {
                val recipes = recipeRepository.getRecipesForDate(mealPlan.date).firstOrNull() ?: emptyList()
                for (recipe in recipes) {
                    try {
                        if (recipe.isCustom && recipe.firestoreDocId != null) {
                            // Fetch custom recipe ingredients from Firestore
                            val userId = firebaseAuth.currentUser?.uid ?: continue
                            val snapshot = firestore.collection("users")
                                .document(userId)
                                .collection("custom_recipes")
                                .document(recipe.firestoreDocId)
                                .get()
                                .await()
                            val customRecipe = snapshot.toObject(CustomRecipe::class.java)
                            if (customRecipe != null) {
                                val ingredients = customRecipe.ingredients.map { ingredient ->
                                    Ingredient(name = ingredient, amount = 0.0, unit = "") // Adjust as needed
                                }
                                allIngredients.addAll(ingredients)
                            }
                        } else {
                            val recipeId = recipe.id.toIntOrNull() ?: continue
                            val recipeDetails = apiService.getRecipeDetails(recipeId, apiKey)
                            allIngredients.addAll(recipeDetails.ingredients)
                        }
                    } catch (e: Exception) {
                        Log.e("ShoppingListViewModel", "Error fetching recipe details: ${e.message}", e)
                    }
                }
            }
            _shoppingList.value = allIngredients.distinctBy { "${it.name}-${it.unit}" }
        }
    }
    fun removeItem(item: Ingredient) {
        _shoppingList.value = _shoppingList.value.filter { it != item }
    }

    fun refresh() {
        loadShoppingList()
    }
}