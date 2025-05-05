package com.lodecab.recmeal.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val customRecipesFlow = MutableStateFlow<List<CustomRecipe>>(emptyList())
    private val recipesForDateFlows = mutableMapOf<String, MutableStateFlow<List<MealPlanRecipeEntity>>>()
    private val recipesForDateListeners = mutableMapOf<String, ListenerRegistration>()
    private var favoritesListener: ListenerRegistration? = null
    private var mealPlansListener: ListenerRegistration? = null
    private var customRecipesListener: ListenerRegistration? = null

    init {
        setupAuthListener()
    }

    private fun setupAuthListener() {
        firebaseAuth.addAuthStateListener { auth ->
            val userId = auth.currentUser?.uid
            if (userId != null) {
                Log.d("RecipeRepository", "User authenticated: $userId")
                setupFavoritesListener(userId)
                setupMealPlansListener(userId)
                setupCustomRecipesListener(userId)
            } else {
                Log.d("RecipeRepository", "User not authenticated")
                clearListeners()
            }
        }
    }

    private fun setupFavoritesListener(userId: String) {
        favoritesListener?.remove()
        favoritesListener = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RecipeRepository", "Listen failed for favorites: ${e.message}", e)
                    return@addSnapshotListener
                }
                val favorites = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(RecipeSummary::class.java)?.copy(id = doc.id.toInt())
                } ?: emptyList()
                favoritesFlow.value = favorites
            }
    }

    private fun setupMealPlansListener(userId: String) {
        mealPlansListener?.remove()
        mealPlansListener = firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RecipeRepository", "Listen failed for meal plans: ${e.message}", e)
                    return@addSnapshotListener
                }
                val mealPlans = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(MealPlanEntity::class.java)?.copy(date = doc.id)
                } ?: emptyList()
                mealPlansFlow.value = mealPlans
            }
    }

    private fun setupCustomRecipesListener(userId: String) {
        customRecipesListener?.remove()
        customRecipesListener = firestore.collection("users")
            .document(userId)
            .collection("custom_recipes")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("RecipeRepository", "Listen failed for custom recipes: ${e.message}", e)
                    return@addSnapshotListener
                }
                val customRecipes = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CustomRecipe::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                customRecipesFlow.value = customRecipes
                Log.d("RecipeRepository", "Custom recipes updated: $customRecipes, isFromCache: ${snapshot?.metadata?.isFromCache ?: true}")
            }
    }

    fun getFavorites(): StateFlow<List<RecipeSummary>> = favoritesFlow.asStateFlow()

    fun getMealPlans(): StateFlow<List<MealPlanEntity>> = mealPlansFlow.asStateFlow()

    fun getCustomRecipes(): StateFlow<List<CustomRecipe>> = customRecipesFlow.asStateFlow()

    fun getRecipesForDate(date: String): StateFlow<List<MealPlanRecipeEntity>> {
        val userId = firebaseAuth.currentUser?.uid ?: return MutableStateFlow(emptyList<MealPlanRecipeEntity>())
        val flow = recipesForDateFlows[date] ?: MutableStateFlow<List<MealPlanRecipeEntity>>(emptyList()).also { newFlow ->
            recipesForDateFlows[date] = newFlow
            val listener = firestore.collection("users")
                .document(userId)
                .collection("meal_plans")
                .document(date)
                .collection("recipes")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("RecipeRepository", "Listen failed for recipes on $date: ${e.message}", e)
                        return@addSnapshotListener
                    }
                    val recipes = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(MealPlanRecipeEntity::class.java)
                    } ?: emptyList()
                    newFlow.value = recipes
                }
            recipesForDateListeners[date] = listener
        }
        return flow.asStateFlow()
    }

    fun insertMealPlan(mealPlan: MealPlanEntity) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(mealPlan.date)
            .set(mealPlan)
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Meal plan inserted for date: ${mealPlan.date}")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to insert meal plan: ${e.message}", e)
            }
    }

    fun insertMealPlanRecipe(date: String, recipe: MealPlanRecipeEntity) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .collection("recipes")
            .document(recipe.id.toString())
            .set(recipe)
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Recipe inserted for date: $date, recipeId: ${recipe.id}")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to insert recipe: ${e.message}", e)
            }
    }

    fun insertCustomRecipe(recipe: CustomRecipe, callback: (Boolean, String?) -> Unit) {
        val userId = firebaseAuth.currentUser?.uid ?: return callback(false, "User not authenticated")
        val docRef = if (recipe.id.isEmpty()) {
            firestore.collection("users")
                .document(userId)
                .collection("custom_recipes")
                .document()
        } else {
            firestore.collection("users")
                .document(userId)
                .collection("custom_recipes")
                .document(recipe.id)
        }
        val recipeWithId = recipe.copy(id = docRef.id)
        docRef.set(recipeWithId)
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Custom recipe inserted: ${recipeWithId.id}")
                callback(true, null)
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to insert custom recipe: ${e.message}", e)
                callback(false, "Failed to insert custom recipe: ${e.message}")
            }
    }

    fun debugCreateCollection(collectionName: String, callback: (Boolean, String?) -> Unit) {
        val userId = firebaseAuth.currentUser?.uid
        if (userId == null) {
            callback(false, "User not authenticated")
            return
        }
        val collectionRef = firestore.collection("users").document(userId).collection(collectionName)
        val debugDocId = "debug_${System.currentTimeMillis()}"
        collectionRef.document(debugDocId)
            .set(mapOf("created" to System.currentTimeMillis()))
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Debug: Successfully created document in $collectionName")
                Thread.sleep(1000)
                collectionRef.document(debugDocId).delete()
                    .addOnSuccessListener {
                        Log.d("RecipeRepository", "Debug: Cleaned up debug document in $collectionName")
                        callback(true, null)
                    }
                    .addOnFailureListener { e ->
                        Log.e("RecipeRepository", "Failed to clean up debug document: ${e.message}", e)
                        callback(false, "Failed to clean up debug document: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to create debug document in $collectionName: ${e.message}", e)
                callback(false, "Failed to create debug document: ${e.message}")
            }
    }

    fun deleteMealPlanAndRecipes(date: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .delete()
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Meal plan deleted for date: $date")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to delete meal plan: ${e.message}", e)
            }
    }

    fun deleteCustomRecipe(recipeId: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("custom_recipes")
            .document(recipeId)
            .delete()
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Custom recipe deleted: $recipeId")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to delete custom recipe: ${e.message}", e)
            }
    }

    suspend fun getFavorite(recipeId: Int): RecipeSummary? {
        val userId = firebaseAuth.currentUser?.uid ?: return null
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId.toString())
            .get()
            .await()
        return snapshot.toObject(RecipeSummary::class.java)?.copy(id = recipeId)
    }

    fun insertFavorite(recipe: RecipeSummary) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipe.id.toString())
            .set(recipe)
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Favorite inserted: ${recipe.id}")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to insert favorite: ${e.message}", e)
            }
    }

    fun deleteFavorite(recipeId: Int) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("favorites")
            .document(recipeId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Favorite deleted: $recipeId")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to delete favorite: ${e.message}", e)
            }
    }

    fun deleteMealPlanRecipe(date: String, recipeId: Int) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users")
            .document(userId)
            .collection("meal_plans")
            .document(date)
            .collection("recipes")
            .document(recipeId.toString())
            .delete()
            .addOnSuccessListener {
                Log.d("RecipeRepository", "Recipe deleted from meal plan: $date, $recipeId")
            }
            .addOnFailureListener { e ->
                Log.e("RecipeRepository", "Failed to delete recipe: ${e.message}", e)
            }
    }

    private fun clearListeners() {
        favoritesListener?.remove()
        mealPlansListener?.remove()
        customRecipesListener?.remove()
        recipesForDateListeners.values.forEach { it.remove() }
        recipesForDateListeners.clear()
        favoritesFlow.value = emptyList()
        mealPlansFlow.value = emptyList()
        customRecipesFlow.value = emptyList()
        recipesForDateFlows.clear()
    }
}