package com.lodecab.recmeal.data

import com.google.firebase.firestore.PropertyName

data class MealPlanEntity(
    @PropertyName("date") val date: String = "",
    @PropertyName("name") val name: String = ""
)

