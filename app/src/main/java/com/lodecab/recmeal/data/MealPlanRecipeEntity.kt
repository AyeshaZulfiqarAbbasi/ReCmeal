package com.lodecab.recmeal.data

import com.google.firebase.firestore.PropertyName

data class MealPlanRecipeEntity(
    @PropertyName("date") val date: String = "",
    @PropertyName("id") private val rawId: Any? = "", // Temporarily accept Any
    @PropertyName("title") val title: String = "",
    @PropertyName("recipeImage") val recipeImage: String? = null,
    @PropertyName("custom") val isCustom: Boolean = false,
    @PropertyName("firestoreDocId") val firestoreDocId: String? = null
) {
    val id: String
        get() = when (rawId) {
            is String -> rawId
            is Long -> rawId.toString() // Convert Long to String
            else -> "" // Fallback for unexpected types
        }
}