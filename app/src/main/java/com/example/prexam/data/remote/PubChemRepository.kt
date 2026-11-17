package com.example.prexam.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class PubChemCompound(
    val queryName: String,
    val smiles: String,
    val formula: String,
    val iupacName: String,
    val imageUrl: String
)

object PubChemRepository {

    suspend fun fetchCompoundByName(name: String): Result<PubChemCompound> = withContext(Dispatchers.IO) {
        try {
            val cleanName = name.trim()
            if (cleanName.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Drug name cannot be empty"))
            }

            val encodedName = URLEncoder.encode(cleanName, "UTF-8")
            val propertyUrlStr = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/$encodedName/property/CanonicalSMILES,ConnectivitySMILES,MolecularFormula,IUPACName/JSON"

            val connection = URL(propertyUrlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != 200) {
                return@withContext Result.failure(Exception("Compound '$name' not found on PubChem (HTTP ${connection.responseCode})"))
            }

            val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
            val rootObj = JSONObject(jsonText)

            val propertyTable = rootObj.getJSONObject("PropertyTable")
            val propertiesArr = propertyTable.getJSONArray("Properties")

            if (propertiesArr.length() == 0) {
                return@withContext Result.failure(Exception("No properties found for '$name'"))
            }

            val propObj = propertiesArr.getJSONObject(0)

            val smiles = when {
                propObj.has("ConnectivitySMILES") -> propObj.getString("ConnectivitySMILES")
                propObj.has("CanonicalSMILES") -> propObj.getString("CanonicalSMILES")
                propObj.has("SMILES") -> propObj.getString("SMILES")
                else -> ""
            }

            val formula = if (propObj.has("MolecularFormula")) propObj.getString("MolecularFormula") else "N/A"
            val iupacName = if (propObj.has("IUPACName")) propObj.getString("IUPACName") else cleanName

            if (smiles.isEmpty()) {
                return@withContext Result.failure(Exception("SMILES structure not found for '$name'"))
            }

            val imageUrl = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound/name/$encodedName/PNG"

            Result.success(
                PubChemCompound(
                    queryName = cleanName,
                    smiles = smiles,
                    formula = formula,
                    iupacName = iupacName,
                    imageUrl = imageUrl
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
