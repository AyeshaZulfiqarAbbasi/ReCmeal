package com.lodecab.recmeal.data

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
@IgnoreExtraProperties
data class MealPlanEntity(
    @PropertyName("date") val date: String = "",
    @PropertyName("name") val name: String = ""

)

