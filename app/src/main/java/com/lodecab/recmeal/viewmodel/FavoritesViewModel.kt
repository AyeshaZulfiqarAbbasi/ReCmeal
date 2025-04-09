package com.lodecab.recmeal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lodecab.recmeal.data.RecipeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val recipeRepository: RecipeRepository
) : ViewModel() {
    val favorites = recipeRepository.getAllFavorites()

    fun removeFavorite(recipeId: Int) {
        viewModelScope.launch {
            recipeRepository.deleteFavorite(recipeId)
        }
    }
}