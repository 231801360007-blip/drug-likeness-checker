package com.example.prexam

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.prexam.data.local.DrugEntity
import com.example.prexam.databinding.ActivityMainBinding
import com.example.prexam.ui.DrugAnalysisResult
import com.example.prexam.ui.MainViewModel
import com.example.prexam.ui.adapter.PredictionAdapter
import com.example.prexam.ui.adapter.SavedDrugAdapter
import com.example.prexam.ui.dialog.SideBySideCompareDialog
import com.example.prexam.util.FirebaseAuthHelper
import com.example.prexam.util.OcrResult
import com.example.prexam.util.TextRecognitionHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var predictionAdapter: PredictionAdapter
    private lateinit var savedDrugAdapter: SavedDrugAdapter

    // Camera launcher for ML Kit text recognition
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processOcrBitmap(bitmap)
        } else {
            Toast.makeText(this, "Camera capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery picker launcher for ML Kit text recognition
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            processOcrUri(uri)
        }
    }

    // Google Sign-In launcher for Firebase Authentication
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        FirebaseAuthHelper.handleGoogleSignInResult(
            completedTask = task,
            onSuccess = { user ->
                viewModel.setUser(user)
                Toast.makeText(this, "Welcome, ${user.displayName}!", Toast.LENGTH_SHORT).show()
                if (viewModel.searchResult.value == null) {
                    viewModel.searchDrug("Paracetamol")
                }
            },
            onFailure = { error ->
                Toast.makeText(this, "Google Sign-In failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        predictionAdapter = PredictionAdapter()
        binding.rvPredictions.layoutManager = LinearLayoutManager(this)
        binding.rvPredictions.adapter = predictionAdapter

        savedDrugAdapter = SavedDrugAdapter(
            onToggleSelect = { id -> viewModel.toggleCompareSelection(id) },
            onDelete = { drug -> viewModel.deleteDrug(drug) },
            onItemClick = { drug -> viewModel.searchDrug(drug.name) }
        )
        binding.rvSavedDrugs.layoutManager = LinearLayoutManager(this)
        binding.rvSavedDrugs.adapter = savedDrugAdapter
    }

    private fun setupListeners() {
        // Welcome Screen Sign-In Buttons
        binding.btnSignInGoogle.setOnClickListener {
            val client = FirebaseAuthHelper.getGoogleSignInClient(this)
            googleSignInLauncher.launch(client.signInIntent)
        }

        binding.btnContinueAsGuest.setOnClickListener {
            FirebaseAuthHelper.signInAsGuest(
                onSuccess = { guest ->
                    viewModel.setUser(guest)
                    Toast.makeText(this, "Welcome, Guest Scientist!", Toast.LENGTH_SHORT).show()
                    if (viewModel.searchResult.value == null) {
                        viewModel.searchDrug("Paracetamol")
                    }
                },
                onFailure = { e ->
                    Toast.makeText(this, "Guest entry error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Main App Screen Buttons
        binding.btnSearch.setOnClickListener {
            val query = binding.etDrugName.text?.toString().orEmpty()
            viewModel.searchDrug(query)
        }

        binding.btnScanCamera.setOnClickListener {
            cameraLauncher.launch(null)
        }

        binding.btnScanGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnSaveDrug.setOnClickListener {
            viewModel.saveCurrentDrugToHistory()
            Toast.makeText(this, "Drug saved to Room database", Toast.LENGTH_SHORT).show()
        }

        binding.btnCompareSelected.setOnClickListener {
            viewModel.compareSelectedDrugs { drug1, drug2 ->
                SideBySideCompareDialog.show(this, drug1, drug2)
            }
        }

        binding.btnAuthAction.setOnClickListener {
            FirebaseAuthHelper.signOut(this) {
                viewModel.setUser(null)
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentUser.collectLatest { user ->
                        if (user != null) {
                            // User is signed in -> Hide Sign-In screen, Show Main App Layout
                            binding.layoutWelcomeAuth.visibility = View.GONE
                            binding.layoutMainAppContent.visibility = View.VISIBLE

                            binding.tvUserName.text = user.displayName
                            binding.tvUserEmail.text = if (user.isGuest) "Guest Account" else user.email
                            if (!user.photoUrl.isNullOrBlank()) {
                                binding.ivUserAvatar.load(user.photoUrl) {
                                    crossfade(true)
                                }
                            } else {
                                binding.ivUserAvatar.setImageResource(android.R.drawable.ic_menu_myplaces)
                            }
                        } else {
                            // Not signed in -> Show Sign-In Screen, Hide Main App Layout
                            binding.layoutWelcomeAuth.visibility = View.VISIBLE
                            binding.layoutMainAppContent.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.isLoading.collectLatest { loading ->
                        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest { msg ->
                        if (!msg.isNullOrBlank()) {
                            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
                            viewModel.clearError()
                        }
                    }
                }

                launch {
                    viewModel.searchResult.collectLatest { result ->
                        if (result != null) {
                            displayAnalysisResult(result)
                        } else {
                            binding.cardResult.visibility = View.GONE
                        }
                    }
                }

                launch {
                    viewModel.savedDrugs.collectLatest { list ->
                        binding.tvEmptyHistory.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                        savedDrugAdapter.updateData(list, viewModel.selectedForCompare.value)
                    }
                }

                launch {
                    viewModel.selectedForCompare.collectLatest { selectedSet ->
                        val count = selectedSet.size
                        binding.btnCompareSelected.text = "Compare ($count/2)"
                        binding.btnCompareSelected.isEnabled = count == 2
                        savedDrugAdapter.updateData(viewModel.savedDrugs.value, selectedSet)
                    }
                }
            }
        }
    }

    private fun displayAnalysisResult(result: DrugAnalysisResult) {
        binding.cardResult.visibility = View.VISIBLE
        binding.tvResultDrugName.text = result.pubChem.queryName
        binding.tvResultIupac.text = "IUPAC: ${result.pubChem.iupacName}"
        binding.tvResultFormula.text = "Formula: ${result.pubChem.formula}"
        binding.tvResultSmiles.text = "SMILES: ${result.pubChem.smiles}"

        // Structure Image via Coil
        binding.ivStructureImage.load(result.pubChem.imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_report_image)
            error(android.R.drawable.ic_menu_report_image)
        }

        // Descriptors
        val d = result.descriptors
        binding.tvMw.text = "MW: ${d.mw} Da"
        binding.tvLogP.text = "LogP: ${d.logP}"
        binding.tvHbd.text = "HBD: ${d.hBondDonors}"
        binding.tvHba.text = "HBA: ${d.hBondAcceptors}"
        binding.tvTpsa.text = "TPSA: ${d.tpsa} Å²"
        binding.tvRotatableBonds.text = "Rotatable Bonds: ${d.rotatableBonds}"

        // Badges
        if (d.lipinskiPasses) {
            binding.tvLipinskiStatus.text = "Lipinski: PASS"
            binding.tvLipinskiStatus.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvLipinskiStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvLipinskiStatus.text = "Lipinski: FAIL"
            binding.tvLipinskiStatus.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvLipinskiStatus.setTextColor(Color.parseColor("#C62828"))
        }

        if (d.veberPasses) {
            binding.tvVeberStatus.text = "Veber: PASS"
            binding.tvVeberStatus.setBackgroundResource(R.drawable.bg_badge_pass)
            binding.tvVeberStatus.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            binding.tvVeberStatus.text = "Veber: FAIL"
            binding.tvVeberStatus.setBackgroundResource(R.drawable.bg_badge_fail)
            binding.tvVeberStatus.setTextColor(Color.parseColor("#C62828"))
        }

        // ADMET Chips (Green = Safe, Red = Risk)
        binding.chipOralBio.text = "Oral: ${d.oralBioavailability.label}"
        binding.chipOralBio.setBackgroundResource(
            if (d.oralBioavailability.isSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk
        )

        binding.chipBbb.text = "BBB: ${d.bbbPenetration.label}"
        binding.chipBbb.setBackgroundResource(
            if (d.bbbPenetration.isSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk
        )

        binding.chipHerg.text = "hERG: ${d.hergInhibition.label}"
        binding.chipHerg.setBackgroundResource(
            if (d.hergInhibition.isSafe) R.drawable.bg_chip_safe else R.drawable.bg_chip_risk
        )

        // Predictions
        predictionAdapter.updateItems(result.predictions)
    }

    private fun processOcrBitmap(bitmap: Bitmap) {
        Toast.makeText(this, "Scanning box with ML Kit...", Toast.LENGTH_SHORT).show()
        TextRecognitionHelper.recognizeTextFromBitmap(
            bitmap = bitmap,
            onSuccess = { ocrResult ->
                handleOcrResult(ocrResult)
            },
            onFailure = { e ->
                Toast.makeText(this, "ML Kit Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun processOcrUri(uri: Uri) {
        Toast.makeText(this, "Scanning box image with ML Kit...", Toast.LENGTH_SHORT).show()
        TextRecognitionHelper.recognizeTextFromUri(
            context = this,
            uri = uri,
            onSuccess = { ocrResult ->
                handleOcrResult(ocrResult)
            },
            onFailure = { e ->
                Toast.makeText(this, "ML Kit Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun handleOcrResult(ocrResult: OcrResult) {
        val candidates = ocrResult.candidateDrugNames
        if (candidates.isEmpty()) {
            Toast.makeText(this, "No text recognized on box", Toast.LENGTH_SHORT).show()
            return
        }

        if (candidates.size == 1) {
            val candidate = candidates[0]
            binding.etDrugName.setText(candidate)
            viewModel.searchDrug(candidate)
            Toast.makeText(this, "Recognized box text: $candidate", Toast.LENGTH_SHORT).show()
        } else {
            val itemsArr = candidates.toTypedArray()
            AlertDialog.Builder(this)
                .setTitle("Select Drug Name from Medicine Box")
                .setItems(itemsArr) { _, which ->
                    val selected = itemsArr[which]
                    binding.etDrugName.setText(selected)
                    viewModel.searchDrug(selected)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
