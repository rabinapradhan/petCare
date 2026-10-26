package com.example.petcare

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Task
import com.example.petcare.data.repository.TaskRepository

import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class AddTaskActivity : AppCompatActivity() {

    // =========================================================
    // IDs
    // =========================================================

    private var userId: Long = -1L

    private var petId: Int = -1

    // -1 = creating a new task
    // otherwise = editing an existing task
    private var taskId: Int = -1


    // =========================================================
    // DATABASE
    // =========================================================

    private lateinit var taskRepository: TaskRepository


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var taskEditText: EditText

    private lateinit var categorySpinner: Spinner

    private lateinit var repeatsSpinner: Spinner

    private lateinit var timeEditText: TextView

    private lateinit var suppliesEditText: EditText

    private lateinit var notesEditText: EditText

    private lateinit var saveTaskButton: TextView


    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)


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
        // GET TASK ID
        // -----------------------------------------------------
        //
        // -1 = new task
        // otherwise = edit mode
        //

        taskId =
            intent.getIntExtra(
                "task_id",
                -1
            )


        // -----------------------------------------------------
        // VALIDATE IDS
        // -----------------------------------------------------

        if (
            userId == -1L ||
            petId == -1
        ) {

            Toast.makeText(
                this,
                "Unable to open task",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }


        // =====================================================
        // LAYOUT
        // =====================================================

        setContentView(
            R.layout.activity_add_task
        )


        // =====================================================
        // DATABASE
        // =====================================================

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )


        taskRepository =
            TaskRepository(
                database.taskDao()
            )


        // =====================================================
        // INITIALISE
        // =====================================================

        initialiseViews()

        setupSpinners()

        setupTimePicker()

        setupListeners()


        // =====================================================
        // EDIT MODE
        // =====================================================

        if (taskId != -1) {

            loadTaskForEditing()

        }
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        taskEditText =
            findViewById(
                R.id.taskEditText
            )


        categorySpinner =
            findViewById(
                R.id.categorySpinner
            )


        repeatsSpinner =
            findViewById(
                R.id.repeatsSpinner
            )


        timeEditText =
            findViewById(
                R.id.timeEditText
            )


        suppliesEditText =
            findViewById(
                R.id.suppliesEditText
            )


        notesEditText =
            findViewById(
                R.id.notesEditText
            )


        saveTaskButton =
            findViewById(
                R.id.saveTaskButton
            )
    }


    // =========================================================
    // SPINNERS

    private fun setupSpinners() {

        // -----------------------------------------------------
        // CATEGORIES
        // -----------------------------------------------------

        val categories =
            arrayOf(
                "Feeding",
                "Exercise",
                "Grooming",
                "Medication",
                "Healthcare",
                "Other"
            )


        val categoryAdapter =
            ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categories
            )


        categoryAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        categorySpinner.adapter =
            categoryAdapter


        // -----------------------------------------------------
        // REPEAT FREQUENCY
        // -----------------------------------------------------

        val repeats =
            arrayOf(
                "Every day",
                "Every week",
                "Every month",
                "Once"
            )


        val repeatAdapter =
            ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                repeats
            )


        repeatAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )


        repeatsSpinner.adapter =
            repeatAdapter
    }


    // =========================================================
    // TIME PICKER
    // =========================================================

    private fun setupTimePicker() {

        timeEditText.setOnClickListener {

            val calendar =
                Calendar.getInstance()


            val hour =
                calendar.get(
                    Calendar.HOUR_OF_DAY
                )


            val minute =
                calendar.get(
                    Calendar.MINUTE
                )


            TimePickerDialog(
                this,

                { _, selectedHour, selectedMinute ->

                    timeEditText.text =
                        String.format(
                            Locale.getDefault(),
                            "%02d:%02d",
                            selectedHour,
                            selectedMinute
                        )
                },

                hour,
                minute,

                true

            ).show()
        }
    }


    // =========================================================
    // LISTENERS
    // =========================================================

    private fun setupListeners() {

        // -----------------------------------------------------
        // CLOSE
        // -----------------------------------------------------

        findViewById<TextView>(
            R.id.closeButton
        ).setOnClickListener {

            finish()

        }


        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        saveTaskButton.setOnClickListener {

            saveTask()

        }
    }


    // =========================================================
    // LOAD EXISTING TASK
    // =========================================================

    private fun loadTaskForEditing() {

        lifecycleScope.launch {

            try {

                val task =
                    taskRepository.getTaskById(
                        taskId,
                        userId
                    )


                // -------------------------------------------------
                // TASK NOT FOUND
                // -------------------------------------------------

                if (task == null) {

                    Toast.makeText(
                        this@AddTaskActivity,
                        "Task not found",
                        Toast.LENGTH_SHORT
                    ).show()


                    finish()

                    return@launch
                }


                // -------------------------------------------------
                // TASK NAME
                // -------------------------------------------------

                taskEditText.setText(
                    task.title
                )


                // -------------------------------------------------
                // TIME
                // -------------------------------------------------

                timeEditText.text =
                    task.time


                // -------------------------------------------------
                // SUPPLIES
                // -------------------------------------------------

                suppliesEditText.setText(
                    task.supplies
                )


                // -------------------------------------------------
                // NOTES
                // -------------------------------------------------

                notesEditText.setText(
                    task.notes
                )


                // -------------------------------------------------
                // CATEGORY
                // -------------------------------------------------

                @Suppress("UNCHECKED_CAST")

                val categoryAdapter =
                    categorySpinner.adapter
                            as ArrayAdapter<String>


                val categoryPosition =
                    categoryAdapter.getPosition(
                        task.category
                    )


                if (categoryPosition >= 0) {

                    categorySpinner.setSelection(
                        categoryPosition
                    )
                }


                // -------------------------------------------------
                // REPEAT
                // -------------------------------------------------

                @Suppress("UNCHECKED_CAST")

                val repeatAdapter =
                    repeatsSpinner.adapter
                            as ArrayAdapter<String>


                val repeatPosition =
                    repeatAdapter.getPosition(
                        task.repeats
                    )


                if (repeatPosition >= 0) {

                    repeatsSpinner.setSelection(
                        repeatPosition
                    )
                }


                // -------------------------------------------------
                // EDIT BUTTON
                // -------------------------------------------------

                saveTaskButton.text =
                    "Update task"


            } catch (exception: Exception) {

                Toast.makeText(
                    this@AddTaskActivity,
                    exception.message
                        ?: "Unable to load task",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    // =========================================================
    // SAVE / UPDATE TASK
    // =========================================================

    // =========================================================
// SAVE / UPDATE TASK
// =========================================================

    private fun saveTask() {

        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        val title =
            taskEditText.text
                .toString()
                .trim()


        if (title.isBlank()) {

            taskEditText.error =
                "Enter a task name"

            taskEditText.requestFocus()

            return
        }


        // -----------------------------------------------------
        // CATEGORY
        // -----------------------------------------------------

        val category =
            categorySpinner
                .selectedItem
                ?.toString()
                ?.trim()
                .orEmpty()


        // -----------------------------------------------------
        // REPEAT
        // -----------------------------------------------------

        val repeats =
            repeatsSpinner
                .selectedItem
                ?.toString()
                ?.trim()
                .orEmpty()


        // -----------------------------------------------------
        // TIME
        // -----------------------------------------------------

        val time =
            timeEditText.text
                .toString()
                .trim()


        // -----------------------------------------------------
        // REQUIRED TIME VALIDATION
        // -----------------------------------------------------

        if (
            time.isBlank() ||
            time == "--:--"
        ) {

            Toast.makeText(
                this,
                "Please select a task time",
                Toast.LENGTH_SHORT
            ).show()

            timeEditText.requestFocus()

            return
        }


        // -----------------------------------------------------
        // ADDITIONAL TIME FORMAT VALIDATION
        // -----------------------------------------------------
        //
        // A valid selected time must be in HH:mm format.
        // Example:
        //
        // 08:30
        // 14:45
        // 19:00
        //

        val validTimeFormat =
            Regex(
                "^([01][0-9]|2[0-3]):[0-5][0-9]$"
            )


        if (
            !validTimeFormat.matches(time)
        ) {

            Toast.makeText(
                this,
                "Please select a valid task time",
                Toast.LENGTH_SHORT
            ).show()

            timeEditText.requestFocus()

            return
        }


        // -----------------------------------------------------
        // SUPPLIES
        // -----------------------------------------------------

        val supplies =
            suppliesEditText.text
                .toString()
                .trim()


        // -----------------------------------------------------
        // NOTES
        // -----------------------------------------------------

        val notes =
            notesEditText.text
                .toString()
                .trim()


        // =====================================================
        // DATABASE OPERATION
        // =====================================================

        lifecycleScope.launch {

            try {

                // =================================================
                // UPDATE EXISTING TASK
                // =================================================

                if (taskId != -1) {

                    /*
                     * Only editable task information is updated.
                     *
                     * Completion information remains unchanged.
                     */

                    taskRepository.updateTask(

                        taskId = taskId,

                        userId = userId,

                        title = title,

                        category = category,

                        repeats = repeats,

                        time = time,

                        supplies = supplies,

                        notes = notes
                    )


                    Toast.makeText(
                        this@AddTaskActivity,
                        "Task updated",
                        Toast.LENGTH_SHORT
                    ).show()
                }


                // =================================================
                // CREATE NEW TASK
                // =================================================

                else {

                    // -------------------------------------------------
                    // TODAY'S DATE
                    // -------------------------------------------------

                    val today =
                        SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.getDefault()
                        ).format(
                            Date()
                        )


                    // -------------------------------------------------
                    // CREATE TASK
                    // -------------------------------------------------

                    val task =
                        Task(

                            userId = userId,

                            petId = petId,

                            title = title,

                            category = category,

                            repeats = repeats,

                            time = time,

                            supplies = supplies,

                            notes = notes,

                            completed = false,

                            startDate = today,

                            completedDate = null
                        )


                    // -------------------------------------------------
                    // SAVE
                    // -------------------------------------------------

                    taskRepository.addTask(
                        task
                    )


                    Toast.makeText(
                        this@AddTaskActivity,
                        "Task added",
                        Toast.LENGTH_SHORT
                    ).show()
                }


                // =================================================
                // RETURN TO PET DETAILS
                // =================================================

                val intent =
                    Intent(
                        this@AddTaskActivity,
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


            } catch (exception: Exception) {

                Toast.makeText(
                    this@AddTaskActivity,

                    exception.message
                        ?: "Unable to save task",

                    Toast.LENGTH_LONG

                ).show()
            }
        }
    }}