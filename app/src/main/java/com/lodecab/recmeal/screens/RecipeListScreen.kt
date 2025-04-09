import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Find Recipes",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Row {
                IconButton(onClick = { navController.navigate(NavRoutes.FAVORITES) }) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Go to Favorites",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { navController.navigate(NavRoutes.MEAL_PLANNER) }) {
                    Icon(
                        imageVector = Icons.Filled.CalendarToday,
                        contentDescription = "Go to Meal Planner",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { navController.navigate(NavRoutes.PROFILE) }) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Go to Profile",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box {
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
                delay(3000) // Show for 3 seconds
                viewModel.clearNavigationError()
            }
        }

        if (recipes.isNotEmpty()) {
            LazyColumn {
                items(recipes) { recipe ->
                    RecipeItem(
                        title = recipe.title,
                        image = recipe.image,
                        usedIngredientCount = recipe.usedIngredientCount,
                        missedIngredientCount = recipe.missedIngredientCount,
                        onClick = {
                            navController.navigate(NavRoutes.recipeDetailsRoute(recipe.id))
                        },
                        useCard = true
                    )
                }
            }
        }
    }
}