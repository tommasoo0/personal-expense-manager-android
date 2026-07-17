package com.expenses.mobile.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReferenceDao {

    @Query("SELECT name FROM categories ORDER BY lower(name)")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT name FROM accounts ORDER BY lower(name)")
    fun observeAccounts(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("DELETE FROM categories WHERE name = :name")
    suspend fun deleteCategoryByName(name: String)

    @Query("DELETE FROM accounts WHERE name = :name")
    suspend fun deleteAccountByName(name: String)

    @Query("UPDATE transactions SET category = :newName WHERE category = :oldName")
    suspend fun updateTransactionCategory(oldName: String, newName: String)

    @Query("UPDATE transactions SET account = :newName WHERE account = :oldName")
    suspend fun updateTransactionAccount(oldName: String, newName: String)

    @Transaction
    suspend fun renameCategory(oldName: String, newName: String) {
        insertCategory(CategoryEntity(newName))
        updateTransactionCategory(oldName, newName)
        deleteCategoryByName(oldName)
    }

    @Transaction
    suspend fun renameAccount(oldName: String, newName: String) {
        insertAccount(AccountEntity(newName))
        updateTransactionAccount(oldName, newName)
        deleteAccountByName(oldName)
    }
}
