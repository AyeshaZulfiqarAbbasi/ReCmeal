object NavRoutes {
    const val LOGIN = "login"
    const val RECIPE_LIST = "recipe_list"
    const val RECIPE_DETAILS = "recipe_details/{recipeId}"
    const val FAVORITES = "favorites"
    const val MEAL_PLANNER = "meal_planner"
    const val PROFILE = "profile"

    fun recipeDetailsRoute(recipeId: Int) = "recipe_details/$recipeId"
}