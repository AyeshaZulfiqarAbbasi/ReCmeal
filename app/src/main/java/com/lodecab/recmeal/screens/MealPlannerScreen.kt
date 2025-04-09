package com.lodecab.recmeal.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.ui.RecipeItem
import com.lodecab.recmeal.viewmodel.AuthState
import com.lodecab.recmeal.viewmodel.AuthViewModel
import com.lodecab.recmeal.viewmodel.MealPlannerViewModel


@Composable
fun MealPlannerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MealPlannerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val mealPlans by viewModel.mealPlans.collectAsState(initial = null)
    val authState by authViewModel.authState.collectAsState()

    // Redirect to login if not signed in
    LaunchedEffect(authState) {
        if (authState is AuthState.SignedOut) {
            navController.navigate(NavRoutes.LOGIN) {
                popUpTo(NavRoutes.MEAL_PLANNER) { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Meal Planner",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp)) // To balance the layout
        }

        when {
            mealPlans == null -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            mealPlans!!.isEmpty() -> {
                Text(
                    text = "No meal plans yet. Add recipes from the recipe details screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            else -> {
                LazyColumn {
                    items(mealPlans!!) { mealPlan ->
                        MealPlanItem(
                            mealPlan = mealPlan,
                            viewModel = viewModel,
                            onRecipeClick = { recipeId ->
                                navController.navigate(NavRoutes.recipeDetailsRoute(recipeId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MealPlanItem(
    mealPlan: MealPlanEntity,
    viewModel: MealPlannerViewModel,
    onRecipeClick: (Int) -> Unit
) {
    val recipes by viewModel.getRecipesForDate(mealPlan.date).collectAsState(initial = emptyList())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = mealPlan.date,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (recipes.isEmpty()) {
                Text(
                    text = "No recipes scheduled for this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                recipes.forEach { recipe ->
                    RecipeItem(
                        title = recipe.recipeTitle,
                        image = recipe.recipeImage,
                        onClick = { onRecipeClick(recipe.recipeId) },
                        onRemove = { viewModel.removeRecipeFromMealPlan(mealPlan.date, recipe.recipeId) },
                        useCard = false
                    )
                }
            }
        }
    }
}