package com.lodecab.recmeal.data

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SpoonacularApiService {
    @GET("recipes/findByIngredients")
    suspend fun findRecipesByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("apiKey") apiKey: String,
        @Query("number") number: Int = 10
    ): List<RecipeSummary>

    @GET("food/ingredients/{id}/information")
    suspend fun getIngredientInfo(
        @Path("id") id: Int,
        @Query("apiKey") apiKey: String,
        @Query("amount") amount: Double,
        @Query("unit") unit: String
    ): IngredientInfoResponse

    @GET("food/ingredients/search")
    suspend fun searchIngredients(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String
    ): IngredientSearchResponse

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
    ): List<IngredientSuggestion>

    @FormUrlEncoded
    @POST("recipes/parseIngredients")
    suspend fun parseIngredients(
        @Query("apiKey") apiKey: String,
        @Query("servings") servings: Int = 1,
        @Field("ingredientList") ingredientList: String
    ): List<ParsedIngredient>
}

data class ParsedIngredient(
    val name: String,
    val amount: Double,
    val unit: String,
    val nutrients: List<Nutrient>? // Changed to nullable
)

data class Nutrient(
    val name: String,
    val amount: Double,
    val unit: String
)

data class IngredientSearchResponse(
    val results: List<IngredientSearchResult>
)

data class IngredientSearchResult(
    val id: Int,
    val name: String
)

data class IngredientInfoResponse(
    val id: Int,
    val name: String,
    val nutrition: NutritionData?
)

data class NutritionData(
    val nutrients: List<Nutrient>
)