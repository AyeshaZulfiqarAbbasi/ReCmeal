package com.lodecab.recmeal.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.Nutrition
import com.lodecab.recmeal.viewmodel.CustomRecipeViewModel

@Composable
fun CustomRecipeScreen(
    navController: NavHostController,
    viewModel: CustomRecipeViewModel = hiltViewModel()
) {
    val recipeTitle by viewModel.recipeTitle.collectAsState()
    val ingredients by viewModel.ingredients.collectAsState()
    val instructions by viewModel.instructions.collectAsState()
    val nutritionInfo by viewModel.nutritionInfo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val navigateToRecipeList by viewModel.navigateToRecipeList.collectAsState()

    var ingredientInput by remember { mutableStateOf("") }
    var instructionInput by remember { mutableStateOf("") }

    LaunchedEffect(navigateToRecipeList) {
        if (navigateToRecipeList) {
            navController.navigate(NavRoutes.CUSTOM_RECIPES) {
                popUpTo(NavRoutes.CUSTOM_RECIPE) { inclusive = true }
            }
            viewModel.onNavigationHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Create Custom Recipe",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipe Title
        OutlinedTextField(
            value = recipeTitle,
            onValueChange = { viewModel.updateRecipeTitle(it) },
            label = { Text("Recipe Title") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Ingredients
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ingredientInput,
                onValueChange = { ingredientInput = it },
                label = { Text("Add Ingredient (e.g., 1 cup water)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.addIngredient(ingredientInput)
                    ingredientInput = ""
                },
                enabled = ingredientInput.isNotBlank()
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        ingredients.forEach { ingredient ->
            Text(
                text = "- $ingredient",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Instructions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = instructionInput,
                onValueChange = { instructionInput = it },
                label = { Text("Add Instruction") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.addInstruction(instructionInput)
                    instructionInput = ""
                },
                enabled = instructionInput.isNotBlank()
            ) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        instructions.forEachIndexed { index, instruction ->
            Text(
                text = "${index + 1}. $instruction",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.analyzeNutrition() },
            modifier = Modifier.fillMaxWidth(),
            enabled = ingredients.isNotEmpty() && !isLoading
        ) {
            Text("Analyze Nutrition")
        }

        Spacer(modifier = Modifier.height(8.dp))

        nutritionInfo?.let { nutrition ->
            if (nutrition != Nutrition(0, 0, 0, 0)) {
                Text(
                    text = "Nutrition",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Calories: ${nutrition.calories}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Protein: ${nutrition.protein}g",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Fat: ${nutrition.fat}g",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
                Text(
                    text = "Carbohydrates: ${nutrition.carbohydrates}g",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))


        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = { viewModel.saveRecipe() },
            modifier = Modifier.fillMaxWidth(),
            enabled = recipeTitle.isNotBlank() && ingredients.isNotEmpty()
        ) {
            Text("Save Recipe")
        }
    }
}