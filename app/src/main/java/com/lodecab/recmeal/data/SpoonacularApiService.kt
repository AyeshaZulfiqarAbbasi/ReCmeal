package com.lodecab.recmeal.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApiService {
    @GET("recipes/findByIngredients")
    suspend fun findRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int = 10
    ): List<RecipeSummary>  // Specify RecipeSummary

    @GET("recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean = false,
        @Query("includeAnalyzedInstructions") includeAnalyzedInstructions: Boolean = true
    ): RecipeDetails

    @GET("food/ingredients/autocomplete")
    suspend fun autocompleteIngredients(
        @Query("query") query: String,
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int = 5
    ): List<IngredientSuggestion>  // Specify IngredientSuggestion
}