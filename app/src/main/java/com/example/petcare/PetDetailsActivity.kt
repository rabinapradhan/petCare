package com.example.petcare

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Task
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.TaskRepository

import kotlinx.coroutines.launch


class PetDetailsActivity : AppCompatActivity() {

    // =========================================================
    // IDs
    // =========================================================

    private var userId: Long = -1L

    private var petId: Int = -1


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private lateinit var petRepository: PetRepository

    private lateinit var taskRepository: TaskRepository


    // =========================================================
    // PET VIEWS
    // =========================================================

    private lateinit var petPhotoImageView: ImageView

    private lateinit var petNameText: TextView

    private lateinit var petSubtitleText: TextView

    private lateinit var ageText: TextView

    private lateinit var weightText: TextView

    private lateinit var dietText: TextView

    private lateinit var allergiesText: TextView

    private lateinit var vaccinationsText: TextView

    private lateinit var toysText: TextView


    // =========================================================
    // TASK CONTAINER
    // =========================================================

    private lateinit var tasksContainer: LinearLayout


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
        // SET LAYOUT
        // -----------------------------------------------------

        setContentView(
            R.layout.activity_pet_details
        )


        // -----------------------------------------------------
        // DATABASE
        // -----------------------------------------------------

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )


        petRepository =
            PetRepository(
                database.petDao()
            )


        taskRepository =
            TaskRepository(
                database.taskDao()
            )


        // -----------------------------------------------------
        // INITIALISE
        // -----------------------------------------------------

        initialiseViews()

        setupListeners()

        loadPet()

        observeTasks()
    }


    // =========================================================
// REFRESH PET WHEN RETURNING FROM EDIT SCREEN
// =========================================================

    override fun onResume() {
        super.onResume()

        if (petId != -1) {
            loadPet()
        }
    }

    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        // -----------------------------------------------------
        // PET PHOTO
        // -----------------------------------------------------

        petPhotoImageView =
            findViewById(
                R.id.petPhotoImageView
            )


        // -----------------------------------------------------
        // PET NAME
        // -----------------------------------------------------

        petNameText =
            findViewById(
                R.id.petNameText
            )


        // -----------------------------------------------------
        // PET SUBTITLE
        // -----------------------------------------------------

        petSubtitleText =
            findViewById(
                R.id.petSubtitleText
            )


        // -----------------------------------------------------
        // AGE
        // -----------------------------------------------------

        ageText =
            findViewById(
                R.id.ageText
            )


        // -----------------------------------------------------
        // WEIGHT
        // -----------------------------------------------------

        weightText =
            findViewById(
                R.id.weightText
            )


        // -----------------------------------------------------
        // DIET
        // -----------------------------------------------------

        dietText =
            findViewById(
                R.id.dietText
            )


        // -----------------------------------------------------
        // ALLERGIES
        // -----------------------------------------------------

        allergiesText =
            findViewById(
                R.id.allergiesText
            )


        // -----------------------------------------------------
        // VACCINATIONS
        // -----------------------------------------------------

        vaccinationsText =
            findViewById(
                R.id.vaccinationsText
            )


        // -----------------------------------------------------
        // FAVOURITE TOYS
        // -----------------------------------------------------

        toysText =
            findViewById(
                R.id.toysText
            )


        // -----------------------------------------------------
        // TASK CONTAINER
        // -----------------------------------------------------

        tasksContainer =
            findViewById(
                R.id.tasksContainer
            )


        // -----------------------------------------------------
        // DEFAULT PHOTO
        // -----------------------------------------------------

        showDefaultPetPhoto()
    }


    // =========================================================
    // DEFAULT PET PHOTO
    // =========================================================

    private fun showDefaultPetPhoto() {

        petPhotoImageView.setImageResource(
            R.drawable.ic_pet_profile
        )


        petPhotoImageView.scaleType =
            ImageView.ScaleType.CENTER_INSIDE
    }


    // =========================================================
    // LOAD PET PHOTO
    // =========================================================

    private fun loadPetPhoto(
        photoUriString: String?
    ) {

        // -----------------------------------------------------
        // DEFAULT FIRST
        // -----------------------------------------------------

        showDefaultPetPhoto()


        // -----------------------------------------------------
        // NO PHOTO
        // -----------------------------------------------------

        if (
            photoUriString.isNullOrBlank()
        ) {

            return
        }


        try {

            // -------------------------------------------------
            // CONVERT STRING TO URI
            // -------------------------------------------------

            val uri =
                Uri.parse(
                    photoUriString
                )


            // -------------------------------------------------
            // VALIDATE URI
            // -------------------------------------------------

            if (
                uri.scheme.isNullOrBlank()
            ) {

                return
            }


            // -------------------------------------------------
            // DISPLAY SAVED PHOTO
            // -------------------------------------------------

            petPhotoImageView.setImageURI(
                uri
            )


            // -------------------------------------------------
            // CROP PHOTO INSIDE BOX
            // -------------------------------------------------

            petPhotoImageView.scaleType =
                ImageView.ScaleType.CENTER_CROP

        } catch (
            exception: Exception
        ) {

            // -------------------------------------------------
            // FALLBACK TO DEFAULT
            // -------------------------------------------------

            showDefaultPetPhoto()
        }
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private fun setupListeners() {

        // =====================================================
        // BACK
        // =====================================================

        findViewById<TextView>(
            R.id.backButton
        ).setOnClickListener {

            finish()
        }


        // =====================================================
        // SMS DELEGATION
        // =====================================================

        findViewById<TextView>(
            R.id.delegateButton
        ).setOnClickListener {

            openSmsDelegation()
        }


        // =====================================================
        // EDIT PET
        // =====================================================

        findViewById<ImageView>(
            R.id.editPetButton
        ).setOnClickListener {

            openEditPet()
        }


        // =====================================================
        // ADD TASK
        // =====================================================

        findViewById<TextView>(
            R.id.addTaskButton
        ).setOnClickListener {

            openAddTask()
        }


        // =====================================================
        // DELETE PET
        // =====================================================

        findViewById<LinearLayout>(
            R.id.deletePetButton
        ).setOnClickListener {

            showDeletePetConfirmation()
        }


        // =====================================================
        // BOTTOM NAVIGATION
        // =====================================================

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
    // OPEN GENERIC ACTIVITY
    // =========================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this@PetDetailsActivity,
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


    // =========================================================
    // OPEN SMS DELEGATION
    // =========================================================

    private fun openSmsDelegation() {

        val intent =
            Intent(
                this@PetDetailsActivity,
                SmsDelegationActivity::class.java
            )


        // -----------------------------------------------------
        // USER ID
        // -----------------------------------------------------

        intent.putExtra(
            "user_id",
            userId
        )


        // -----------------------------------------------------
        // PET ID
        // -----------------------------------------------------

        intent.putExtra(
            "pet_id",
            petId
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // OPEN ADD / EDIT TASK
    // =========================================================

    private fun openAddTask() {

        val intent =
            Intent(
                this@PetDetailsActivity,
                AddTaskActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        intent.putExtra(
            "pet_id",
            petId
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // OPEN EDIT PET
    // =========================================================

    private fun openEditPet() {

        val intent =
            Intent(
                this@PetDetailsActivity,
                EditPetActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        intent.putExtra(
            "pet_id",
            petId
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // LOAD PET
    // =========================================================

    private fun loadPet() {

        lifecycleScope.launch {

            try {

                val pet =
                    petRepository.getPetById(
                        petId
                    )


                // -------------------------------------------------
                // PET NOT FOUND
                // -------------------------------------------------

                if (
                    pet == null
                ) {

                    Toast.makeText(
                        this@PetDetailsActivity,
                        "Pet not found",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()

                    return@launch
                }


                // =================================================
                // PET PHOTO
                // =================================================

                loadPetPhoto(
                    pet.photoUri
                )


                // =================================================
                // BASIC INFORMATION
                // =================================================

                petNameText.text =
                    pet.name


                petSubtitleText.text =
                    "${pet.species} · ${pet.breed}"


                // =================================================
                // DETAILS
                // =================================================

                ageText.text =
                    pet.age


                weightText.text =
                    pet.weight


                dietText.text =
                    pet.dietaryPreferences


                allergiesText.text =
                    pet.allergies


                vaccinationsText.text =
                    pet.vaccinationHistory


                toysText.text =
                    pet.favouriteToys

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@PetDetailsActivity,
                    exception.message
                        ?: "Unable to load pet",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // OBSERVE TASKS
    // =========================================================

    private fun observeTasks() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                /*
                 * TaskRepository expects:
                 *
                 * userId FIRST
                 * petId SECOND
                 */

                taskRepository
                    .getTasksForPet(
                        userId,
                        petId
                    )
                    .collect { tasks ->

                        displayTasks(
                            tasks
                        )
                    }
            }
        }
    }


    // =========================================================
    // DISPLAY TASKS
    // =========================================================

    private fun displayTasks(
        tasks: List<Task>
    ) {

        // -----------------------------------------------------
        // CLEAR OLD TASKS
        // -----------------------------------------------------

        tasksContainer.removeAllViews()


        val noTasksText =
            findViewById<TextView>(
                R.id.noTasksText
            )


        // -----------------------------------------------------
        // NO TASKS
        // -----------------------------------------------------

        if (
            tasks.isEmpty()
        ) {

            noTasksText.visibility =
                TextView.VISIBLE


            noTasksText.text =
                "No tasks yet. Add feeding times, walks, grooming or medication."


            return
        }


        noTasksText.visibility =
            TextView.GONE


        // -----------------------------------------------------
        // DISPLAY TASKS
        // -----------------------------------------------------

        tasks.forEach { task ->

            val taskView =
                LayoutInflater
                    .from(this)
                    .inflate(
                        R.layout.item_task,
                        tasksContainer,
                        false
                    )


            // =================================================
            // TASK ICON
            // =================================================

            taskView
                .findViewById<TextView>(
                    R.id.taskIconText
                )
                .text =
                getTaskIcon(
                    task.category
                )


            // =================================================
            // TASK TITLE
            // =================================================

            val titleText =
                taskView.findViewById<TextView>(
                    R.id.taskTitleText
                )


            titleText.text =
                task.title


            // -------------------------------------------------
            // COMPLETED TASK
            // -------------------------------------------------

            if (
                task.completed
            ) {

                titleText.paintFlags =
                    titleText.paintFlags or
                            Paint.STRIKE_THRU_TEXT_FLAG


                titleText.alpha =
                    0.55f

            } else {

                titleText.paintFlags =
                    titleText.paintFlags and
                            Paint.STRIKE_THRU_TEXT_FLAG.inv()


                titleText.alpha =
                    1f
            }


            // =================================================
            // TASK DETAILS
            // =================================================

            taskView
                .findViewById<TextView>(
                    R.id.taskDetailsText
                )
                .text =
                "${task.category} · ${task.time} · ${task.repeats}"


            // =================================================
            // SUPPLIES
            // =================================================

            val suppliesText =
                taskView.findViewById<TextView>(
                    R.id.taskSuppliesText
                )


            if (
                task.supplies.isBlank()
            ) {

                suppliesText.visibility =
                    TextView.GONE

            } else {

                suppliesText.visibility =
                    TextView.VISIBLE


                suppliesText.text =
                    "Supplies: ${task.supplies}"
            }


            // =================================================
            // EDIT TASK
            // =================================================

            taskView
                .findViewById<ImageButton>(
                    R.id.editTaskButton
                )
                .setOnClickListener {

                    openEditTask(
                        task
                    )
                }


            // =================================================
            // DELETE TASK
            // =================================================

            taskView
                .findViewById<ImageButton>(
                    R.id.deleteTaskButton
                )
                .setOnClickListener {

                    showDeleteTaskConfirmation(
                        task
                    )
                }


            // -------------------------------------------------
            // ADD TO CONTAINER
            // -------------------------------------------------

            tasksContainer.addView(
                taskView
            )
        }
    }


    // =========================================================
    // OPEN EDIT TASK
    // =========================================================

    private fun openEditTask(
        task: Task
    ) {

        val intent =
            Intent(
                this@PetDetailsActivity,
                AddTaskActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        intent.putExtra(
            "pet_id",
            petId
        )


        intent.putExtra(
            "task_id",
            task.id
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // DELETE TASK CONFIRMATION
    // =========================================================

    private fun showDeleteTaskConfirmation(
        task: Task
    ) {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Delete task?"
            )
            .setMessage(
                "Are you sure you want to delete \"${task.title}\"?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deleteTask(
                    task
                )
            }
            .show()
    }


    // =========================================================
    // DELETE TASK
    // =========================================================

    private fun deleteTask(
        task: Task
    ) {

        lifecycleScope.launch {

            try {

                taskRepository.deleteTask(
                    task.id,
                    userId
                )


                Toast.makeText(
                    this@PetDetailsActivity,
                    "Task deleted",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@PetDetailsActivity,
                    exception.message
                        ?: "Unable to delete task",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // DELETE PET CONFIRMATION
    // =========================================================

    private fun showDeletePetConfirmation() {

        AlertDialog.Builder(
            this
        )
            .setTitle(
                "Delete pet?"
            )
            .setMessage(
                "Are you sure you want to delete this pet and its care tasks?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                deletePet()
            }
            .show()
    }


    // =========================================================
    // DELETE PET
    // =========================================================

    /*
     * Deletes the pet's care tasks first and then deletes
     * the pet.
     *
     * This prevents orphaned Task records from remaining
     * in the Room database.
     */

    private fun deletePet() {

        lifecycleScope.launch {

            try {

                // -------------------------------------------------
                // STEP 1
                // Delete all tasks belonging to this pet.
                // -------------------------------------------------

                taskRepository.deleteTasksForPet(
                    petId = petId,
                    userId = userId
                )


                // -------------------------------------------------
                // STEP 2
                // Delete the pet itself.
                // -------------------------------------------------

                petRepository.deletePet(
                    petId = petId,
                    userId = userId
                )


                // -------------------------------------------------
                // SUCCESS
                // -------------------------------------------------

                Toast.makeText(
                    this@PetDetailsActivity,
                    "Pet and its care tasks deleted",
                    Toast.LENGTH_SHORT
                ).show()


                // -------------------------------------------------
                // RETURN TO PETS SCREEN
                // -------------------------------------------------

                val intent =
                    Intent(
                        this@PetDetailsActivity,
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

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@PetDetailsActivity,
                    exception.message
                        ?: "Unable to delete pet",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // TASK ICON
    // =========================================================

    private fun getTaskIcon(
        category: String
    ): String {

        return when (
            category.lowercase()
        ) {

            "feeding" ->
                "🍴"

            "exercise" ->
                "♧"

            "grooming" ->
                "✦"

            "medication" ->
                "+"

            else ->
                "•"
        }
    }
}