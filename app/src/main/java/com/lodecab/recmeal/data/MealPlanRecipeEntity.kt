package com.lodecab.recmeal.data

import com.google.firebase.firestore.PropertyName

data class MealPlanRecipeEntity(
    @PropertyName("date") val date: String = "",
    @PropertyName("recipeId") val recipeId: Int = 0,
    @PropertyName("recipeTitle") val recipeTitle: String = "",
    @PropertyName("recipeImage") val recipeImage: String? = null
)