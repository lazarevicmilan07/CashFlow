package com.moneytracker.simplebudget.ui.categories

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.ui.components.AvailableColors
import com.moneytracker.simplebudget.ui.components.AvailableIcons
import com.moneytracker.simplebudget.ui.components.CategoryIcon
import com.moneytracker.simplebudget.ui.components.dragHandle
import com.moneytracker.simplebudget.ui.components.draggableItem
import com.moneytracker.simplebudget.ui.components.getIconForName
import com.moneytracker.simplebudget.ui.components.rememberDragDropListState

private fun groupKeyOf(key: Any): String {
    val k = key as? String ?: return ""
    return if (k.startsWith("root_")) "root"
    else {
        val parts = k.split("_")
        if (parts.size >= 2) "${parts[0]}_${parts[1]}" else ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: (() -> Unit)? = null,
    onShowPremium: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categoriesState by viewModel.categoriesState.collectAsState()
    val expandedCategories by viewModel.expandedCategories.collectAsState()
    val context = LocalContext.current

    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    // Local mutable state for drag-in-progress ordering
    var localRoots by remember { mutableStateOf(categoriesState.rootCategories) }
    var localSubs by remember { mutableStateOf(categoriesState.subcategoriesMap) }

    // Sync from DB when not dragging
    val lazyListState = rememberLazyListState()
    val dragDropState = rememberDragDropListState(lazyListState)

    LaunchedEffect(categoriesState.rootCategories, categoriesState.subcategoriesMap) {
        if (!dragDropState.isDragging) {
            localRoots = categoriesState.rootCategories
            localSubs = categoriesState.subcategoriesMap
        }
    }

    dragDropState.groupKeyOf = ::groupKeyOf
    dragDropState.onSwap = onSwap@{ draggedKey, targetKey ->
        val dk = draggedKey as? String ?: return@onSwap
        val tk = targetKey as? String ?: return@onSwap

        if (dk.startsWith("root_") && tk.startsWith("root_")) {
            val draggedId = dk.removePrefix("root_").toLong()
            val targetId = tk.removePrefix("root_").toLong()
            val list = localRoots.toMutableList()
            val from = list.indexOfFirst { it.id == draggedId }
            val to = list.indexOfFirst { it.id == targetId }
            if (from >= 0 && to >= 0) {
                list.add(to, list.removeAt(from))
                localRoots = list
            }
        } else if (dk.startsWith("sub_") && tk.startsWith("sub_")) {
            val dParts = dk.split("_")
            val tParts = tk.split("_")
            val parentId = dParts[1].toLong()
            val draggedId = dParts[2].toLong()
            val targetId = tParts[2].toLong()
            val map = localSubs.toMutableMap()
            val list = map[parentId]?.toMutableList() ?: return@onSwap
            val from = list.indexOfFirst { it.id == draggedId }
            val to = list.indexOfFirst { it.id == targetId }
            if (from >= 0 && to >= 0) {
                list.add(to, list.removeAt(from))
                map[parentId] = list
                localSubs = map
            }
        }
    }
    dragDropState.onDragEnd = {
        viewModel.saveCategoryOrder(localRoots)
        localSubs.forEach { (_, subcats) ->
            viewModel.saveCategoryOrder(subcats)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CategoryEvent.CategorySaved -> {
                    Toast.makeText(context, "Category saved", Toast.LENGTH_SHORT).show()
                }
                is CategoryEvent.CategoryDeleted -> {
                    Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
                }
                is CategoryEvent.ShowPremiumRequired -> {
                    onShowPremium()
                }
                is CategoryEvent.ShowError -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = lazyListState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (onNavigateBack != null) 64.dp else 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                localRoots.forEach { category ->
                    val itemKey = "root_${category.id}"
                    val hasSubcategories = localSubs.containsKey(category.id)
                    val isExpanded = category.id in expandedCategories

                    item(key = itemKey) {
                        CategoryListItem(
                            category = category,
                            hasSubcategories = hasSubcategories,
                            isExpanded = isExpanded,
                            onToggleExpand = { viewModel.toggleCategoryExpanded(category.id) },
                            onClick = { viewModel.showEditDialog(category) },
                            onAddSubcategory = { viewModel.showAddDialog(parentCategoryId = category.id) },
                            dragHandleModifier = Modifier.dragHandle(dragDropState, itemKey),
                            modifier = Modifier.draggableItem(dragDropState, itemKey)
                        )
                    }

                    if (hasSubcategories && isExpanded) {
                        val subcategories = localSubs[category.id] ?: emptyList()
                        subcategories.forEach { subcategory ->
                            val subKey = "sub_${category.id}_${subcategory.id}"
                            item(key = subKey) {
                                SubcategoryListItem(
                                    category = subcategory,
                                    onClick = { viewModel.showEditDialog(subcategory) },
                                    dragHandleModifier = Modifier.dragHandle(dragDropState, subKey),
                                    modifier = Modifier.draggableItem(dragDropState, subKey)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Dialog
    if (uiState.showDialog) {
        CategoryDialog(
            isEditing = uiState.editingCategory != null,
            isSubcategory = uiState.parentCategoryId != null || uiState.editingCategory?.parentCategoryId != null,
            name = uiState.dialogName,
            icon = uiState.dialogIcon,
            color = uiState.dialogColor,
            onNameChange = viewModel::updateDialogName,
            onIconChange = viewModel::updateDialogIcon,
            onColorChange = viewModel::updateDialogColor,
            onSave = viewModel::saveCategory,
            onDelete = if (uiState.editingCategory != null) {
                { categoryToDelete = uiState.editingCategory }
            } else null,
            onDismiss = viewModel::hideDialog
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete Category") },
            text = {
                Text("Are you sure you want to delete \"${category.name}\"? Transactions using this category will become uncategorized.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        categoryToDelete = null
                        viewModel.hideDialog()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(
    category: Category,
    hasSubcategories: Boolean = false,
    isExpanded: Boolean = false,
    onToggleExpand: () -> Unit = {},
    onClick: () -> Unit,
    onAddSubcategory: () -> Unit = {},
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Expand/collapse button for categories that have or can have subcategories
            IconButton(
                onClick = { onToggleExpand() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = if (hasSubcategories)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            CategoryIcon(
                icon = category.icon,
                color = category.color,
                size = 30.dp,
                iconSize = 15.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            // Add subcategory button
            IconButton(
                onClick = { onAddSubcategory() },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = "Add Subcategory",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(15.dp)
                )
            }

            // Drag handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = dragHandleModifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SubcategoryListItem(
    category: Category,
    onClick: () -> Unit,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 32.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.SubdirectoryArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(15.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            CategoryIcon(
                icon = category.icon,
                color = category.color,
                size = 26.dp,
                iconSize = 13.dp
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            // Drag handle
            Icon(
                Icons.Default.DragHandle,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = dragHandleModifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CategoryDialog(
    isEditing: Boolean,
    isSubcategory: Boolean = false,
    name: String,
    icon: String,
    color: Color,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Color) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val title = when {
        isEditing && isSubcategory -> "Edit Subcategory"
        isEditing -> "Edit Category"
        isSubcategory -> "New Subcategory"
        else -> "New Category"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CategoryIcon(
                        icon = icon,
                        color = color,
                        size = 64.dp,
                        iconSize = 32.dp
                    )
                }

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Icon Selector
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AvailableIcons) { iconName ->
                        val isSelected = iconName == icon
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) color.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        2.dp,
                                        color,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable { onIconChange(iconName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getIconForName(iconName),
                                contentDescription = null,
                                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Color Selector
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AvailableColors) { colorOption ->
                        val isSelected = colorOption == color
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colorOption)
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.onSurface,
                                        CircleShape
                                    ) else Modifier
                                )
                                .clickable { onColorChange(colorOption) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
