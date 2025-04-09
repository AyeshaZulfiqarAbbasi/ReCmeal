package com.lodecab.recmeal.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.lodecab.recmeal.data.Ingredient
import com.lodecab.recmeal.viewmodel.ShoppingListViewModel

@Composable
fun ShoppingListScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    viewModel: ShoppingListViewModel = hiltViewModel()
) {
    val shoppingList by viewModel.shoppingList.collectAsState(initial = emptyList())

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
                text = "Shopping List",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.width(48.dp)) // Balances layout
        }

        if (shoppingList.isEmpty()) {
            Text(
                text = "No items in your shopping list yet. Add recipes to your meal planner!",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
            )
        } else {
            LazyColumn {
                items(shoppingList) { item ->
                    ShoppingListItem(
                        ingredient = item,
                        onRemove = { viewModel.removeItem(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ShoppingListItem(
    ingredient: Ingredient,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "${ingredient.name}: ${ingredient.amount} ${ingredient.unit}",
            style = MaterialTheme.typography.bodyMedium
        )
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Remove $ingredient.name from shopping list",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}