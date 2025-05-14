package com.lodecab.recmeal.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.viewmodel.CustomRecipesViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRecipeDetailsScreen(
    navController: NavHostController,
    firestoreDocId: String,
    viewModel: CustomRecipesViewModel = hiltViewModel()
) {
    // Collect the custom recipe flow
    val customRecipe by viewModel.getCustomRecipe(firestoreDocId).collectAsState(initial = null)

    LaunchedEffect(firestoreDocId) {
        Log.d("CustomRecipeDetails", "Loading custom recipe with firestoreDocId: $firestoreDocId")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Recipe Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (customRecipe == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Log.d("CustomRecipeDetails", "Loading recipe for firestoreDocId: $firestoreDocId")
            } else {
                val recipe = customRecipe!!
                Text(text = "Title: ${recipe.title}", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Ingredients: ${recipe.ingredients.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Instructions: ${recipe.instructions.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                recipe.nutrition?.let { nutrition ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nutrition: Calories=${nutrition.calories}, Protein=${nutrition.protein}g, " +
                                "Fat=${nutrition.fat}g, Carbs=${nutrition.carbohydrates}g",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}