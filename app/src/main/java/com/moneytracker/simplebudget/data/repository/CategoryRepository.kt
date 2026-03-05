package com.moneytracker.simplebudget.data.repository

import com.moneytracker.simplebudget.data.local.dao.CategoryDao
import com.moneytracker.simplebudget.data.mapper.toDomain
import com.moneytracker.simplebudget.data.mapper.toEntity
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.DefaultCategories
import com.moneytracker.simplebudget.domain.model.DefaultSubcategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {

    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getRootCategories(): Flow<List<Category>> =
        categoryDao.getRootCategories().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getSubcategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getSubcategories(parentId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun hasSubcategories(categoryId: Long): Boolean =
        categoryDao.hasSubcategories(categoryId)

    suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    suspend fun getCategoryCount(): Int =
        categoryDao.getCategoryCount()

    suspend fun insertCategory(category: Category): Long {
        val withOrder = if (category.displayOrder == 0 && category.id == 0L) {
            val nextOrder = if (category.parentCategoryId != null) {
                categoryDao.getNextSubcategoryDisplayOrder(category.parentCategoryId)
            } else {
                categoryDao.getNextRootDisplayOrder()
            }
            category.copy(displayOrder = nextOrder)
        } else {
            category
        }
        return categoryDao.insertCategory(withOrder.toEntity())
    }

    suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    suspend fun updateCategoryOrders(categories: List<Category>) =
        categoryDao.updateCategories(categories.map { it.toEntity() })

    suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())

    suspend fun deleteCategoryById(id: Long) =
        categoryDao.deleteCategoryById(id)

    suspend fun initializeDefaultCategories() {
        if (!categoryDao.hasCategories()) {
            categoryDao.insertCategories(
                DefaultCategories.mapIndexed { index, cat ->
                    cat.copy(displayOrder = index).toEntity()
                }
            )

            // Seed default subcategories for each parent category
            for (defaultCategory in DefaultCategories) {
                val subcategoryDefs = DefaultSubcategories[defaultCategory.name] ?: continue
                val parentEntity = categoryDao.getCategoryByName(defaultCategory.name) ?: continue
                val subcategories = subcategoryDefs.mapIndexed { index, (name, icon, color) ->
                    Category(
                        name = name,
                        icon = icon,
                        color = color,
                        isDefault = true,
                        parentCategoryId = parentEntity.id,
                        displayOrder = index
                    ).toEntity()
                }
                categoryDao.insertCategories(subcategories)
            }
        }
    }

    suspend fun insertCategories(categories: List<Category>) =
        categoryDao.insertCategories(categories.map { it.toEntity() })

    suspend fun deleteAllCategories() =
        categoryDao.deleteAllCategories()

    suspend fun getAllCategoriesSync(): List<Category> =
        categoryDao.getAllCategoriesSync().map { it.toDomain() }
}
