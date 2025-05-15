package com.lodecab.recmeal.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.MealPlanEntity
import com.lodecab.recmeal.data.RecipeSummary
import com.lodecab.recmeal.ui.RecipeItem
import com.lodecab.recmeal.viewmodel.AuthState
import com.lodecab.recmeal.viewmodel.AuthViewModel
import com.lodecab.recmeal.viewmodel.MealPlannerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlannerScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MealPlannerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val mealPlans by viewModel.mealPlans.collectAsState(initial = null)
    val authState by authViewModel.authState.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedOut) {
            navController.navigate(NavRoutes.LOGIN) {
                popUpTo(NavRoutes.MEAL_PLANNER) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meal Planner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
                    val validMealPlans = mealPlans!!.filter { it.date.matches("\\d{4}-\\d{2}-\\d{2}".toRegex()) }
                        .sortedBy { it.date }
                    if (validMealPlans.isEmpty()) {
                        Text(
                            text = "No valid meal plans found.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                        LazyColumn {
                            items(validMealPlans) { mealPlan ->
                                MealPlanItem(
                                    mealPlan = mealPlan,
                                    viewModel = viewModel,
                                    onRecipeClick = { recipeId, isCustom ->
                                        if (isCustom) {
                                            Log.d("MealPlannerScreen", "Navigating to custom recipe details: $recipeId")
                                            navController.navigate(NavRoutes.recipeDetailsRoute(recipeId, isCustom = true, firestoreDocId = recipeId))
                                        } else {
                                            val recipeIdInt = recipeId.toIntOrNull()
                                            if (recipeIdInt != null) {
                                                Log.d("MealPlannerScreen", "Navigating to Spoonacular recipe details: $recipeIdInt")
                                                navController.navigate(NavRoutes.recipeDetailsRoute(recipeIdInt.toString(), isCustom = false))
                                            } else {
                                                Log.e("MealPlannerScreen", "Failed to parse recipeId as Int: $recipeId")
                                            }
                                        }
                                    }
                                )
                            }
                        }
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
    onRecipeClick: (String, Boolean) -> Unit
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
                    val recipeSummary = RecipeSummary(
                        id = recipe.id.toIntOrNull() ?: 0,
                        title = recipe.title,
                        image = recipe.recipeImage
                    )
                    RecipeItem(
                        recipe = recipeSummary,
                        title = recipe.title,
                        image = recipe.recipeImage,
                        onClick = { onRecipeClick(recipe.id, recipe.isCustom) },
                        onRemove = { viewModel.removeRecipeFromMealPlan(mealPlan.date, recipe.id) },
                        useCard = false
                    )
                }
            }
        }
    }
}