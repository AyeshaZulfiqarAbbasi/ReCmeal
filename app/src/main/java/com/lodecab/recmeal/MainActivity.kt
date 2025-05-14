package com.lodecab.recmeal

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lodecab.recmeal.screens.*
import com.lodecab.recmeal.ui.theme.RecMealTheme
import com.lodecab.recmeal.viewmodel.AuthState
import com.lodecab.recmeal.viewmodel.AuthViewModel
import com.lodecab.recmeal.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.navigation.navArgument

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecMealTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val authState by authViewModel.authState.collectAsState()

                val startDestination = when (authState) {
                    is AuthState.SignedIn -> NavRoutes.RECIPE_LIST
                    else -> NavRoutes.LOGIN
                }

                AppNavigation(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(NavRoutes.RECIPE_LIST) {
            RecipeListScreen(navController = navController)
        }
        composable(
            NavRoutes.RECIPE_DETAILS,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.StringType },
                navArgument("isCustom") { type = NavType.BoolType },
                navArgument("firestoreDocId") { type = NavType.StringType; defaultValue = "null" }
            )
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId") ?: "null"
            val isCustom = backStackEntry.arguments?.getBoolean("isCustom") ?: false
            val firestoreDocId = backStackEntry.arguments?.getString("firestoreDocId") ?: "null"
            val mainViewModel: MainViewModel = hiltViewModel(navController.getBackStackEntry(NavRoutes.RECIPE_LIST))

            when {
                isCustom && firestoreDocId != "null" -> {
                    Log.d("NavHost", "Navigating to CustomRecipeDetailsScreen with firestoreDocId: $firestoreDocId")
                    CustomRecipeDetailsScreen(navController, firestoreDocId)
                }
                recipeId != "null" -> {
                    val recipeIdInt = recipeId.toIntOrNull()
                    if (recipeIdInt != null) {
                        Log.d("NavHost", "Navigating to RecipeDetailsScreen with recipeId: $recipeIdInt")
                        RecipeDetailsScreen(
                            recipeId = recipeIdInt,
                            isCustom = false,
                            firestoreDocId = null,
                            navController = navController
                        )
                    } else {
                        mainViewModel.setNavigationError("Invalid recipe ID: $recipeId")
                        navController.navigate(NavRoutes.RECIPE_LIST) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                }
                else -> {
                    mainViewModel.setNavigationError("Invalid navigation parameters: recipeId=$recipeId, isCustom=$isCustom, firestoreDocId=$firestoreDocId")
                    navController.navigate(NavRoutes.RECIPE_LIST) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable(NavRoutes.CUSTOM_RECIPE) {
            CustomRecipeScreen(navController = navController)
        }
        composable(NavRoutes.CUSTOM_RECIPES) {
            CustomRecipesScreen(navController = navController)
        }
        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(navController = navController)
        }
        composable(NavRoutes.MEAL_PLANNER) {
            MealPlannerScreen(navController = navController)
        }
        composable(NavRoutes.PROFILE) {
            ProfileScreen(navController = navController)
        }
    }
}