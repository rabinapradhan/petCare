package com.example.petcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.viewmodel.AddPetState
import com.example.petcare.viewmodel.AddPetViewModel

import kotlinx.coroutines.launch

import java.io.File


class AddPetActivity : AppCompatActivity() {

    // =========================================================
    // VIEW MODEL
    // =========================================================

    private lateinit var viewModel: AddPetViewModel


    // =========================================================
    // USER
    // =========================================================

    private var userId: Long = -1L

    private var source: String = "dashboard"


    // =========================================================
    // PHOTO
    // =========================================================

    private var selectedPhotoUri: Uri? = null

    private var cameraPhotoUri: Uri? = null

    private var cameraPhotoFile: File? = null


    // =========================================================
    // FORM VIEWS
    // =========================================================

    private lateinit var nameEditText: EditText

    private lateinit var speciesSpinner: Spinner

    private lateinit var breedEditText: EditText

    private lateinit var ageEditText: EditText

    private lateinit var weightEditText: EditText

    private lateinit var dietEditText: EditText

    private lateinit var allergiesEditText: EditText

    private lateinit var vaccinationEditText: EditText

    private lateinit var toysEditText: EditText

    private lateinit var notesEditText: EditText

    private lateinit var savePetButton: Button


    // =========================================================
    // PHOTO VIEWS
    // =========================================================

    private lateinit var photoText: TextView

    private lateinit var photoPreview: ImageView


    // =========================================================
    // GALLERY PICKER
    // =========================================================

    private val imagePicker =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri == null) {
                return@registerForActivityResult
            }

            try {

                /*
                 * Keep permission to read the selected image.
                 *
                 * This is important because the URI is saved
                 * in the database and needs to remain readable
                 * after leaving this Activity.
                 */
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            } catch (_: SecurityException) {

                /*
                 * Some gallery providers do not support
                 * persistable permissions.
                 *
                 * The image can still be used during
                 * the current session.
                 */
            }

            selectedPhotoUri = uri

            showSelectedPhoto(uri)

            photoText.text = "Photo selected"
        }


    // =========================================================
    // CAMERA
    // =========================================================

    private val cameraLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->

            if (
                success &&
                cameraPhotoUri != null
            ) {

                selectedPhotoUri =
                    cameraPhotoUri

                cameraPhotoUri?.let { uri ->

                    showSelectedPhoto(uri)
                }

                photoText.text =
                    "Photo selected"

            } else {

                /*
                 * Camera was cancelled or failed.
                 */

                cameraPhotoFile?.delete()

                cameraPhotoFile = null

                cameraPhotoUri = null
            }
        }


    // =========================================================
    // CAMERA PERMISSION
    // =========================================================

    private val cameraPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {

                launchCamera()

            } else {

                Toast.makeText(
                    this,
                    "Camera permission is required to take a photo",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        // -----------------------------------------------------
        // USER ID
        // -----------------------------------------------------

        userId =
            intent.getLongExtra(
                "user_id",
                -1L
            )


        // -----------------------------------------------------
        // SOURCE
        // -----------------------------------------------------

        source =
            intent.getStringExtra(
                "source"
            ) ?: "dashboard"


        // -----------------------------------------------------
        // VALIDATE USER
        // -----------------------------------------------------

        if (userId == -1L) {

            Toast.makeText(
                this,
                "Unable to identify user",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // -----------------------------------------------------
        // LAYOUT
        // -----------------------------------------------------

        setContentView(
            R.layout.activity_add_pet
        )


        // -----------------------------------------------------
        // INITIALISE
        // -----------------------------------------------------

        initialiseViewModel()

        initialiseViews()

        setupSpeciesSpinner()

        setupListeners()

        observeState()
    }


    // =========================================================
    // VIEW MODEL
    // =========================================================

    private fun initialiseViewModel() {

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )


        val repository =
            PetRepository(
                database.petDao()
            )


        viewModel =
            ViewModelProvider(
                this,
                AddPetViewModel.Factory(
                    repository
                )
            )[AddPetViewModel::class.java]
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        nameEditText =
            findViewById(
                R.id.nameEditText
            )


        speciesSpinner =
            findViewById(
                R.id.speciesSpinner
            )


        breedEditText =
            findViewById(
                R.id.breedEditText
            )


        ageEditText =
            findViewById(
                R.id.ageEditText
            )


        weightEditText =
            findViewById(
                R.id.weightEditText
            )


        dietEditText =
            findViewById(
                R.id.dietEditText
            )


        allergiesEditText =
            findViewById(
                R.id.allergiesEditText
            )


        vaccinationEditText =
            findViewById(
                R.id.vaccinationEditText
            )


        toysEditText =
            findViewById(
                R.id.toysEditText
            )


        notesEditText =
            findViewById(
                R.id.notesEditText
            )


        savePetButton =
            findViewById(
                R.id.savePetButton
            )


        photoText =
            findViewById(
                R.id.photoText
            )


        /*
         * IMPORTANT:
         *
         * activity_add_pet.xml must contain:
         *
         * @+id/photoPreview
         */

        photoPreview =
            findViewById(
                R.id.photoPreview
            )


        // -----------------------------------------------------
        // INITIAL PHOTO STATE
        // -----------------------------------------------------

        photoPreview.visibility =
            View.GONE
    }


    // =========================================================
    // SHOW SELECTED PHOTO
    // =========================================================

    private fun showSelectedPhoto(
        uri: Uri
    ) {

        try {

            /*
             * Set the selected image.
             */

            photoPreview.setImageURI(
                uri
            )


            /*
             * Make the ImageView visible.
             */

            photoPreview.visibility =
                View.VISIBLE


            /*
             * Crop the image so it fills
             * the photo box.
             */

            photoPreview.scaleType =
                ImageView.ScaleType.CENTER_CROP

        } catch (exception: Exception) {

            photoPreview.setImageDrawable(
                null
            )

            photoPreview.visibility =
                View.GONE


            Toast.makeText(
                this,
                "Unable to display selected photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }



    // SPECIES SPINNER


    private fun setupSpeciesSpinner() {

        val species =
            arrayOf(
                "Dog",
                "Cat",
                "Bird",
                "Rabbit",
                "Other"
            )


        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                species
            )


        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        speciesSpinner.adapter =
            adapter
    }


    // LISTENERS


    private fun setupListeners() {

        // BACK


        findViewById<View>(
            R.id.backButton
        ).setOnClickListener {

            finish()
        }



        // PHOTO


        findViewById<View>(
            R.id.choosePhotoButton
        ).setOnClickListener {

            showPhotoOptions()
        }


        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        savePetButton.setOnClickListener {

            savePet()
        }


        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            openDashboard()
        }


        // -----------------------------------------------------
        // PETS
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            openPets()
        }


        // -----------------------------------------------------
        // CHECKLIST
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Checklist coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }


        // -----------------------------------------------------
        // PLACES
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Places coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }


        // -----------------------------------------------------
        // SPEND
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Spend coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // PHOTO OPTIONS
    // =========================================================

    private fun showPhotoOptions() {

        AlertDialog.Builder(this)

            .setTitle(
                "Pet photo"
            )

            .setItems(
                arrayOf(
                    "📷 Take Photo",
                    "🖼️ Choose from Gallery"
                )
            ) { _, which ->

                when (which) {

                    0 -> {
                        openCamera()
                    }

                    1 -> {
                        openGallery()
                    }
                }
            }

            .show()
    }


    // =========================================================
    // OPEN GALLERY
    // =========================================================

    private fun openGallery() {

        imagePicker.launch(
            arrayOf(
                "image/*"
            )
        )
    }


    // =========================================================
    // OPEN CAMERA
    // =========================================================

    private fun openCamera() {

        val permission =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            )


        if (
            permission ==
            PackageManager.PERMISSION_GRANTED
        ) {

            launchCamera()

        } else {

            cameraPermissionLauncher.launch(
                Manifest.permission.CAMERA
            )
        }
    }


    // =========================================================
    // LAUNCH CAMERA
    // =========================================================

    private fun launchCamera() {

        val picturesDirectory =
            getExternalFilesDir(
                Environment.DIRECTORY_PICTURES
            )


        if (
            picturesDirectory == null
        ) {

            Toast.makeText(
                this,
                "Unable to access photo storage",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // -----------------------------------------------------
        // PETCARE PHOTO DIRECTORY
        // -----------------------------------------------------

        val petPhotosDirectory =
            File(
                picturesDirectory,
                "PetCare"
            )


        if (
            !petPhotosDirectory.exists()
        ) {

            if (
                !petPhotosDirectory.mkdirs()
            ) {

                Toast.makeText(
                    this,
                    "Unable to create photo directory",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }
        }


        // -----------------------------------------------------
        // CREATE PHOTO FILE
        // -----------------------------------------------------

        val photoFile =
            File(
                petPhotosDirectory,
                "pet_${System.currentTimeMillis()}.jpg"
            )


        cameraPhotoFile =
            photoFile


        // -----------------------------------------------------
        // CREATE FILE PROVIDER URI
        // -----------------------------------------------------

        cameraPhotoUri =
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )


        // -----------------------------------------------------
        // LAUNCH CAMERA
        // -----------------------------------------------------

        cameraPhotoUri?.let { uri ->

            cameraLauncher.launch(
                uri
            )
        }
    }


    // =========================================================
    // SAVE PET
    // =========================================================

    private fun savePet() {

        // -----------------------------------------------------
        // NAME
        // -----------------------------------------------------

        val name =
            nameEditText.text
                .toString()
                .trim()


        if (
            name.isEmpty()
        ) {

            nameEditText.error =
                "Please enter your pet's name"

            nameEditText.requestFocus()

            return
        }


        // -----------------------------------------------------
        // SPECIES
        // -----------------------------------------------------

        val species =
            speciesSpinner.selectedItem
                ?.toString()
                ?.trim()
                .orEmpty()


        // -----------------------------------------------------
        // OTHER FIELDS
        // -----------------------------------------------------

        val breed =
            breedEditText.text
                .toString()
                .trim()


        val age =
            ageEditText.text
                .toString()
                .trim()


        val weight =
            weightEditText.text
                .toString()
                .trim()


        val diet =
            dietEditText.text
                .toString()
                .trim()


        val allergies =
            allergiesEditText.text
                .toString()
                .trim()


        val vaccination =
            vaccinationEditText.text
                .toString()
                .trim()


        val toys =
            toysEditText.text
                .toString()
                .trim()


        val notes =
            notesEditText.text
                .toString()
                .trim()


        // -----------------------------------------------------
        // CREATE PET
        // -----------------------------------------------------

        val pet =
            Pet(

                userId =
                    userId,

                name =
                    name,

                species =
                    species,

                breed =
                    breed,

                age =
                    age,

                weight =
                    weight,

                dietaryPreferences =
                    diet,

                allergies =
                    allergies,

                vaccinationHistory =
                    vaccination,

                favouriteToys =
                    toys,

                notes =
                    notes,

                /*
                 * Save the URI string in Room.
                 *
                 * This allows PetsActivity,
                 * PetDetailsActivity and EditPetActivity
                 * to retrieve the same image later.
                 */

                photoUri =
                    selectedPhotoUri?.toString()
            )


        // SAVE THROUGH VIEW MODEL

        viewModel.savePet(
            pet
        )
    }



    // OBSERVE VIEW MODEL STATE

    private fun observeState() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.state.collect { state ->

                    when (state) {


                        // IDLE

                        AddPetState.Idle -> {

                            savePetButton.isEnabled =
                                true

                            savePetButton.text =
                                "Save pet"
                        }



                        // SAVING

                        AddPetState.Saving -> {

                            savePetButton.isEnabled =
                                false

                            savePetButton.text =
                                "Saving..."
                        }



                        // SUCCESS
                        is AddPetState.Success -> {

                            savePetButton.isEnabled =
                                true

                            savePetButton.text =
                                "Save pet"


                            Toast.makeText(
                                this@AddPetActivity,
                                "Pet saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()


                            openPets()
                        }



                        // ERROR

                        is AddPetState.Error -> {

                            savePetButton.isEnabled =
                                true

                            savePetButton.text =
                                "Save pet"


                            Toast.makeText(
                                this@AddPetActivity,
                                state.message,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }



    // OPEN DASHBOARD

    private fun openDashboard() {

        val intent =
            Intent(
                this@AddPetActivity,
                DashboardActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP


        startActivity(
            intent
        )


        finish()
    }


    // =========================================================
    // OPEN PETS
    // =========================================================

    private fun openPets() {

        val intent =
            Intent(
                this@AddPetActivity,
                PetsActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP


        startActivity(
            intent
        )


        finish()
    }
}