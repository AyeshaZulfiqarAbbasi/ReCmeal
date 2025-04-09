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
import com.lodecab.recmeal.data.CustomRecipe
import com.lodecab.recmeal.data.Nutrition
import com.lodecab.recmeal.viewmodel.CustomRecipesViewModel

@Composable
fun CustomRecipesScreen(
    navController: NavHostController,
    viewModel: CustomRecipesViewModel = hiltViewModel()
) {
    val customRecipes by viewModel.customRecipes.collectAsState()

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
                        },
                        navController = navController
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
    navController: NavHostController
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf("") }

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
                        onValueChange = { selectedDate = it },
                        label = { Text("Date") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedDate.isNotBlank()) {
                            onAddToMealPlan(selectedDate)
                            showDatePicker = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}