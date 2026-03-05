package com.moneytracker.simplebudget.domain.model

import androidx.compose.ui.graphics.Color

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val color: Color,
    val isDefault: Boolean = false,
    val parentCategoryId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val displayOrder: Int = 0
)

val DefaultCategories = listOf(
    Category(name = "Food & Dining", icon = "restaurant", color = Color(0xFFFF6B6B), isDefault = true),
    Category(name = "Transportation", icon = "directions_car", color = Color(0xFF4ECDC4), isDefault = true),
    Category(name = "Shopping", icon = "shopping_bag", color = Color(0xFFFFE66D), isDefault = true),
    Category(name = "Entertainment", icon = "movie", color = Color(0xFF7158E2), isDefault = true),
    Category(name = "Bills & Utilities", icon = "receipt_long", color = Color(0xFFFF9F43), isDefault = true),
    Category(name = "Health", icon = "medical_services", color = Color(0xFFE056A0), isDefault = true),
    Category(name = "Education", icon = "school", color = Color(0xFF87CEEB), isDefault = true),
    Category(name = "Salary", icon = "payments", color = Color(0xFF3AE374), isDefault = true),
    Category(name = "Investment", icon = "trending_up", color = Color(0xFFB4A7D6), isDefault = true),
    Category(name = "Other", icon = "more_horiz", color = Color(0xFFBDBDBD), isDefault = true)
)

/**
 * Maps parent category name to a list of default subcategories (name, icon, color).
 */
val DefaultSubcategories: Map<String, List<Triple<String, String, Color>>> = mapOf(
    "Food & Dining" to listOf(
        Triple("Groceries", "local_grocery_store", Color(0xFFFF6B6B)),
        Triple("Restaurants", "restaurant", Color(0xFFEE5A24)),
        Triple("Coffee & Tea", "local_cafe", Color(0xFFF3A683)),
        Triple("Fast Food", "fastfood", Color(0xFFFF9F43)),
        Triple("Delivery", "delivery_dining", Color(0xFFE77F67))
    ),
    "Transportation" to listOf(
        Triple("Fuel", "local_gas_station", Color(0xFF4ECDC4)),
        Triple("Public Transit", "train", Color(0xFF17C0EB)),
        Triple("Parking", "local_parking", Color(0xFF01A3A4)),
        Triple("Maintenance", "build", Color(0xFF63CDDA)),
        Triple("Taxi & Rides", "local_taxi", Color(0xFF70A1FF))
    ),
    "Shopping" to listOf(
        Triple("Clothing", "checkroom", Color(0xFFFFE66D)),
        Triple("Electronics", "devices", Color(0xFFF8A5C2)),
        Triple("Home & Garden", "yard", Color(0xFF3AE374)),
        Triple("Personal Care", "spa", Color(0xFFE056A0))
    ),
    "Entertainment" to listOf(
        Triple("Movies & TV", "movie", Color(0xFF95E1D3)),
        Triple("Music", "music_note", Color(0xFF778BEB)),
        Triple("Games", "sports_esports", Color(0xFF7158E2)),
        Triple("Streaming", "live_tv", Color(0xFF5F27CD)),
        Triple("Sports", "sports_soccer", Color(0xFF2BCB6E))
    ),
    "Bills & Utilities" to listOf(
        Triple("Electricity", "bolt", Color(0xFFFFC312)),
        Triple("Water", "water_drop", Color(0xFF17C0EB)),
        Triple("Internet", "wifi", Color(0xFFA8E6CF)),
        Triple("Phone Bill", "phone", Color(0xFF3AE374)),
        Triple("Rent", "apartment", Color(0xFF786FA6))
    ),
    "Health" to listOf(
        Triple("Doctor", "local_hospital", Color(0xFFDDA0DD)),
        Triple("Pharmacy", "local_pharmacy", Color(0xFFCF6A87)),
        Triple("Gym & Fitness", "fitness_center", Color(0xFFE056A0)),
        Triple("Insurance", "shield", Color(0xFF574B90))
    ),
    "Education" to listOf(
        Triple("Tuition", "school", Color(0xFF87CEEB)),
        Triple("Books", "menu_book", Color(0xFF70A1FF)),
        Triple("Courses", "laptop", Color(0xFF778BEB)),
        Triple("Supplies", "brush", Color(0xFF63CDDA))
    ),
    "Salary" to listOf(
        Triple("Main Job", "work", Color(0xFF98D8C8)),
        Triple("Freelance", "laptop", Color(0xFF2BCB6E)),
        Triple("Bonus", "attach_money", Color(0xFF3AE374))
    ),
    "Investment" to listOf(
        Triple("Stocks", "show_chart", Color(0xFFB4A7D6)),
        Triple("Crypto", "currency_exchange", Color(0xFF7158E2)),
        Triple("Dividends", "savings", Color(0xFF5F27CD))
    ),
    "Other" to listOf(
        Triple("Gifts", "card_giftcard", Color(0xFFBDBDBD)),
        Triple("Charity", "volunteer_activism", Color(0xFFF8A5C2)),
        Triple("Miscellaneous", "more_horiz", Color(0xFF786FA6))
    )
)
