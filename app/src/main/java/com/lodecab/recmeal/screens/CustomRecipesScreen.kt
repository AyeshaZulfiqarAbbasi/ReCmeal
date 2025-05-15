package com.lodecab.recmeal.screens

import NavRoutes
import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.viewmodel.CustomRecipesViewModel
import kotlinx.coroutines.delay
import java.util.Calendar

@Composable
fun CustomRecipesScreen(
    navController: NavHostController,
    viewModel: CustomRecipesViewModel = hiltViewModel()
) {
    val customRecipes by viewModel.customRecipes.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Trigger a recomposition when error changes to ensure UI updates
    LaunchedEffect(error) {
        if (error != null && error!!.isNotEmpty() && error != "null") {
            Log.d("CustomRecipesScreen", "Error updated: $error")
            // Show error in Snackbar and auto-dismiss after 5 seconds
            snackbarHostState.showSnackbar(
                message = error!!,
                duration = SnackbarDuration.Long
            )
            // Clear the error after displaying it
            delay(5000) // 5 seconds
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Text(
                        text = "Custom Recipes",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                if (customRecipes.isEmpty()) {
                    Text(
                        text = "No custom recipes yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn {
                        items(customRecipes) { recipe ->
                            CustomRecipeItem(
                                recipe = recipe,
                                onDelete = { viewModel.deleteCustomRecipe(recipe.id) },
                                onAddToMealPlan = { date ->
                                    viewModel.addRecipeToMealPlan(date, recipe)
                                    navController.navigate(NavRoutes.MEAL_PLANNER)
                                },
                                onClick = {
                                    navController.navigate(NavRoutes.recipeDetailsRoute(recipe.id, isCustom = true, firestoreDocId = recipe.id))
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun CustomRecipeItem(
    recipe: CustomRecipe,
    onDelete: () -> Unit,
    onAddToMealPlan: (String) -> Unit,
    onClick: () -> Unit
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }

    // Context for launching the DatePickerDialog
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = recipe.title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ingredients: ${recipe.ingredients.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Instructions: ${recipe.instructions.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (recipe.nutrition != null) {
                Text(
                    text = "Nutrition: Calories=${recipe.nutrition.calories}, Protein=${recipe.nutrition.protein}g, Fat=${recipe.nutrition.fat}g, Carbs=${recipe.nutrition.carbohydrates}g",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { showDatePickerDialog = true }) {
                    Text("Add to Meal Plan")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }

    if (showDatePickerDialog) {
        // Show DatePickerDialog
        DatePickerDialog(
            context,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Format the selected date as YYYY-MM-DD
                val formattedDate = String.format("%d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                selectedDate = formattedDate
                onAddToMealPlan(formattedDate)
                showDatePickerDialog = false
            },
            year,
            month,
            day
        ).apply {
            // Optional: Set a minimum date to prevent selecting past dates
            datePicker.minDate = calendar.timeInMillis
            show()
        }

        // Reset the dialog state after showing to prevent re-showing on recomposition
        LaunchedEffect(showDatePickerDialog) {
            if (showDatePickerDialog) {
                showDatePickerDialog = false
            }
        }
    }
}