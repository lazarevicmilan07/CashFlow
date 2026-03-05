package com.moneytracker.simplebudget.di

import android.content.Context
import androidx.room.Room
import com.moneytracker.simplebudget.data.local.ExpenseDatabase
import com.moneytracker.simplebudget.data.local.dao.AccountDao
import com.moneytracker.simplebudget.data.local.dao.CategoryDao
import com.moneytracker.simplebudget.data.local.dao.ExpenseDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideExpenseDatabase(
        @ApplicationContext context: Context
    ): ExpenseDatabase {
        return Room.databaseBuilder(
            context,
            ExpenseDatabase::class.java,
            ExpenseDatabase.DATABASE_NAME
        )
            .addMigrations(
                ExpenseDatabase.MIGRATION_1_2,
                ExpenseDatabase.MIGRATION_2_3,
                ExpenseDatabase.MIGRATION_3_4,
                ExpenseDatabase.MIGRATION_4_5,
                ExpenseDatabase.MIGRATION_5_6
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideExpenseDao(database: ExpenseDatabase): ExpenseDao {
        return database.expenseDao()
    }

    @Provides
    @Singleton
    fun provideCategoryDao(database: ExpenseDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    @Singleton
    fun provideAccountDao(database: ExpenseDatabase): AccountDao {
        return database.accountDao()
    }
}
