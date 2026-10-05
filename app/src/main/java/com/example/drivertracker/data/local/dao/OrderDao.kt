package com.example.drivertracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.drivertracker.data.local.entity.OrderRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    @Query("SELECT * FROM order_records ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderRecord>>

    @Query("SELECT * FROM order_records")
    suspend fun getAllOrdersList(): List<OrderRecord>

    @Query("SELECT * FROM order_records WHERE id = :id")
    fun getOrderById(id: Long): Flow<OrderRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderRecord): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderRecord>): List<Long>

    @Update
    suspend fun updateOrder(order: OrderRecord)

    @Query("DELETE FROM order_records WHERE id = :id")
    suspend fun deleteOrder(id: Long)

    @Query("DELETE FROM order_records")
    suspend fun deleteAllOrders()
}
