package com.lodecab.recmeal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.lodecab.recmeal.data.RecipeSummary

@Composable
fun RecipeItem(
    recipe: RecipeSummary,
    title: String,
    image: String?,
    onClick: () -> Unit,
    onRemove: (() -> Unit)? = null,
    useCard: Boolean = false
) {
    val content = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = image,
                    contentDescription = "Recipe image for $title",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Remove "Used Ingredients" and "Missed" text to avoid null exceptions
                }
            }
            if (onRemove != null) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove recipe",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (useCard) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = recipe.title, style = MaterialTheme.typography.titleMedium)
                // Other UI elements...
            }
        }
    } else {
        content()
    }
}