package com.example.prexam.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_drugs")
data class DrugEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val smiles: String,
    val formula: String,
    val iupacName: String,
    val mw: Double,
    val logP: Double,
    val hBondDonors: Int,
    val hBondAcceptors: Int,
    val tpsa: Double,
    val rotatableBonds: Int,
    val lipinskiPasses: Boolean,
    val veberPasses: Boolean,
    val oralBioavailability: String,
    val oralBioavailabilitySafe: Boolean,
    val bbbPenetration: String,
    val bbbPenetrationSafe: Boolean,
    val hergInhibition: String,
    val hergInhibitionSafe: Boolean,
    val predictionsJson: String, // JSON array of predictions
    val timestamp: Long = System.currentTimeMillis()
)
