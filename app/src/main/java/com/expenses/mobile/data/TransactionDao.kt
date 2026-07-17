package com.expenses.mobile.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query(
        """
        SELECT *
        FROM transactions
        WHERE (:month IS NULL OR substr(date, 6, 2) = :month)
          AND (:year IS NULL OR substr(date, 1, 4) = :year)
          AND (:account IS NULL OR account = :account)
        ORDER BY date DESC, id DESC
        """
    )
    fun observeFiltered(month: String?, year: String?, account: String?): Flow<List<TransactionEntity>>

    @Query("SELECT DISTINCT substr(date, 1, 4) FROM transactions ORDER BY substr(date, 1, 4) DESC")
    fun observeYears(): Flow<List<String>>

    @Insert
    suspend fun insert(transaction: TransactionEntity)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)
}
