package com.lodecab.recmeal.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecipeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    private val favoritesFlow = MutableStateFlow<List<RecipeSummary>>(emptyList())
    private val mealPlansFlow = MutableStateFlow<List<MealPlanEntity>>(emptyList())
    private val recipesForDateFlows = mutableMapOf<String, MutableStateFlow<List<MealPlanRecipeEntity>>>()
    private val recipesForDateListeners = mutableMapOf<String, ListenerRegistration>()
    private var favoritesListener: ListenerRegistration? = null
    private var mealPlansListener: ListenerRegistration? = null

    init {
        setupAuthListener()
    }

    private fun setupAuthListener() {
        firebaseAuth.addAuthStateListener { auth ->
            val userId = auth.currentUser?.uid
            if (userId != null) {
                setupFavoritesListener(userId)
                setupMealPlansListener(userId)
            } else {
                clearListeners()
            }
        }
    }

    private fun setupFavoritesListener(userId: String) {
        favoritesListener?.remove()
        favoritesListener = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, _ ->
                val favorites = snapshot?.documents?.mapNotNull { it.toObject<RecipeSummary>() } ?: emptyList()
                favoritesFlow.value = favorites
            }
    }

    private fun setupMealPlansListener(userId: String) {
        mealPlansListener?.remove()
        mealPlansListener = firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .addSnapshotListener { snapshot, _ ->
                val mealPlans = snapshot?.documents?.mapNotNull { it.toObject<MealPlanEntity>() } ?: emptyList()
                mealPlansFlow.value = mealPlans
                for (mealPlan in mealPlans) {
                    getRecipesForDate(mealPlan.date)
                }
            }
    }

    private fun clearListeners() {
        favoritesListener?.remove()
        favoritesListener = null
        mealPlansListener?.remove()
        mealPlansListener = null
        recipesForDateListeners.values.forEach { it.remove() }
        recipesForDateListeners.clear()
        favoritesFlow.value = emptyList()
        mealPlansFlow.value = emptyList()
        recipesForDateFlows.clear()
    }

    // Favorites
    suspend fun insertFavorite(recipe: RecipeSummary) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipe.id.toString())
            .set(recipe)
            .await()
    }

    suspend fun deleteFavorite(recipeId: Int) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId.toString())
            .delete()
            .await()
    }

    suspend fun getFavorite(recipeId: Int): RecipeSummary? {
        val userId = firebaseAuth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId.toString())
            .get()
            .await()
        return snapshot.toObject<RecipeSummary>()
    }

    fun getAllFavorites(): Flow<List<RecipeSummary>> = favoritesFlow

    // Meal Plans
    suspend fun insertMealPlan(mealPlan: MealPlanEntity) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(mealPlan.date)
            .set(mealPlan)
            .await()
    }

    suspend fun insertMealPlanRecipe(mealPlanRecipe: MealPlanRecipeEntity) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(mealPlanRecipe.date)
            .collection("recipes")
            .document(mealPlanRecipe.recipeId.toString())
            .set(mealPlanRecipe)
            .await()
    }

    suspend fun deleteMealPlanRecipe(date: String, recipeId: Int) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .collection("recipes")
            .document(recipeId.toString())
            .delete()
            .await()
    }

    suspend fun deleteMealPlanAndRecipes(date: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val recipesRef = firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .collection("recipes")
        val recipesSnapshot = recipesRef.get().await()
        for (doc in recipesSnapshot.documents) {
            doc.reference.delete().await()
        }
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .delete()
            .await()
        recipesForDateFlows.remove(date)
        recipesForDateListeners.remove(date)?.remove()
    }

    fun getAllMealPlans(): Flow<List<MealPlanEntity>> = mealPlansFlow

    fun getRecipesForDate(date: String): Flow<List<MealPlanRecipeEntity>> {
        val userId = firebaseAuth.currentUser?.uid ?: return MutableStateFlow(emptyList())
        val recipesFlow = recipesForDateFlows.getOrPut(date) {
            MutableStateFlow(emptyList())
        }
        recipesForDateListeners[date]?.remove()  // Remove existing listener if any
        recipesForDateListeners[date] = firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .collection("recipes")
            .addSnapshotListener { snapshot, _ ->
                val recipes = snapshot?.documents?.mapNotNull { it.toObject<MealPlanRecipeEntity>() } ?: emptyList()
                recipesFlow.value = recipes
            }
        return recipesFlow
    }
}