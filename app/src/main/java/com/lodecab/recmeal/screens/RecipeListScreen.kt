package com.lodecab.recmeal.screens

import NavRoutes
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.ui.RecipeItem
import com.lodecab.recmeal.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun RecipeListScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel()
) {
    val searchQuery = remember { mutableStateOf("") }
    val recipes by viewModel.recipes.collectAsState()
    val error by viewModel.error.collectAsState()
    val ingredientSuggestions by viewModel.ingredientSuggestions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    // Log screen width only once when the composable is first composed
    LaunchedEffect(Unit) {
        Log.d("ScreenWidth", "Screen width in dp: $screenWidthDp")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Add padding to move content below the camera notch
        Spacer(modifier = Modifier.height(24.dp)) // Adjust this value based on notch height

        // Header with title and navigation icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .zIndex(1f) // Ensure header is above other elements
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Find Recipes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .wrapContentWidth(Alignment.Start)
            )
            // Adaptive navigation icons
            if (screenWidthDp >= 250) {
                // Show all icons on wider screens
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.background(Color.Transparent)
                ) {
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.CUSTOM_RECIPE) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create Custom Recipe",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.CUSTOM_RECIPES) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "View Custom Recipes",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.FAVORITES) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Go to Favorites",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.MEAL_PLANNER) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Go to Meal Planner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { navController.navigate(NavRoutes.PROFILE) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Go to Profile",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                // Show "More" button with dropdown on narrower screens
                var expanded by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("Create Custom Recipe") },
                        onClick = {
                            navController.navigate(NavRoutes.CUSTOM_RECIPE)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Custom Recipes") },
                        onClick = {
                            navController.navigate(NavRoutes.CUSTOM_RECIPES)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Favorites") },
                        onClick = {
                            navController.navigate(NavRoutes.FAVORITES)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Meal Planner") },
                        onClick = {
                            navController.navigate(NavRoutes.MEAL_PLANNER)
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Profile") },
                        onClick = {
                            navController.navigate(NavRoutes.PROFILE)
                            expanded = false
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier.zIndex(0f) // Ensure text field is below header
        ) {
            OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { newValue ->
                    searchQuery.value = newValue
                    viewModel.fetchIngredientSuggestions(newValue)
                },
                label = { Text("Enter ingredients (comma separated)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    disabledIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    errorIndicatorColor = MaterialTheme.colorScheme.error,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    errorLabelColor = MaterialTheme.colorScheme.error
                )
            )
            DropdownMenu(
                expanded = ingredientSuggestions.isNotEmpty(),
                onDismissRequest = { viewModel.clearIngredientSuggestions() },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clip(RoundedCornerShape(8.dp))
                    .zIndex(0f) // Ensure dropdown doesn’t overlap header
            ) {
                ingredientSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion.name, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            val currentIngredients = searchQuery.value.split(",").map { it.trim() }
                            val updatedIngredients = if (currentIngredients.isNotEmpty() && currentIngredients.last().isNotBlank()) {
                                currentIngredients.dropLast(1) + suggestion.name
                            } else {
                                currentIngredients + suggestion.name
                            }
                            searchQuery.value = updatedIngredients.joinToString(", ")
                            viewModel.clearIngredientSuggestions()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (searchQuery.value.isNotBlank()) {
                        viewModel.searchRecipes(searchQuery.value)
                        viewModel.clearIngredientSuggestions()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Search Recipes", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
                onClick = {
                    searchQuery.value = ""
                    viewModel.clearRecipes()
                    viewModel.clearIngredientSuggestions()
                },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Clear", style = MaterialTheme.typography.labelMedium)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LaunchedEffect(it) {
                delay(3000)
                viewModel.clearError()
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (recipes.isEmpty() && searchQuery.value.isNotBlank()) {
            Text(
                text = "No recipes found. Try different ingredients.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (recipes.isEmpty()) {
            Text(
                text = "Enter ingredients and search to find recipes!",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                    items(recipes) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            title = recipe.title,
                            image = recipe.image,
                            onClick = {
                                navController.navigate(
                                    NavRoutes.recipeDetailsRoute(
                                        recipe.id.toString(),
                                        isCustom = false,
                                        firestoreDocId = null
                                    )
                                )
                            },
                            useCard = true
                        )
                    }
                }
            }
        }
    }
