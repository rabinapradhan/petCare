package com.example.petcare

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Task
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.TaskRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class ChecklistActivity : AppCompatActivity() {

    // =========================================================
    // USER
    // =========================================================

    private var userId: Long = -1L


    // =========================================================
    // REPOSITORIES
    // =========================================================

    private lateinit var taskRepository: TaskRepository
    private lateinit var petRepository: PetRepository


    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var tasksContainer: LinearLayout
    private lateinit var progressText: TextView
    private lateinit var percentText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var noTasksText: TextView


    // =========================================================
    // PET NAMES
    // =========================================================

    private var petNameMap: Map<Int, String> =
        emptyMap()


    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        // =====================================================
        // GET USER ID
        // =====================================================

        userId =
            intent.getLongExtra(
                "user_id",
                -1L
            )


        // =====================================================
        // VALIDATE USER
        // =====================================================

        if (userId == -1L) {

            finish()

            return
        }


        // =====================================================
        // LAYOUT
        // =====================================================

        setContentView(
            R.layout.activity_checklist
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


        petRepository =
            PetRepository(
                database.petDao()
            )


        // =====================================================
        // INITIALISE
        // =====================================================

        initialiseViews()

        setupNavigation()

        setupDelegationButton()

        observeTasksAndPets()
    }


    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        tasksContainer =
            findViewById(
                R.id.tasksContainer
            )


        progressText =
            findViewById(
                R.id.progressText
            )


        percentText =
            findViewById(
                R.id.percentText
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )


        noTasksText =
            findViewById(
                R.id.noTasksText
            )
    }


    // =========================================================
    // DELEGATION BUTTON
    // =========================================================

    private fun setupDelegationButton() {

        findViewById<ImageView>(
            R.id.delegateButton
        ).setOnClickListener {

            openSmsDelegation()
        }
    }


    // =========================================================
    // OPEN SMS DELEGATION
    // =========================================================

    private fun openSmsDelegation() {

        val intent =
            Intent(
                this@ChecklistActivity,
                SmsDelegationActivity::class.java
            )


        intent.putExtra(
            "user_id",
            userId
        )


        startActivity(
            intent
        )
    }


    // =========================================================
    // OBSERVE TASKS AND PETS
    // =========================================================

    private fun observeTasksAndPets() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                combine(

                    taskRepository.getTasksForUser(
                        userId
                    ),

                    petRepository.getPets(
                        userId
                    )

                ) { tasks, pets ->

                    Pair(
                        tasks,
                        pets
                    )

                }.collect { (tasks, pets) ->

                    // -------------------------------------------------
                    // CREATE PET NAME MAP
                    // -------------------------------------------------

                    petNameMap =
                        pets.associate { pet ->

                            pet.id to pet.name
                        }


                    // -------------------------------------------------
                    // ONLY SHOW TODAY'S TASKS
                    // -------------------------------------------------

                    val todayTasks =
                        getTodayTasks(
                            tasks
                        )


                    renderChecklist(
                        todayTasks
                    )
                }
            }
        }
    }


    // =========================================================
    // GET TODAY'S TASKS
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

            if (startDate == null) {

                return@filter true
            }


            // -------------------------------------------------
            // TASK HAS NOT STARTED
            // -------------------------------------------------

            if (startDate.after(today)) {

                return@filter false
            }


            val startCalendar =
                Calendar.getInstance().apply {

                    time = startDate
                }


            // -------------------------------------------------
            // REPEAT
            // -------------------------------------------------

            when (task.repeats) {

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
    // SAME DATE
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
    // TODAY STRING
    // =========================================================

    private fun getTodayString(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(
            Date()
        )
    }


    // =========================================================
    // COMPLETED TODAY
    // =========================================================

    private fun isCompletedToday(
        task: Task
    ): Boolean {

        return task.completed &&
                task.completedDate ==
                getTodayString()
    }


    // =========================================================
    // RENDER CHECKLIST
    // =========================================================

    private fun renderChecklist(
        tasks: List<Task>
    ) {

        // -----------------------------------------------------
        // PROGRESS
        // -----------------------------------------------------

        val completed =
            tasks.count { task ->

                isCompletedToday(
                    task
                )
            }


        val total =
            tasks.size


        val percentage =
            if (total == 0) {

                0

            } else {

                (completed * 100) / total
            }


        progressText.text =
            "$completed of $total tasks done"


        percentText.text =
            "$percentage%"


        progressBar.progress =
            percentage


        // -----------------------------------------------------
        // CLEAR OLD CARDS
        // -----------------------------------------------------

        tasksContainer.removeAllViews()


        // -----------------------------------------------------
        // EMPTY STATE
        // -----------------------------------------------------

        if (tasks.isEmpty()) {

            noTasksText.visibility =
                View.VISIBLE

            return
        }


        noTasksText.visibility =
            View.GONE


        // -----------------------------------------------------
        // GROUP BY CATEGORY
        // -----------------------------------------------------

        val groups =
            tasks.groupBy {
                it.category
            }


        // -----------------------------------------------------
        // CATEGORY ORDER
        // -----------------------------------------------------

        val categoryOrder =
            listOf(
                "Feeding",
                "Exercise",
                "Grooming",
                "Medication",
                "Healthcare"
            )


        // -----------------------------------------------------
        // DISPLAY ONLY ALLOWED CATEGORIES
        // -----------------------------------------------------

        categoryOrder.forEach { category ->

            val categoryTasks =
                groups[category]
                    ?: emptyList()


            if (categoryTasks.isNotEmpty()) {

                addCategoryHeader(
                    category
                )


                categoryTasks.forEach { task ->

                    addTaskCard(
                        task
                    )
                }
            }
        }

        /*
         * IMPORTANT:
         *
         * There is intentionally NO "Other" category
         * and NO unknown-category rendering here.
         *
         * Therefore only:
         *
         * Feeding
         * Exercise
         * Grooming
         * Medication
         * Healthcare
         *
         * are displayed.
         */
    }


    // =========================================================
    // CATEGORY HEADER
    // =========================================================

    private fun addCategoryHeader(
        category: String
    ) {

        // -----------------------------------------------------
        // HEADER CONTAINER
        // -----------------------------------------------------

        val header =
            LinearLayout(this)


        header.orientation =
            LinearLayout.HORIZONTAL


        header.gravity =
            android.view.Gravity.CENTER_VERTICAL


        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        params.topMargin =
            dp(18)


        params.bottomMargin =
            dp(8)


        header.layoutParams =
            params


        // -----------------------------------------------------
        // CATEGORY ICON
        // -----------------------------------------------------

        val icon =
            ImageView(this)


        icon.setImageResource(
            getCategoryIcon(
                category
            )
        )


        icon.layoutParams =
            LinearLayout.LayoutParams(
                dp(26),
                dp(26)
            )


        icon.setPadding(
            dp(3),
            dp(3),
            dp(3),
            dp(3)
        )


        icon.setColorFilter(
            getCategoryColor(
                category
            )
        )


        header.addView(
            icon
        )


        // -----------------------------------------------------
        // CATEGORY TEXT
        // -----------------------------------------------------

        val title =
            TextView(this)


        title.text =
            category


        title.textSize =
            17f


        title.setTextColor(
            getCategoryColor(
                category
            )
        )


        title.setTypeface(
            null,
            Typeface.BOLD
        )


        title.setPadding(
            dp(7),
            0,
            0,
            0
        )


        header.addView(
            title
        )


        tasksContainer.addView(
            header
        )
    }


    // =========================================================
    // TASK CARD
    // =========================================================

    private fun addTaskCard(
        task: Task
    ) {

        val card =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.item_checklist_task,
                    tasksContainer,
                    false
                )


        // -----------------------------------------------------
        // VIEWS
        // -----------------------------------------------------

        val taskIcon =
            card.findViewById<ImageView>(
                R.id.taskIconText
            )


        val taskTitleText =
            card.findViewById<TextView>(
                R.id.taskTitleText
            )


        val taskDetailsText =
            card.findViewById<TextView>(
                R.id.taskDetailsText
            )


        val taskSuppliesText =
            card.findViewById<TextView>(
                R.id.taskSuppliesText
            )


        val completeButton =
            card.findViewById<ImageView>(
                R.id.completeButton
            )


        val editButton =
            card.findViewById<ImageView>(
                R.id.editTaskButton
            )


        val deleteButton =
            card.findViewById<ImageView>(
                R.id.deleteTaskButton
            )


        // -----------------------------------------------------
        // MATERIAL CATEGORY ICON
        // -----------------------------------------------------

        taskIcon.setImageResource(
            getCategoryIcon(
                task.category
            )
        )


        taskIcon.setColorFilter(
            getCategoryColor(
                task.category
            )
        )


        // -----------------------------------------------------
        // TITLE
        // -----------------------------------------------------

        taskTitleText.text =
            task.title


        // -----------------------------------------------------
        // PET NAME + TIME + REPEAT
        // -----------------------------------------------------

        val petName =
            petNameMap[task.petId]
                ?: "Unknown pet"


        val repeatText =
            when (
                task.repeats.lowercase(
                    Locale.getDefault()
                )
            ) {

                "every day" ->
                    "daily"

                "every week" ->
                    "weekly"

                "every month" ->
                    "monthly"

                "once" ->
                    "once"

                else ->
                    task.repeats.lowercase(
                        Locale.getDefault()
                    )
            }


        taskDetailsText.text =
            "$petName · ${task.time} · $repeatText"


        // -----------------------------------------------------
        // SUPPLIES
        // -----------------------------------------------------

        if (task.supplies.isBlank()) {

            taskSuppliesText.text =
                "Supplies: None"

        } else {

            taskSuppliesText.text =
                "Supplies: ${task.supplies}"
        }


        taskSuppliesText.visibility =
            View.VISIBLE


        // -----------------------------------------------------
        // COMPLETED APPEARANCE
        // -----------------------------------------------------

        updateCompletedAppearance(
            task,
            taskTitleText,
            taskDetailsText,
            taskSuppliesText,
            completeButton
        )


        // =====================================================
        // COMPLETE
        // =====================================================

        completeButton.setOnClickListener {

            toggleTask(
                task
            )
        }


        // =====================================================
        // CARD CLICK
        // =====================================================

        card.setOnClickListener {

            toggleTask(
                task
            )
        }


        // =====================================================
        // EDIT
        // =====================================================

        editButton.setOnClickListener {

            val intent =
                Intent(
                    this@ChecklistActivity,
                    AddTaskActivity::class.java
                )


            intent.putExtra(
                "user_id",
                userId
            )


            intent.putExtra(
                "pet_id",
                task.petId
            )


            intent.putExtra(
                "task_id",
                task.id
            )


            startActivity(
                intent
            )
        }


        // =====================================================
        // DELETE
        // =====================================================

        deleteButton.setOnClickListener {

            showDeleteConfirmation(
                task
            )
        }


        // -----------------------------------------------------
        // ADD CARD
        // -----------------------------------------------------

        tasksContainer.addView(
            card
        )
    }


    // =========================================================
    // TOGGLE TASK
    // =========================================================

    private fun toggleTask(
        task: Task
    ) {

        lifecycleScope.launch {

            try {

                val completedToday =
                    isCompletedToday(
                        task
                    )


                if (completedToday) {

                    // -----------------------------------------
                    // UNCOMPLETE
                    // -----------------------------------------

                    taskRepository.setCompleted(
                        taskId = task.id,
                        userId = userId,
                        completed = false,
                        completedDate = null
                    )

                } else {

                    // -----------------------------------------
                    // COMPLETE TODAY
                    // -----------------------------------------

                    taskRepository.setCompleted(
                        taskId = task.id,
                        userId = userId,
                        completed = true,
                        completedDate =
                            getTodayString()
                    )
                }

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@ChecklistActivity,
                    exception.message
                        ?: "Unable to update task",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // =========================================================
    // COMPLETED APPEARANCE
    // =========================================================

    private fun updateCompletedAppearance(
        task: Task,
        title: TextView,
        details: TextView,
        supplies: TextView,
        completeButton: ImageView
    ) {

        val completedToday =
            isCompletedToday(
                task
            )


        if (completedToday) {

            title.paintFlags =
                title.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG


            details.paintFlags =
                details.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG


            supplies.paintFlags =
                supplies.paintFlags or
                        Paint.STRIKE_THRU_TEXT_FLAG


            title.alpha =
                0.55f


            details.alpha =
                0.55f


            supplies.alpha =
                0.55f


            completeButton.setImageResource(
                R.drawable.ic_checkbox_mark
            )

        } else {

            title.paintFlags =
                title.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()


            details.paintFlags =
                details.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()


            supplies.paintFlags =
                supplies.paintFlags and
                        Paint.STRIKE_THRU_TEXT_FLAG.inv()


            title.alpha =
                1f


            details.alpha =
                1f


            supplies.alpha =
                1f


            completeButton.setImageResource(
                R.drawable.ic_checkbox
            )
        }
    }


    // =========================================================
    // DELETE CONFIRMATION
    // =========================================================

    private fun showDeleteConfirmation(
        task: Task
    ) {

        AlertDialog.Builder(this)

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

                lifecycleScope.launch {

                    try {

                        taskRepository.deleteTask(
                            task.id,
                            userId
                        )


                        Toast.makeText(
                            this@ChecklistActivity,
                            "Task deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (
                        exception: Exception
                    ) {

                        Toast.makeText(
                            this@ChecklistActivity,
                            exception.message
                                ?: "Unable to delete task",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            .show()
    }


    // =========================================================
    // CATEGORY ICON
    // =========================================================

    private fun getCategoryIcon(
        category: String
    ): Int {

        return when (
            category.trim().lowercase(
                Locale.getDefault()
            )
        ) {

            "feeding" ->
                R.drawable.ic_category_feeding


            "exercise" ->
                R.drawable.ic_category_exercise


            "grooming" ->
                R.drawable.ic_category_grooming


            "medication" ->
                R.drawable.ic_category_medication


            "healthcare" ->
                R.drawable.ic_category_healthcare


            // -------------------------------------------------
            // FALLBACK
            // -------------------------------------------------
            //
            // "Other" is NOT a category.
            //
            // This fallback only prevents a crash if an old
            // database record contains an invalid category.
            // Such a record will NOT be displayed because
            // renderChecklist() only renders the five allowed
            // categories.
            //

            else ->
                R.drawable.ic_category_healthcare
        }
    }


    // =========================================================
    // CATEGORY COLOR
    // =========================================================

    private fun getCategoryColor(
        category: String
    ): Int {

        return when (
            category.trim().lowercase(
                Locale.getDefault()
            )
        ) {

            "feeding" ->
                Color.rgb(
                    210,
                    110,
                    20
                )


            "exercise" ->
                Color.rgb(
                    0,
                    130,
                    190
                )


            "grooming" ->
                Color.rgb(
                    45,
                    129,
                    74
                )


            "medication" ->
                Color.rgb(
                    199,
                    60,
                    77
                )


            "healthcare" ->
                Color.rgb(
                    180,
                    60,
                    80
                )


            else ->
                Color.rgb(
                    180,
                    60,
                    80
                )
        }
    }


    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun setupNavigation() {

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

            // Already here.
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
    // OPEN ACTIVITY
    // =========================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this@ChecklistActivity,
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
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}