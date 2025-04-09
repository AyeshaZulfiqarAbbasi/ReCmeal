package com.lodecab.recmeal.data

import com.google.firebase.firestore.PropertyName
import com.google.gson.annotations.SerializedName

data class RecipeDetails(
    @SerializedName("id") @PropertyName("id") val id: Int = 0,
    @SerializedName("title") @PropertyName("title") val title: String = "",
    @SerializedName("image") @PropertyName("image") val image: String? = null,
    @SerializedName("readyInMinutes") @PropertyName("readyInMinutes") val readyInMinutes: Int = 0,
    @SerializedName("servings") @PropertyName("servings") val servings: Int = 0,
    @SerializedName("extendedIngredients") @PropertyName("ingredients") val ingredients: List<Ingredient> = emptyList(),
    @SerializedName("instructions") @PropertyName("instructions") val instructions: String? = null,
    @SerializedName("analyzedInstructions") @PropertyName("analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction>? = null
)

data class Ingredient(
    @SerializedName("name") @PropertyName("name") val name: String = "",
    @SerializedName("amount") @PropertyName("amount") val amount: Double = 0.0,
    @SerializedName("unit") @PropertyName("unit") val unit: String = ""

)

data class AnalyzedInstruction(
    @SerializedName("name") @PropertyName("name") val name: String = "",
    @SerializedName("steps") @PropertyName("steps") val steps: List<Step> = emptyList()
)

data class Step(
    @SerializedName("number") @PropertyName("number") val number: Int = 0,
    @SerializedName("step") @PropertyName("step") val step: String = ""
)

data class IngredientSuggestion(
    @SerializedName("id") @PropertyName("id") val id: Int = 0,
    @SerializedName("name") @PropertyName("name") val name: String = ""
)