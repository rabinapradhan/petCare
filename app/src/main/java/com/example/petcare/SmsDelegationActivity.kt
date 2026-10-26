package com.example.petcare

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.entity.Task
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.TaskRepository

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class SmsDelegationActivity : AppCompatActivity() {


    // =========================================================
    // USER
    // =========================================================

    private var userId: Long = -1L


    // =========================================================
    // OPTIONAL PET ID
    // =========================================================

    /*
     * Checklist can open this screen without a pet_id.
     *
     * PetDetailsActivity can open this screen with a pet_id.
     *
     * Therefore pet_id is nullable.
     */

    private var preselectedPetId: Int? = null


    // =========================================================
    // SELECTED PET
    // =========================================================

    private var selectedPet: Pet? = null


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private lateinit var petRepository: PetRepository

    private lateinit var taskRepository: TaskRepository


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var petNameText: TextView

    private lateinit var petSubtitleText: TextView

    private lateinit var phoneNumberEditText: EditText

    private lateinit var tasksContainer: LinearLayout

    private lateinit var noTasksText: TextView

    private lateinit var messagePreviewText: TextView

    private lateinit var sendSmsButton: Button


    // =========================================================
    // TASK DATA
    // =========================================================

    private var todayTasks: List<Task> =
        emptyList()


    private val selectedTaskIds =
        mutableSetOf<Int>()


    /*
     * Stores the current task collector.
     *
     * When the user changes:
     *
     * Max → Luna
     *
     * the previous collector is cancelled.
     */

    private var tasksJob: Job? =
        null


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
        // GET OPTIONAL PET ID
        // -----------------------------------------------------

        preselectedPetId =
            if (
                intent.hasExtra(
                    "pet_id"
                )
            ) {

                intent.getIntExtra(
                    "pet_id",
                    -1
                ).takeIf {
                    it != -1
                }

            } else {

                null
            }


        // -----------------------------------------------------
        // VALIDATE USER
        // -----------------------------------------------------

        if (
            userId == -1L
        ) {

            Toast.makeText(
                this,
                "User information not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // -----------------------------------------------------
        // LAYOUT
        // -----------------------------------------------------

        setContentView(
            R.layout.activity_sms_delegation
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


        /*
         * Load all pets first.
         *
         * If pet_id was supplied,
         * that pet will be selected automatically.
         */

        loadPets()
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        petNameText =
            findViewById(
                R.id.petNameText
            )


        petSubtitleText =
            findViewById(
                R.id.petSubtitleText
            )


        phoneNumberEditText =
            findViewById(
                R.id.phoneNumberEditText
            )


        tasksContainer =
            findViewById(
                R.id.tasksContainer
            )


        noTasksText =
            findViewById(
                R.id.noTasksText
            )


        messagePreviewText =
            findViewById(
                R.id.messagePreviewText
            )


        sendSmsButton =
            findViewById(
                R.id.sendSmsButton
            )


        // -----------------------------------------------------
        // INITIAL TEXT
        // -----------------------------------------------------

        petNameText.text =
            "Select a pet"


        petSubtitleText.text =
            "Choose the pet whose care you want to delegate"


        messagePreviewText.text =
            "Select a pet and care tasks to create the SMS."


        // -----------------------------------------------------
        // IMPORTANT:
        // FORCE PREVIEW LEFT + TOP ALIGNMENT
        // -----------------------------------------------------

        messagePreviewText.gravity =
            Gravity.TOP or Gravity.START


        messagePreviewText.textAlignment =
            View.TEXT_ALIGNMENT_VIEW_START
    }


    // =========================================================
    // VALIDATE PHONE NUMBER
    // =========================================================

    private fun isValidPhoneNumber(
        phoneNumber: String
    ): Boolean {

        /*
         * Remove common formatting characters.
         *
         * Example:
         *
         * 98123-45678
         * becomes:
         *
         * 9812345678
         */

        val cleanedNumber =
            phoneNumber
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "")


        // -----------------------------------------------------
        // NEPAL MOBILE NUMBER
        // -----------------------------------------------------
        //
        // 98XXXXXXXX
        // 97XXXXXXXX
        //
        // Total = 10 digits
        // -----------------------------------------------------

        if (
            cleanedNumber.matches(
                Regex("^9[78]\\d{8}$")
            )
        ) {

            return true
        }


        // -----------------------------------------------------
        // INTERNATIONAL NEPAL FORMAT
        // -----------------------------------------------------
        //
        // +97798XXXXXXXX
        // +97797XXXXXXXX
        // -----------------------------------------------------

        if (
            cleanedNumber.matches(
                Regex("^\\+9779[78]\\d{8}$")
            )
        ) {

            return true
        }


        return false
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
        // PET SELECTION
        // -----------------------------------------------------

        petNameText.setOnClickListener {

            showPetSelectionDialog()
        }


        petSubtitleText.setOnClickListener {

            showPetSelectionDialog()
        }


        // -----------------------------------------------------
        // SEND SMS
        // -----------------------------------------------------

        sendSmsButton.setOnClickListener {

            sendSms()
        }
    }


    // =========================================================
    // LOAD USER'S PETS
    // =========================================================

    private fun loadPets() {

        lifecycleScope.launch {

            try {

                /*
                 * Existing repository method:
                 *
                 * getPets(userId)
                 */

                val pets =
                    petRepository
                        .getPets(
                            userId
                        )
                        .first()


                // -------------------------------------------------
                // NO PETS
                // -------------------------------------------------

                if (
                    pets.isEmpty()
                ) {

                    petNameText.text =
                        "No pets found"


                    petSubtitleText.text =
                        "Add a pet before delegating care."


                    noTasksText.visibility =
                        View.VISIBLE


                    noTasksText.text =
                        "No pets available."


                    messagePreviewText.text =
                        "No pet information is available."


                    return@launch
                }


                // -------------------------------------------------
                // PRESELECTED PET
                // -------------------------------------------------

                if (
                    preselectedPetId != null
                ) {

                    val pet =
                        pets.firstOrNull {

                            it.id ==
                                    preselectedPetId
                        }


                    if (
                        pet != null
                    ) {

                        selectPet(
                            pet
                        )

                        return@launch
                    }
                }


                // -------------------------------------------------
                // ONLY ONE PET
                // -------------------------------------------------

                if (
                    pets.size == 1
                ) {

                    selectPet(
                        pets.first()
                    )

                    return@launch
                }


                // -------------------------------------------------
                // MULTIPLE PETS
                // -----------------------------------------------------

                showPetSelectionDialog(
                    pets
                )

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@SmsDelegationActivity,
                    exception.message
                        ?: "Unable to load pets",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // PET SELECTION DIALOG
    // =========================================================

    private fun showPetSelectionDialog(
        pets: List<Pet>? = null
    ) {

        lifecycleScope.launch {

            try {

                val availablePets =
                    pets
                        ?: petRepository
                            .getPets(
                                userId
                            )
                            .first()


                // -------------------------------------------------
                // NO PETS
                // -------------------------------------------------

                if (
                    availablePets.isEmpty()
                ) {

                    Toast.makeText(
                        this@SmsDelegationActivity,
                        "No pets found. Add a pet first.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }


                // -------------------------------------------------
                // PET DISPLAY NAMES
                // -------------------------------------------------

                val petNames =
                    availablePets
                        .map { pet ->

                            if (
                                pet.breed.isBlank()
                            ) {

                                "${pet.name} · ${pet.species}"

                            } else {

                                "${pet.name} · ${pet.species} · ${pet.breed}"
                            }
                        }
                        .toTypedArray()


                // -------------------------------------------------
                // CURRENT SELECTION
                // -------------------------------------------------

                val checkedItem =
                    selectedPet?.let { selected ->

                        availablePets.indexOfFirst {

                            it.id ==
                                    selected.id
                        }

                    } ?: -1


                // -------------------------------------------------
                // DIALOG
                // -----------------------------------------------------

                AlertDialog.Builder(
                    this@SmsDelegationActivity
                )
                    .setTitle(
                        "Select a pet"
                    )
                    .setSingleChoiceItems(
                        petNames,
                        checkedItem
                    ) { dialog, which ->

                        val pet =
                            availablePets[
                                which
                            ]


                        selectPet(
                            pet
                        )


                        dialog.dismiss()
                    }
                    .setNegativeButton(
                        "Cancel",
                        null
                    )
                    .show()

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@SmsDelegationActivity,
                    exception.message
                        ?: "Unable to load pets",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // SELECT PET
    // =========================================================

    private fun selectPet(
        pet: Pet
    ) {

        selectedPet =
            pet


        // -----------------------------------------------------
        // DISPLAY PET
        // -----------------------------------------------------

        petNameText.text =
            pet.name


        petSubtitleText.text =
            if (
                pet.breed.isBlank()
            ) {

                pet.species

            } else {

                "${pet.species} · ${pet.breed}"
            }


        // -----------------------------------------------------
        // RESET TASK SELECTION
        // -----------------------------------------------------

        selectedTaskIds.clear()


        todayTasks =
            emptyList()


        // -----------------------------------------------------
        // UPDATE PREVIEW
        // -----------------------------------------------------

        messagePreviewText.text =
            buildMessagePreview()


        // -----------------------------------------------------
        // LOAD TASKS
        // -----------------------------------------------------

        observeTasksForPet(
            pet.id
        )
    }


    // =========================================================
    // OBSERVE SELECTED PET TASKS
    // =========================================================

    private fun observeTasksForPet(
        petId: Int
    ) {

        /*
         * Cancel previous collector.
         */

        tasksJob?.cancel()


        tasksJob =
            lifecycleScope.launch {

                try {

                    taskRepository
                        .getTasksForPet(
                            userId,
                            petId
                        )
                        .collect { tasks ->

                            todayTasks =
                                getTodayTasks(
                                    tasks
                                )


                            selectedTaskIds.clear()


                            renderTasks(
                                todayTasks
                            )
                        }

                } catch (
                    exception: Exception
                ) {

                    Toast.makeText(
                        this@SmsDelegationActivity,
                        exception.message
                            ?: "Unable to load care tasks",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }


    // =========================================================
    // GET TODAY'S APPLICABLE TASKS
    // =========================================================

    private fun getTodayTasks(
        tasks: List<Task>
    ): List<Task> {

        val todayCalendar =
            Calendar.getInstance()


        val today =
            todayCalendar.time


        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )


        return tasks.filter { task ->


            // -------------------------------------------------
            // START DATE
            // -------------------------------------------------

            val startDate =
                try {

                    dateFormat.parse(
                        task.startDate
                    )

                } catch (
                    exception: Exception
                ) {

                    null
                }


            // -------------------------------------------------
            // INVALID DATE
            // -------------------------------------------------

            if (
                startDate == null
            ) {

                return@filter true
            }


            // -------------------------------------------------
            // FUTURE TASK
            // -------------------------------------------------

            if (
                startDate.after(
                    today
                )
            ) {

                return@filter false
            }


            val startCalendar =
                Calendar.getInstance().apply {

                    time =
                        startDate
                }


            // -------------------------------------------------
            // REPEAT RULE
            // -------------------------------------------------

            when (
                task.repeats
            ) {

                "Every day" -> {

                    true
                }


                "Every week" -> {

                    startCalendar.get(
                        Calendar.DAY_OF_WEEK
                    ) ==
                            todayCalendar.get(
                                Calendar.DAY_OF_WEEK
                            )
                }


                "Every month" -> {

                    startCalendar.get(
                        Calendar.DAY_OF_MONTH
                    ) ==
                            todayCalendar.get(
                                Calendar.DAY_OF_MONTH
                            )
                }


                "Once" -> {

                    isSameDate(
                        startDate,
                        today
                    )
                }


                else -> {

                    false
                }
            }
        }
    }


    // =========================================================
    // COMPARE DATES
    // =========================================================

    private fun isSameDate(
        firstDate: Date,
        secondDate: Date
    ): Boolean {

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )


        return dateFormat.format(
            firstDate
        ) ==
                dateFormat.format(
                    secondDate
                )
    }


    // =========================================================
    // DISPLAY TASKS
    // =========================================================

    private fun renderTasks(
        tasks: List<Task>
    ) {

        tasksContainer.removeAllViews()


        // -----------------------------------------------------
        // NO TASKS
        // -----------------------------------------------------

        if (
            tasks.isEmpty()
        ) {

            noTasksText.visibility =
                View.VISIBLE


            noTasksText.text =
                "No care tasks are scheduled for this pet today."


            messagePreviewText.text =
                buildMessagePreview()


            return
        }


        noTasksText.visibility =
            View.GONE


        // -----------------------------------------------------
        // TASK CHECKBOXES
        // -----------------------------------------------------

        tasks
            .sortedBy {

                it.time
            }
            .forEach { task ->


                val checkBox =
                    CheckBox(this)


                checkBox.text =
                    buildTaskLabel(
                        task
                    )


                checkBox.textSize =
                    14f


                checkBox.setTextColor(
                    android.graphics.Color.rgb(
                        24,
                        35,
                        27
                    )
                )


                checkBox.gravity =
                    Gravity.CENTER_VERTICAL


                checkBox.setPadding(
                    dp(4),
                    dp(9),
                    dp(4),
                    dp(9)
                )


                checkBox.setOnCheckedChangeListener {
                        _,
                        isChecked ->


                    if (
                        isChecked
                    ) {

                        selectedTaskIds.add(
                            task.id
                        )

                    } else {

                        selectedTaskIds.remove(
                            task.id
                        )
                    }


                    messagePreviewText.text =
                        buildMessagePreview()


                    // Keep preview aligned after updating it.

                    messagePreviewText.gravity =
                        Gravity.TOP or Gravity.START


                    messagePreviewText.textAlignment =
                        View.TEXT_ALIGNMENT_VIEW_START
                }


                tasksContainer.addView(
                    checkBox
                )
            }


        // -----------------------------------------------------
        // INITIAL PREVIEW
        // -----------------------------------------------------

        messagePreviewText.text =
            buildMessagePreview()


        messagePreviewText.gravity =
            Gravity.TOP or Gravity.START


        messagePreviewText.textAlignment =
            View.TEXT_ALIGNMENT_VIEW_START
    }


    // =========================================================
    // TASK LABEL
    // =========================================================

    private fun buildTaskLabel(
        task: Task
    ): String {

        return buildString {

            if (
                task.time.isNotBlank()
            ) {

                append(
                    task.time
                )

                append(
                    "  ·  "
                )
            }


            append(
                task.title
            )


            if (
                task.category.isNotBlank()
            ) {

                append(
                    "  ·  ${task.category}"
                )
            }
        }
    }


    // =========================================================
    // BUILD SMS MESSAGE
    // =========================================================

    private fun buildMessagePreview(): String {

        val pet =
            selectedPet
                ?: return "Select a pet and care tasks to create the SMS."


        val selectedTasks =
            todayTasks
                .filter {

                    it.id in selectedTaskIds
                }
                .sortedBy {

                    it.time
                }


        // -----------------------------------------------------
        // NOTHING SELECTED
        // -----------------------------------------------------

        if (
            selectedTasks.isEmpty()
        ) {

            return buildString {

                append(
                    "PetCare\n"
                )

                append(
                    "Care instructions for ${pet.name}\n\n"
                )

                append(
                    "No tasks selected yet.\n"
                )

                append(
                    "Select the care tasks you want to delegate."
                )
            }
        }


        // -----------------------------------------------------
        // BUILD TASK LINES
        // -----------------------------------------------------

        val taskLines =
            selectedTasks.joinToString(
                separator = "\n"
            ) { task ->


                buildString {

                    // TIME

                    if (
                        task.time.isNotBlank()
                    ) {

                        append(
                            "• ${task.time} — ${task.title}"
                        )

                    } else {

                        append(
                            "• ${task.title}"
                        )
                    }


                    // CATEGORY

                    if (
                        task.category.isNotBlank()
                    ) {

                        append(
                            "\n  ${task.category}"
                        )
                    }


                    // REPEAT

                    if (
                        task.repeats.isNotBlank()
                    ) {

                        append(
                            " · ${task.repeats}"
                        )
                    }


                    // SUPPLIES

                    if (
                        task.supplies.isNotBlank()
                    ) {

                        append(
                            "\n  Supplies: ${task.supplies}"
                        )
                    }


                    // NOTES

                    if (
                        task.notes.isNotBlank()
                    ) {

                        append(
                            "\n  Notes: ${task.notes}"
                        )
                    }
                }
            }


        // -----------------------------------------------------
        // FINAL MESSAGE
        // -----------------------------------------------------

        return buildString {

            append(
                "PetCare\n"
            )

            append(
                "Care instructions for ${pet.name}\n\n"
            )

            append(
                taskLines
            )

            append(
                "\n\n"
            )

            append(
                "Please follow the care instructions above."
            )
        }
    }


    // =========================================================
    // SEND SMS
    // =========================================================

    private fun sendSms() {


        // -----------------------------------------------------
        // PHONE NUMBER
        // -----------------------------------------------------

        val phoneNumber =
            phoneNumberEditText.text
                .toString()
                .trim()


        // -----------------------------------------------------
        // EMPTY PHONE NUMBER
        // -----------------------------------------------------

        if (
            phoneNumber.isBlank()
        ) {

            phoneNumberEditText.error =
                "Enter a phone number"

            phoneNumberEditText.requestFocus()

            return
        }


        // -----------------------------------------------------
        // INVALID PHONE NUMBER
        // -----------------------------------------------------

        if (
            !isValidPhoneNumber(
                phoneNumber
            )
        ) {

            phoneNumberEditText.error =
                "Enter a valid mobile number"

            phoneNumberEditText.requestFocus()


            Toast.makeText(
                this,
                "Please enter a valid 10-digit Nepal mobile number.",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        // -----------------------------------------------------
        // CLEAR PREVIOUS ERROR
        // -----------------------------------------------------

        phoneNumberEditText.error =
            null


        // -----------------------------------------------------
        // PET
        // -----------------------------------------------------

        val pet =
            selectedPet


        if (
            pet == null
        ) {

            Toast.makeText(
                this,
                "Please select a pet first",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        // -----------------------------------------------------
        // TASKS
        // -----------------------------------------------------

        if (
            selectedTaskIds.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Select at least one care task",
                Toast.LENGTH_SHORT
            ).show()


            return
        }


        // -----------------------------------------------------
        // BUILD MESSAGE
        // -----------------------------------------------------

        val message =
            buildMessagePreview()


        // -----------------------------------------------------
        // SMS INTENT
        // -----------------------------------------------------

        val smsIntent =
            Intent(
                Intent.ACTION_SENDTO
            ).apply {

                data =
                    Uri.parse(
                        "smsto:${Uri.encode(phoneNumber)}"
                    )


                putExtra(
                    "sms_body",
                    message
                )
            }


        // -----------------------------------------------------
        // OPEN SMS APPLICATION
        // -----------------------------------------------------

        try {

            startActivity(
                smsIntent
            )

        } catch (
            exception: ActivityNotFoundException
        ) {

            Toast.makeText(
                this,
                "No SMS application is available on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // =========================================================
    // DP HELPER
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources
                            .displayMetrics
                            .density
                ).toInt()
    }


    // =========================================================
    // CLEAN UP
    // =========================================================

    override fun onDestroy() {

        tasksJob?.cancel()

        super.onDestroy()
    }
}
