package com.lodecab.recmeal.data

import com.google.gson.annotations.SerializedName

data class RecipeSummary(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("image") val image: String? = null,
    @SerializedName("usedIngredientCount") val usedIngredientCount: Int = 0,
    @SerializedName("missedIngredientCount") val missedIngredientCount: Int = 0
)