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
import androidx.lifecycle.lifecycleScope

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.PetRepository

import kotlinx.coroutines.launch

import java.io.File


class EditPetActivity : AppCompatActivity() {

    // =========================================================
    // USER / PET IDs
    // =========================================================

    private var userId: Long = -1L

    private var petId: Int = -1


    // =========================================================
    // EXISTING PET
    // =========================================================

    private var existingPet: Pet? = null


    // =========================================================
    // PHOTO
    // =========================================================

    private var selectedPhotoUri: Uri? = null

    private var cameraPhotoUri: Uri? = null

    private var cameraPhotoFile: File? = null


    // =========================================================
    // REPOSITORY
    // =========================================================

    private lateinit var repository: PetRepository


    // =========================================================
    // HEADER
    // =========================================================

    private lateinit var formTitleText: TextView

    private lateinit var formSubtitleText: TextView


    // =========================================================
    // FORM
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


    // =========================================================
    // SAVE BUTTON
    // =========================================================

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


            // -------------------------------------------------
            // Keep permission for the selected image
            // -------------------------------------------------

            try {

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

            } catch (_: SecurityException) {

                /*
                 * Some gallery/document providers do not
                 * support persistable URI permissions.
                 *
                 * The image can still be used for the
                 * current session.
                 */
            }


            // -------------------------------------------------
            // Store selected URI
            // -------------------------------------------------

            selectedPhotoUri =
                uri


            // -------------------------------------------------
            // Show selected image
            // -------------------------------------------------

            showSelectedPhoto(
                uri
            )


            photoText.text =
                "New photo selected"
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

                // ---------------------------------------------
                // Store camera URI
                // ---------------------------------------------

                selectedPhotoUri =
                    cameraPhotoUri


                // ---------------------------------------------
                // Show camera photo
                // ---------------------------------------------

                cameraPhotoUri?.let { uri ->

                    showSelectedPhoto(
                        uri
                    )
                }


                photoText.text =
                    "New photo selected"

            } else {

                // ---------------------------------------------
                // Camera cancelled / failed
                // ---------------------------------------------

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
        // GET USER ID
        // -----------------------------------------------------

        userId =
            intent.getLongExtra(
                "user_id",
                -1L
            )


        // -----------------------------------------------------
        // GET PET ID
        // -----------------------------------------------------

        petId =
            intent.getIntExtra(
                "pet_id",
                -1
            )


        // -----------------------------------------------------
        // VALIDATE IDs
        // -----------------------------------------------------

        if (
            userId == -1L ||
            petId == -1
        ) {

            Toast.makeText(
                this,
                "Unable to identify pet",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // -----------------------------------------------------
        // USE SAME FORM AS ADD PET
        // -----------------------------------------------------

        setContentView(
            R.layout.activity_add_pet
        )


        // -----------------------------------------------------
        // DATABASE
        // -----------------------------------------------------

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )


        repository =
            PetRepository(
                database.petDao()
            )


        // -----------------------------------------------------
        // INITIALISE
        // -----------------------------------------------------

        initialiseViews()

        setupSpeciesSpinner()

        setupListeners()

        loadPet()
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        formTitleText =
            findViewById(
                R.id.formTitleText
            )


        formSubtitleText =
            findViewById(
                R.id.formSubtitleText
            )


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


        // -----------------------------------------------------
        // IMPORTANT PHOTO VIEW
        // -----------------------------------------------------

        photoPreview =
            findViewById(
                R.id.photoPreview
            )


        // -----------------------------------------------------
        // EDIT SCREEN TEXT
        // -----------------------------------------------------

        formTitleText.text =
            "Edit pet"


        formSubtitleText.text =
            "Update your pet's profile"


        savePetButton.text =
            "Update pet"


        // -----------------------------------------------------
        // INITIAL PHOTO STATE
        // -----------------------------------------------------

        photoPreview.visibility =
            View.GONE
    }


    // =========================================================
    // SHOW PHOTO
    // =========================================================

    private fun showSelectedPhoto(
        uri: Uri
    ) {

        try {

            // -------------------------------------------------
            // Put image into the ImageView
            // -------------------------------------------------

            photoPreview.setImageURI(
                uri
            )


            // -------------------------------------------------
            // Make ImageView visible
            // -------------------------------------------------

            photoPreview.visibility =
                View.VISIBLE


            // -------------------------------------------------
            // Fill the green square
            // -------------------------------------------------

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
                "Unable to display pet photo",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // =========================================================
    // SPECIES SPINNER
    // =========================================================

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


    // =========================================================
    // LISTENERS
    // =========================================================

    private fun setupListeners() {

        // -----------------------------------------------------
        // BACK
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.backButton
        ).setOnClickListener {

            finish()
        }


        // -----------------------------------------------------
        // PHOTO
        // -----------------------------------------------------

        findViewById<View>(
            R.id.choosePhotoButton
        ).setOnClickListener {

            showPhotoOptions()
        }


        // -----------------------------------------------------
        // UPDATE
        // -----------------------------------------------------

        savePetButton.setOnClickListener {

            updatePet()
        }


        // -----------------------------------------------------
        // HOME
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            openActivity(
                DashboardActivity::class.java
            )
        }


        // -----------------------------------------------------
        // PETS
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            openActivity(
                PetsActivity::class.java
            )
        }


        // -----------------------------------------------------
        // CHECKLIST
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            openActivity(
                ChecklistActivity::class.java
            )
        }


        // -----------------------------------------------------
        // PLACES
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {

            openActivity(
                PlacesActivity::class.java
            )
        }


        // -----------------------------------------------------
        // SPEND
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            openActivity(
                SpendingActivity::class.java
            )
        }
    }


    // =========================================================
    // PHOTO OPTIONS
    // =========================================================

    private fun showPhotoOptions() {

        AlertDialog.Builder(
            this
        )
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

                    0 -> openCamera()

                    1 -> openGallery()
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

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) ==
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
        // CREATE PETCARE DIRECTORY
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
    // LOAD EXISTING PET
    // =========================================================

    private fun loadPet() {

        lifecycleScope.launch {

            val pet =
                repository.getPetByIdForUser(
                    petId,
                    userId
                )


            // -------------------------------------------------
            // PET NOT FOUND
            // -------------------------------------------------

            if (pet == null) {

                Toast.makeText(
                    this@EditPetActivity,
                    "Pet not found",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

                return@launch
            }


            // -------------------------------------------------
            // SAVE EXISTING PET
            // -------------------------------------------------

            existingPet =
                pet


            // =================================================
            // LOAD EXISTING PHOTO
            // =================================================

            selectedPhotoUri =
                if (
                    pet.photoUri.isNullOrBlank()
                ) {

                    null

                } else {

                    try {

                        Uri.parse(
                            pet.photoUri
                        )

                    } catch (
                        exception: Exception
                    ) {

                        null
                    }
                }


            // -------------------------------------------------
            // DISPLAY EXISTING PHOTO
            // -------------------------------------------------

            if (
                selectedPhotoUri != null
            ) {

                showSelectedPhoto(
                    selectedPhotoUri!!
                )

                photoText.text =
                    "Current photo selected"

            } else {

                photoPreview.visibility =
                    View.GONE

                photoText.text =
                    "Choose photo"
            }


            // =================================================
            // LOAD FORM DATA
            // =================================================

            nameEditText.setText(
                pet.name
            )


            breedEditText.setText(
                pet.breed
            )


            ageEditText.setText(
                pet.age
            )


            weightEditText.setText(
                pet.weight
            )


            dietEditText.setText(
                pet.dietaryPreferences
            )


            allergiesEditText.setText(
                pet.allergies
            )


            vaccinationEditText.setText(
                pet.vaccinationHistory
            )


            toysEditText.setText(
                pet.favouriteToys
            )


            notesEditText.setText(
                pet.notes
            )


            // =================================================
            // SELECT SPECIES
            // =================================================

            val adapter =
                speciesSpinner.adapter
                        as ArrayAdapter<String>


            val position =
                adapter.getPosition(
                    pet.species
                )


            if (
                position >= 0
            ) {

                speciesSpinner.setSelection(
                    position
                )
            }
        }
    }


    // =========================================================
    // UPDATE PET
    // =========================================================

    private fun updatePet() {

        val pet =
            existingPet
                ?: return


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


        if (
            species.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please select a species",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // =====================================================
        // CREATE UPDATED PET
        // =====================================================

        val updatedPet =
            pet.copy(

                name =
                    name,

                species =
                    species,

                breed =
                    breedEditText.text
                        .toString()
                        .trim(),

                age =
                    ageEditText.text
                        .toString()
                        .trim(),

                weight =
                    weightEditText.text
                        .toString()
                        .trim(),

                dietaryPreferences =
                    dietEditText.text
                        .toString()
                        .trim(),

                allergies =
                    allergiesEditText.text
                        .toString()
                        .trim(),

                vaccinationHistory =
                    vaccinationEditText.text
                        .toString()
                        .trim(),

                favouriteToys =
                    toysEditText.text
                        .toString()
                        .trim(),

                notes =
                    notesEditText.text
                        .toString()
                        .trim(),


                // -------------------------------------------------
                // PHOTO
                //
                // If user selected a new photo:
                //     save the new URI.
                //
                // If user did NOT select a new photo:
                //     keep the old URI.
                // -------------------------------------------------

                photoUri =
                    selectedPhotoUri?.toString()
                        ?: pet.photoUri
            )


        // -----------------------------------------------------
        // DISABLE UPDATE BUTTON
        // -----------------------------------------------------

        savePetButton.isEnabled =
            false


        savePetButton.text =
            "Updating..."


        // =====================================================
        // DATABASE UPDATE
        // =====================================================

        lifecycleScope.launch {

            try {

                repository.updatePet(
                    updatedPet
                )


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                Toast.makeText(
                    this@EditPetActivity,
                    "Pet updated successfully",
                    Toast.LENGTH_SHORT
                ).show()


                // -------------------------------------------------
                // RETURN TO PET DETAILS
                // -------------------------------------------------

                val intent =
                    Intent(
                        this@EditPetActivity,
                        PetDetailsActivity::class.java
                    )


                intent.putExtra(
                    "user_id",
                    userId
                )


                intent.putExtra(
                    "pet_id",
                    petId
                )


                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP


                startActivity(
                    intent
                )


                finish()

            } catch (
                exception: Exception
            ) {

                // -------------------------------------------------
                // RESTORE BUTTON
                // -------------------------------------------------

                savePetButton.isEnabled =
                    true


                savePetButton.text =
                    "Update pet"


                Toast.makeText(
                    this@EditPetActivity,
                    exception.message
                        ?: "Unable to update pet",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this,
                activityClass
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