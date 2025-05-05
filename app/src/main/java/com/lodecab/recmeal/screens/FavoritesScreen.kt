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
import com.lodecab.recmeal.ui.RecipeItem
import com.lodecab.recmeal.viewmodel.AuthState
import com.lodecab.recmeal.viewmodel.AuthViewModel
import com.lodecab.recmeal.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val favorites by viewModel.favorites.collectAsState(initial = emptyList())
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.SignedOut) {
            navController.navigate(NavRoutes.LOGIN) {
                popUpTo(NavRoutes.FAVORITES) { inclusive = true }
            }
        }
    }

    Column(
        modifier = modifier
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
                text = "Favorites",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp))
        }

        if (favorites.size == 0) { // Replaced isEmpty() to avoid ambiguity
            Text(
                text = "No favorite recipes yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            LazyColumn {
                items(favorites) { recipe ->
                    RecipeItem(
                        title = recipe.title,
                        image = recipe.image,
                        usedIngredientCount = recipe.usedIngredientCount,
                        missedIngredientCount = recipe.missedIngredientCount,
                        onClick = {
                            navController.navigate(NavRoutes.recipeDetailsRoute(recipe.id))
                        },
                        onRemove = {
                            viewModel.removeFavorite(recipe.id)
                        },
                        useCard = true
                    )
                }
            }
        }
    }
}