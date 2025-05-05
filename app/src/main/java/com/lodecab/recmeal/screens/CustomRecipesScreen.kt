package com.lodecab.recmeal.screens

import NavRoutes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.viewmodel.CustomRecipesViewModel

@Composable
fun CustomRecipesScreen(
    navController: NavHostController,
    viewModel: CustomRecipesViewModel = hiltViewModel()
) {
    val customRecipes by viewModel.customRecipes.collectAsState()
    val error by viewModel.error.collectAsState()

    Column(
        modifier = Modifier
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
                text = "Custom Recipes",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        // Display error message if present
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
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
                            // Navigate to MealPlannerScreen to see the updated meal plan
                            navController.navigate(NavRoutes.MEAL_PLANNER)
                        },
                        onClick = {
                            navController.navigate(NavRoutes.recipeDetailsRoute(recipe.id.toInt(), isCustom = true, firestoreDocId = recipe.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomRecipeItem(
    recipe: CustomRecipe,
    onDelete: () -> Unit,
    onAddToMealPlan: (String) -> Unit,
    onClick: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }
    var dateError by remember { mutableStateOf<String?>(null) }

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
                Button(onClick = { showDatePicker = true }) {
                    Text("Add to Meal Plan")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }

    if (showDatePicker) {
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Date for Meal Plan") },
            text = {
                Column {
                    Text("Enter date (YYYY-MM-DD):")
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {
                            selectedDate = it
                            // Validate the date format
                            dateError = if (selectedDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                null
                            } else {
                                "Please use YYYY-MM-DD format"
                            }
                        },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = dateError != null
                    )
                    dateError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDate.isNotBlank() && dateError == null) {
                            onAddToMealPlan(selectedDate)
                            showDatePicker = false
                        }
                    },
                    enabled = selectedDate.isNotBlank() && dateError == null
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showDatePicker = false
                    selectedDate = ""
                    dateError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}