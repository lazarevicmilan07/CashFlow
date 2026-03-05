package com.moneytracker.simplebudget.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.simplebudget.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId IS NULL ORDER BY displayOrder ASC, name ASC")
    fun getRootCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parentCategoryId = :parentId ORDER BY displayOrder ASC, name ASC")
    fun getSubcategories(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM categories WHERE parentCategoryId = :categoryId LIMIT 1)")
    suspend fun hasSubcategories(categoryId: Long): Boolean

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name AND parentCategoryId IS NULL LIMIT 1")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    @Query("SELECT * FROM categories ORDER BY displayOrder ASC, name ASC")
    suspend fun getAllCategoriesSync(): List<CategoryEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM categories LIMIT 1)")
    suspend fun hasCategories(): Boolean

    @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM categories WHERE parentCategoryId IS NULL")
    suspend fun getNextRootDisplayOrder(): Int

    @Query("SELECT COALESCE(MAX(displayOrder), -1) + 1 FROM categories WHERE parentCategoryId = :parentId")
    suspend fun getNextSubcategoryDisplayOrder(parentId: Long): Int
}
