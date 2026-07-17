package com.expenses.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val description: String,
    val amountCents: Long,
    val category: String,
    val type: String,
    val account: String
)
