package com.example.prexam.data.local

import kotlinx.coroutines.flow.Flow

class DrugRepository(private val drugDao: DrugDao) {

    val allDrugs: Flow<List<DrugEntity>> = drugDao.getAllDrugs()

    suspend fun insertDrug(drug: DrugEntity): Long {
        return drugDao.insertDrug(drug)
    }

    suspend fun deleteDrug(drug: DrugEntity) {
        drugDao.deleteDrug(drug)
    }

    suspend fun deleteDrugById(id: Long) {
        drugDao.deleteDrugById(id)
    }

    suspend fun getDrugsByIds(ids: List<Long>): List<DrugEntity> {
        return drugDao.getDrugsByIds(ids)
    }
}
