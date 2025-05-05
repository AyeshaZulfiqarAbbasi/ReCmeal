object NavRoutes {
    const val LOGIN = "login"
    const val RECIPE_LIST = "recipe_list"
    const val RECIPE_DETAILS = "recipe_details/{recipeId}/{isCustom}/{firestoreDocId}"
    const val CUSTOM_RECIPE = "custom_recipe"
    const val CUSTOM_RECIPES = "custom_recipes"
    const val FAVORITES = "favorites"
    const val MEAL_PLANNER = "meal_planner"
    const val PROFILE = "profile"

    fun recipeDetailsRoute(recipeId: Int, isCustom: Boolean = false, firestoreDocId: String? = null): String {
        return "recipe_details/$recipeId/$isCustom/${firestoreDocId ?: "null"}"
    }
}