package com.lodecab.recmeal.screens

import android.text.Html
import android.util.Log
import android.widget.CalendarView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.lodecab.recmeal.data.AnalyzedInstruction
import com.lodecab.recmeal.data.Ingredient
import com.lodecab.recmeal.data.Step
import com.lodecab.recmeal.viewmodel.RecipeDetailsViewModel

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(
    recipeId: Int,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailsViewModel = hiltViewModel()
) {
    val recipeDetails by viewModel.recipeDetails.collectAsState()
    val error by viewModel.error.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val instructions = recipeDetails?.instructions
    val analyzedInstructions = recipeDetails?.analyzedInstructions
    Log.d("RecipeDetailsScreen", "instructions: $instructions")
    Log.d("RecipeDetailsScreen", "analyzedInstructions: $analyzedInstructions")

    // State for date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf("") }

    LaunchedEffect(recipeId) {
        viewModel.fetchRecipeDetails(recipeId)
    }

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Row {
                IconButton(onClick = { recipeDetails?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Schedule Recipe",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker) {
            AlertDialog(
                onDismissRequest = { showDatePicker = false },
                title = { Text("Select Date") },
                text = {
                    AndroidView(
                        factory = { context ->
                            CalendarView(context).apply {
                                val today = Calendar.getInstance().time
                                date = today.time
                                selectedDate.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today)
                                setOnDateChangeListener { _, year, month, dayOfMonth ->
                                    val date = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth)
                                    }.time
                                    selectedDate.value = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        recipeDetails?.let { viewModel.scheduleRecipe(selectedDate.value, it) }
                        showDatePicker = false
                        navController.navigate(NavRoutes.MEAL_PLANNER)
                    }) {
                        Text("Schedule")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }

        when {
            recipeDetails == null && error == null -> {
                Text(
                    text = "Loading recipe details...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            recipeDetails != null -> {
                val details = recipeDetails!!
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                details.image?.let { imageUrl ->
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Recipe Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Ready in: ${details.readyInMinutes} minutes",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Servings: ${details.servings}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Ingredients:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                val ingredientsList = details.ingredients // Smart cast to List<Ingredient>
                ingredientsList.forEach { ingredient: Ingredient ->
                    Text(
                        text = "- ${ingredient.name}: ${ingredient.amount} ${ingredient.unit}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (analyzedInstructions?.isNotEmpty() == true) {
                    analyzedInstructions.forEach { instruction ->
                        instruction.steps.forEach { step ->
                            Text(
                                text = "${step.number}. ${step.step}",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                } else if (!instructions.isNullOrBlank()) {
                    val plainInstructions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        Html.fromHtml(instructions, Html.FROM_HTML_MODE_COMPACT).toString()
                    } else {
                        @Suppress("DEPRECATION")
                        Html.fromHtml(instructions).toString()
                    }
                    Text(
                        text = plainInstructions,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "No instructions available for this recipe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}