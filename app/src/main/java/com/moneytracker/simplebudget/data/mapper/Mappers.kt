package com.moneytracker.simplebudget.data.mapper

import androidx.compose.ui.graphics.Color
import com.moneytracker.simplebudget.data.local.entity.AccountEntity
import com.moneytracker.simplebudget.data.local.entity.AccountWithBalanceEntity
import com.moneytracker.simplebudget.data.local.entity.CategoryEntity
import com.moneytracker.simplebudget.data.local.entity.ExpenseEntity
import com.moneytracker.simplebudget.domain.model.Account
import com.moneytracker.simplebudget.domain.model.AccountWithBalance
import com.moneytracker.simplebudget.domain.model.Category
import com.moneytracker.simplebudget.domain.model.Expense
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    color = Color(color.toULong()),
    isDefault = isDefault,
    parentCategoryId = parentCategoryId,
    createdAt = createdAt,
    displayOrder = displayOrder
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color.value.toLong(),
    isDefault = isDefault,
    parentCategoryId = parentCategoryId,
    createdAt = createdAt,
    displayOrder = displayOrder
)

fun ExpenseEntity.toDomain(): Expense = Expense(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    type = type,
    date = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate(),
    createdAt = createdAt
)

fun Expense.toEntity(): ExpenseEntity = ExpenseEntity(
    id = id,
    amount = amount,
    note = note,
    categoryId = categoryId,
    subcategoryId = subcategoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    type = type,
    date = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    createdAt = createdAt
)

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    icon = icon,
    color = Color(color.toULong()),
    initialBalance = initialBalance,
    isDefault = isDefault,
    createdAt = createdAt
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    icon = icon,
    color = color.value.toLong(),
    initialBalance = initialBalance,
    isDefault = isDefault,
    createdAt = createdAt
)

fun AccountWithBalanceEntity.toDomain(): AccountWithBalance = AccountWithBalance(
    account = account.toDomain(),
    currentBalance = currentBalance
)
