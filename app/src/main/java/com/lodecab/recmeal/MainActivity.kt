package com.lodecab.recmeal


import RecipeListScreen
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lodecab.recmeal.screens.FavoritesScreen
import com.lodecab.recmeal.screens.LoginScreen
import com.lodecab.recmeal.screens.MealPlannerScreen
import com.lodecab.recmeal.screens.ProfileScreen
import com.lodecab.recmeal.screens.RecipeDetailsScreen
import com.lodecab.recmeal.ui.theme.RecMealTheme
import com.lodecab.recmeal.viewmodel.AuthState
import com.lodecab.recmeal.viewmodel.AuthViewModel
import com.lodecab.recmeal.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

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
        composable(NavRoutes.RECIPE_DETAILS) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")?.toIntOrNull()
            val mainViewModel: MainViewModel = hiltViewModel(navController.getBackStackEntry(NavRoutes.RECIPE_LIST))
            if (recipeId != null) {
                RecipeDetailsScreen(recipeId = recipeId, navController = navController)
            } else {
                mainViewModel.setNavigationError("Invalid recipe ID. Please try again.")
                navController.navigate(NavRoutes.RECIPE_LIST) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = false }
                    launchSingleTop = true
                }
            }
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