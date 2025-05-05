package com.lodecab.recmeal.data

import com.google.firebase.firestore.PropertyName

data class MealPlanRecipeEntity(
    @PropertyName("date") val date: String = "",
    @PropertyName("recipeId") val id: Int = 0,
    @PropertyName("recipeTitle") val title: String = "",
    @PropertyName("recipeImage") val recipeImage: String? = null,
    val isCustom: Boolean,
    val firestoreDocId: String?
)