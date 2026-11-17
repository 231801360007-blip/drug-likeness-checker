package com.example.prexam.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDao {

    @Query("SELECT * FROM saved_drugs ORDER BY timestamp DESC")
    fun getAllDrugs(): Flow<List<DrugEntity>>

    @Query("SELECT * FROM saved_drugs WHERE id = :id")
    suspend fun getDrugById(id: Long): DrugEntity?

    @Query("SELECT * FROM saved_drugs WHERE id IN (:ids)")
    suspend fun getDrugsByIds(ids: List<Long>): List<DrugEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugEntity): Long

    @Delete
    suspend fun deleteDrug(drug: DrugEntity)

    @Query("DELETE FROM saved_drugs WHERE id = :id")
    suspend fun deleteDrugById(id: Long)
}
