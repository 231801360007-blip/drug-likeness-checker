package com.example.prexam.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import biokt.DrugMLPredictor
import biokt.MolDescriptors
import biokt.Molecule
import biokt.Descriptors
import biokt.Prediction
import com.example.prexam.data.local.AppDatabase
import com.example.prexam.data.local.DrugEntity
import com.example.prexam.data.local.DrugRepository
import com.example.prexam.data.remote.PubChemCompound
import com.example.prexam.data.remote.PubChemRepository
import com.example.prexam.util.AppUser
import com.example.prexam.util.FirebaseAuthHelper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DrugAnalysisResult(
    val pubChem: PubChemCompound,
    val molecule: Molecule,
    val descriptors: Descriptors,
    val predictions: List<Prediction>
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DrugRepository(AppDatabase.getInstance(application).drugDao())
    private val gson = Gson()

    private val _currentUser = MutableStateFlow<AppUser?>(FirebaseAuthHelper.getCurrentUser(application))
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    private val _searchResult = MutableStateFlow<DrugAnalysisResult?>(null)
    val searchResult: StateFlow<DrugAnalysisResult?> = _searchResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _savedDrugs = MutableStateFlow<List<DrugEntity>>(emptyList())
    val savedDrugs: StateFlow<List<DrugEntity>> = _savedDrugs.asStateFlow()

    private val _selectedForCompare = MutableStateFlow<Set<Long>>(emptySet())
    val selectedForCompare: StateFlow<Set<Long>> = _selectedForCompare.asStateFlow()

    private val _comparisonResult = MutableStateFlow<Pair<DrugEntity, DrugEntity>?>(null)
    val comparisonResult: StateFlow<Pair<DrugEntity, DrugEntity>?> = _comparisonResult.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allDrugs.collect { drugs ->
                _savedDrugs.value = drugs
            }
        }
    }

    fun setUser(user: AppUser?) {
        _currentUser.value = user
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun searchDrug(name: String) {
        val query = name.trim()
        if (query.isEmpty()) {
            _errorMessage.value = "Please enter or scan a drug name"
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val pubChemResult = PubChemRepository.fetchCompoundByName(query)
            pubChemResult.fold(
                onSuccess = { compound ->
                    val molecule = Molecule(smiles = compound.smiles, name = compound.queryName)
                    val descriptors = MolDescriptors.calculate(molecule)
                    val predictions = DrugMLPredictor.fullAnalysis(molecule)

                    _searchResult.value = DrugAnalysisResult(
                        pubChem = compound,
                        molecule = molecule,
                        descriptors = descriptors,
                        predictions = predictions
                    )
                    _isLoading.value = false
                },
                onFailure = { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to fetch drug structure"
                }
            )
        }
    }

    fun saveCurrentDrugToHistory() {
        val result = _searchResult.value ?: return
        viewModelScope.launch {
            val entity = DrugEntity(
                name = result.pubChem.queryName,
                smiles = result.pubChem.smiles,
                formula = result.pubChem.formula,
                iupacName = result.pubChem.iupacName,
                mw = result.descriptors.mw,
                logP = result.descriptors.logP,
                hBondDonors = result.descriptors.hBondDonors,
                hBondAcceptors = result.descriptors.hBondAcceptors,
                tpsa = result.descriptors.tpsa,
                rotatableBonds = result.descriptors.rotatableBonds,
                lipinskiPasses = result.descriptors.lipinskiPasses,
                veberPasses = result.descriptors.veberPasses,
                oralBioavailability = result.descriptors.oralBioavailability.label,
                oralBioavailabilitySafe = result.descriptors.oralBioavailability.isSafe,
                bbbPenetration = result.descriptors.bbbPenetration.label,
                bbbPenetrationSafe = result.descriptors.bbbPenetration.isSafe,
                hergInhibition = result.descriptors.hergInhibition.label,
                hergInhibitionSafe = result.descriptors.hergInhibition.isSafe,
                predictionsJson = gson.toJson(result.predictions)
            )
            repository.insertDrug(entity)
        }
    }

    fun deleteDrug(drug: DrugEntity) {
        viewModelScope.launch {
            repository.deleteDrug(drug)
            val currentSelected = _selectedForCompare.value.toMutableSet()
            if (currentSelected.contains(drug.id)) {
                currentSelected.remove(drug.id)
                _selectedForCompare.value = currentSelected
            }
        }
    }

    fun toggleCompareSelection(drugId: Long) {
        val set = _selectedForCompare.value.toMutableSet()
        if (set.contains(drugId)) {
            set.remove(drugId)
        } else {
            if (set.size >= 2) {
                // Keep max 2
                set.clear()
            }
            set.add(drugId)
        }
        _selectedForCompare.value = set
    }

    fun compareSelectedDrugs(onComplete: (DrugEntity, DrugEntity) -> Unit) {
        val ids = _selectedForCompare.value.toList()
        if (ids.size != 2) {
            _errorMessage.value = "Select exactly 2 drugs from history to compare side-by-side"
            return
        }

        viewModelScope.launch {
            val drugs = repository.getDrugsByIds(ids)
            if (drugs.size == 2) {
                _comparisonResult.value = Pair(drugs[0], drugs[1])
                onComplete(drugs[0], drugs[1])
            } else {
                _errorMessage.value = "Could not load selected drugs for comparison"
            }
        }
    }

    fun deserializePredictions(json: String): List<Prediction> {
        return try {
            val type = object : TypeToken<List<Prediction>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
}
