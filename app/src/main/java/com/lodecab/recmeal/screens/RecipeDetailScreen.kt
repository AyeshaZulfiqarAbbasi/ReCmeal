package com.lodecab.recmeal.screens

import NavRoutes
import android.text.Html
import android.widget.CalendarView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.lodecab.recmeal.viewmodel.RecipeDetailsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailsScreen(
    recipeId: Int?, // Change to Int?
    isCustom: Boolean,
    firestoreDocId: String?,
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailsViewModel = hiltViewModel()
) {
    val recipeDetails by viewModel.recipeDetails.collectAsState()
    val customRecipe by viewModel.customRecipe.collectAsState()
    val error by viewModel.error.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    // State for date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }
    val selectedDate = remember { mutableStateOf("") }

    LaunchedEffect(recipeId, isCustom, firestoreDocId) {
        if (isCustom && firestoreDocId != null && firestoreDocId != "null") {
            viewModel.fetchCustomRecipe(firestoreDocId)
        } else if (recipeId != null) { // Check for null
            viewModel.fetchRecipeDetails(recipeId)
        }
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
                IconButton(onClick = {
                    if (isCustom) {
                        customRecipe?.let { viewModel.toggleFavorite(it) }
                    } else {
                        recipeDetails?.let { viewModel.toggleFavorite(it) }
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from Favorites" else "Add to Favorites",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
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
                        if (isCustom) {
                            customRecipe?.let { viewModel.scheduleCustomRecipe(selectedDate.value, it) }
                        } else {
                            recipeDetails?.let { viewModel.scheduleRecipe(selectedDate.value, it) }
                        }
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
            (recipeDetails == null && !isCustom) || (customRecipe == null && isCustom) && error == null -> {
                Text(
                    text = "Loading recipe details...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            isCustom && customRecipe != null -> {
                val details = customRecipe!!
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Ingredients:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                details.ingredients.forEach { ingredient ->
                    Text(
                        text = "- $ingredient",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(
                    text = "Instructions:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                if (details.instructions.isNotEmpty()) {
                    details.instructions.forEachIndexed { index, instruction ->
                        Text(
                            text = "${index + 1}. $instruction",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "No instructions available for this recipe.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (details.nutrition != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text(
                        text = "Nutrition:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Calories: ${details.nutrition.calories}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Protein: ${details.nutrition.protein}g",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Fat: ${details.nutrition.fat}g",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                    Text(
                        text = "Carbohydrates: ${details.nutrition.carbohydrates}g",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
}