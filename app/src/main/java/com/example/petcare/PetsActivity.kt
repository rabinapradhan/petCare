package com.example.petcare

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.PetRepository
import kotlinx.coroutines.launch

class PetsActivity : AppCompatActivity() {

    private var userId: Long = -1L

    private lateinit var repository: PetRepository

    private lateinit var petsContainer: LinearLayout
    private lateinit var emptyPetsText: TextView

    -------
    // COLORS
    -------

    private val green = Color.rgb(46, 132, 75)
    private val darkText = Color.rgb(25, 35, 28)
    private val secondaryText = Color.rgb(100, 115, 105)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --------------------------------------------------------
        // GET USER ID
        // --------------------------------------------------------

        userId = intent.getLongExtra(
            "user_id",
            -1L
        )

        if (userId == -1L) {
            finish()
            return
        }

        // --------------------------------------------------------
        // LAYOUT
        // --------------------------------------------------------

        setContentView(R.layout.activity_pets)

        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        val database = PetCareDatabase.getDatabase(
            applicationContext
        )

        repository = PetRepository(
            database.petDao()
        )

        // --------------------------------------------------------
        // VIEWS
        // --------------------------------------------------------

        petsContainer = findViewById(
            R.id.petsContainer
        )

        emptyPetsText = findViewById(
            R.id.emptyPetsText
        )

        // --------------------------------------------------------
        // LISTENERS
        // --------------------------------------------------------

        setupClickListeners()

        // --------------------------------------------------------
        // OBSERVE ROOM
        // --------------------------------------------------------

        observePets()
    }

    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private fun setupClickListeners() {

        // --------------------------------------------------------
        // ADD PET
        // --------------------------------------------------------

        findViewById<View>(
            R.id.addPetButton
        ).setOnClickListener {

            val intent = Intent(
                this@PetsActivity,
                AddPetActivity::class.java
            )

            intent.putExtra(
                "user_id",
                userId
            )

            intent.putExtra(
                "source",
                "pets"
            )

            startActivity(intent)
        }

        // --------------------------------------------------------
        // HOME
        // --------------------------------------------------------

        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            openDashboard()
        }

        // --------------------------------------------------------
        // PETS
        // --------------------------------------------------------

        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            // Already on Pets screen.
        }

        // --------------------------------------------------------
        // CHECKLIST
        // --------------------------------------------------------

        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            openChecklist()
        }

        // --------------------------------------------------------
        // PLACES
        // --------------------------------------------------------

        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {

            val intent = Intent(
                this@PetsActivity,
                PlacesActivity::class.java
            )

            intent.putExtra(
                "user_id",
                userId
            )

            startActivity(intent)

            finish()
        }

        // --------------------------------------------------------
        // SPEND
        // --------------------------------------------------------

        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            val intent = Intent(
                this@PetsActivity,
                SpendingActivity::class.java
            )

            intent.putExtra(
                "user_id",
                userId
            )

            startActivity(intent)
        }

        // --------------------------------------------------------
        // LOGOUT
        // --------------------------------------------------------

        findViewById<ImageView>(
            R.id.logoutButton
        ).setOnClickListener {

            finish()
        }
    }

    // ============================================================
    // OBSERVE PETS FROM ROOM
    // ============================================================

    private fun observePets() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                repository
                    .getPets(userId)
                    .collect { pets ->

                        displayPets(pets)
                    }
            }
        }
    }

    // ============================================================
    // DISPLAY PETS
    // ============================================================

    private fun displayPets(
        pets: List<Pet>
    ) {

        petsContainer.removeAllViews()

        // --------------------------------------------------------
        // NO PETS
        // --------------------------------------------------------

        if (pets.isEmpty()) {

            emptyPetsText.visibility =
                View.VISIBLE

            return
        }

        // --------------------------------------------------------
        // PETS EXIST
        // --------------------------------------------------------

        emptyPetsText.visibility =
            View.GONE

        pets.forEach { pet ->

            addPetCard(pet)
        }
    }

    // ============================================================
    // CREATE PET CARD
    // ============================================================

    private fun addPetCard(
        pet: Pet
    ) {

        // --------------------------------------------------------
        // CARD
        // --------------------------------------------------------

        val card = LinearLayout(this)

        card.orientation =
            LinearLayout.HORIZONTAL

        card.gravity =
            Gravity.CENTER_VERTICAL

        card.setPadding(
            18.dp,
            14.dp,
            14.dp,
            14.dp
        )

        card.background =
            ContextCompat.getDrawable(
                this,
                R.drawable.bg_pet_card
            )

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                88.dp
            )

        cardParams.bottomMargin =
            12.dp

        card.layoutParams =
            cardParams

        // --------------------------------------------------------
        // PET PHOTO
        // --------------------------------------------------------

        val petIcon =
            ImageView(this)

        petIcon.layoutParams =
            LinearLayout.LayoutParams(
                52.dp,
                52.dp
            )

        /*
         * Load the saved pet photo.
         *
         * If no photo exists, the default pet icon
         * will be displayed instead.
         */
        loadPetPhoto(
            petIcon,
            pet.photoUri
        )

        // --------------------------------------------------------
        // INFORMATION CONTAINER
        // --------------------------------------------------------

        val information =
            LinearLayout(this)

        information.orientation =
            LinearLayout.VERTICAL

        val informationParams =
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        informationParams.weight =
            1f

        informationParams.marginStart =
            14.dp

        information.layoutParams =
            informationParams

        // --------------------------------------------------------
        // PET NAME
        // --------------------------------------------------------

        val name =
            TextView(this)

        name.text =
            pet.name

        name.textSize =
            17f

        name.setTextColor(
            darkText
        )

        name.setTypeface(
            null,
            Typeface.BOLD
        )

        // --------------------------------------------------------
        // PET DETAILS
        // --------------------------------------------------------

        val details =
            TextView(this)

        details.text =
            buildPetSubtitle(pet)

        details.textSize =
            13f

        details.setTextColor(
            secondaryText
        )

        details.setPadding(
            0,
            3.dp,
            0,
            0
        )

        information.addView(
            name
        )

        information.addView(
            details
        )

        // --------------------------------------------------------
        // CHEVRON
        // --------------------------------------------------------

        val arrow =
            TextView(this)

        arrow.text =
            "›"

        arrow.textSize =
            30f

        arrow.gravity =
            Gravity.CENTER

        arrow.setTextColor(
            Color.rgb(
                70,
                85,
                74
            )
        )

        arrow.layoutParams =
            LinearLayout.LayoutParams(
                40.dp,
                50.dp
            )

        // --------------------------------------------------------
        // ADD CHILD VIEWS
        // --------------------------------------------------------

        card.addView(
            petIcon
        )

        card.addView(
            information
        )

        card.addView(
            arrow
        )

        // --------------------------------------------------------
        // CLICK CARD
        // --------------------------------------------------------

        card.setOnClickListener {

            openPetDetails(
                pet.id
            )
        }

        // --------------------------------------------------------
        // ADD CARD TO CONTAINER
        // --------------------------------------------------------

        petsContainer.addView(
            card
        )
    }

    // ============================================================
    // LOAD PET PHOTO
    // ============================================================

    private fun loadPetPhoto(
        imageView: ImageView,
        photoUriString: String?
    ) {

        // --------------------------------------------------------
        // DEFAULT PHOTO
        // --------------------------------------------------------

        imageView.setImageResource(
            R.drawable.ic_pet_profile
        )

        imageView.scaleType =
            ImageView.ScaleType.CENTER_INSIDE

        imageView.setPadding(
            8.dp,
            8.dp,
            8.dp,
            8.dp
        )

        // --------------------------------------------------------
        // NO SAVED PHOTO
        // --------------------------------------------------------

        if (photoUriString.isNullOrBlank()) {
            return
        }

        // --------------------------------------------------------
        // LOAD SAVED PHOTO
        // --------------------------------------------------------

        try {

            val uri =
                Uri.parse(
                    photoUriString
                )

            imageView.setImageURI(
                uri
            )

            imageView.scaleType =
                ImageView.ScaleType.CENTER_CROP

            imageView.setPadding(
                0,
                0,
                0,
                0
            )

        } catch (
            exception: Exception
        ) {

            // ----------------------------------------------------
            // FALLBACK TO DEFAULT ICON
            // ----------------------------------------------------

            imageView.setImageResource(
                R.drawable.ic_pet_profile
            )

            imageView.scaleType =
                ImageView.ScaleType.CENTER_INSIDE

            imageView.setPadding(
                8.dp,
                8.dp,
                8.dp,
                8.dp
            )
        }
    }

    // ============================================================
    // PET SUBTITLE
    // ============================================================

    private fun buildPetSubtitle(
        pet: Pet
    ): String {

        val species =
            pet.species.ifBlank {
                "Pet"
            }

        val breed =
            pet.breed.ifBlank {
                ""
            }

        val age =
            pet.age.ifBlank {
                ""
            }

        return listOf(
            species,
            breed,
            age
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                " · "
            )
    }

    // ============================================================
    // OPEN PET DETAILS
    // ============================================================

    private fun openPetDetails(
        petId: Int
    ) {

        val intent = Intent(
            this@PetsActivity,
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

        startActivity(intent)
    }

    // ============================================================
    // OPEN DASHBOARD
    // ============================================================

    private fun openDashboard() {

        val intent = Intent(
            this@PetsActivity,
            DashboardActivity::class.java
        )

        intent.putExtra(
            "user_id",
            userId
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }

    // ============================================================
    // OPEN CHECKLIST
    // ============================================================

    private fun openChecklist() {

        val intent = Intent(
            this@PetsActivity,
            ChecklistActivity::class.java
        )

        intent.putExtra(
            "user_id",
            userId
        )

        startActivity(intent)

        finish()
    }

    // ============================================================
    // DP EXTENSION
    // ============================================================

    private val Int.dp: Int
        get() =
            (
                    this *
                            resources.displayMetrics.density
                    ).toInt()
}