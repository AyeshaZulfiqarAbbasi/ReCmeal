package com.lodecab.recmeal.data

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class CustomRecipe(
    val id: String = "", // This should be the Firestore document ID
    val title: String = "",
    val ingredients: List<String> = emptyList(),
    val instructions: List<String> = emptyList(),
    val nutrition: Nutrition? = null
)

data class Nutrition(
    val calories: Int = 0,
    val protein: Int = 0,
    val fat: Int = 0,
    val carbohydrates: Int = 0
)