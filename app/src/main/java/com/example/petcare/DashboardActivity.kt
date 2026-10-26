package com.example.petcare

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.entity.Task
import com.example.petcare.data.repository.ExpenseRepository
import com.example.petcare.data.repository.PetRepository
import com.example.petcare.data.repository.PlaceRepository
import com.example.petcare.data.repository.TaskRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.petcare.data.dao.UserDao


class DashboardActivity : AppCompatActivity() {

    // ============================================================
    // REPOSITORIES
    // ============================================================

    private lateinit var petRepository: PetRepository
    private lateinit var taskRepository: TaskRepository
    private lateinit var placeRepository: PlaceRepository
    private lateinit var expenseRepository: ExpenseRepository

    private lateinit var userDao: UserDao
    // ============================================================
    // DASHBOARD VIEWS
    // ============================================================

    private lateinit var greetingText: TextView

    private lateinit var petCountText: TextView

    private lateinit var placeCountText: TextView

    private lateinit var spendText: TextView

    private lateinit var checklistDoneText: TextView

    private lateinit var checklistPercentText: TextView

    private lateinit var progressBar: View

    private lateinit var stillDueContainer: LinearLayout

    private lateinit var stillDueText: TextView

    private lateinit var myPetsContainer: LinearLayout


    // ============================================================
    // USER
    // ============================================================

    private var userId: Long = -1L


    // ============================================================
    // ON CREATE
    // ============================================================

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
        // SET LAYOUT
        // --------------------------------------------------------

        setContentView(
            R.layout.activity_dashboard
        )


        // --------------------------------------------------------
        // DATABASE
        // --------------------------------------------------------

        initialiseRepositories()


        // --------------------------------------------------------
        // VIEWS
        // --------------------------------------------------------

        initialiseViews()


        // --------------------------------------------------------
        // BUTTONS / NAVIGATION
        // --------------------------------------------------------

        setupClickListeners()


        // --------------------------------------------------------
        // ROOM OBSERVERS
        // --------------------------------------------------------

        observeDashboardData()
    }


    // ============================================================
    // INITIALISE REPOSITORIES
    // ============================================================

    private fun initialiseRepositories() {

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )

        // User DAO
        userDao =
            database.userDao()

        // Pet repository
        petRepository =
            PetRepository(
                database.petDao()
            )

        // Task repository
        taskRepository =
            TaskRepository(
                database.taskDao()
            )

        // Place repository
        placeRepository =
            PlaceRepository(
                database.placeDao()
            )

        // Expense repository
        expenseRepository =
            ExpenseRepository(
                database.expenseDao()
            )
    }


    // ============================================================
    // INITIALISE VIEWS
    // ============================================================

    private fun initialiseViews() {

        greetingText =
            findViewById(
                R.id.greetingText
            )


        petCountText =
            findViewById(
                R.id.petCountText
            )


        placeCountText =
            findViewById(
                R.id.placeCountText
            )


        spendText =
            findViewById(
                R.id.spendText
            )


        checklistDoneText =
            findViewById(
                R.id.checklistDoneText
            )


        checklistPercentText =
            findViewById(
                R.id.checklistPercentText
            )


        progressBar =
            findViewById(
                R.id.progressBar
            )


        stillDueContainer =
            findViewById(
                R.id.stillDueContainer
            )


        stillDueText =
            findViewById(
                R.id.stillDueText
            )


        myPetsContainer =
            findViewById(
                R.id.myPetsContainer
            )


        loadUserName()
    }


    // ============================================================
// LOAD LOGGED-IN USER NAME
// ============================================================

    private fun loadUserName() {

        lifecycleScope.launch {

            val user =
                userDao.getUserById(
                    userId.toInt()
                )

            if (user != null) {

                greetingText.text =
                    "Hi ${user.name}"

            } else {

                // Fallback if the user cannot be found
                greetingText.text =
                    "Hi PetCare Pro"
            }
        }
    }

    // ============================================================
    // CLICK LISTENERS
    // ============================================================

    private fun setupClickListeners() {

        // ========================================================
        // OPEN CHECKLIST
        // ========================================================

        findViewById<TextView>(
            R.id.openChecklistButton
        ).setOnClickListener {

            openActivity(
                ChecklistActivity::class.java
            )
        }


        // ========================================================
        // HOME
        // ========================================================

        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            // Already on Dashboard.
        }


        // ========================================================
        // PETS
        // ========================================================

        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            openActivity(
                PetsActivity::class.java
            )
        }


        // ========================================================
        // CHECKLIST
        // ========================================================

        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            openActivity(
                ChecklistActivity::class.java
            )
        }


        // ========================================================
        // PLACES
        // ========================================================

        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {

            openActivity(
                PlacesActivity::class.java
            )
        }


        // ========================================================
        // SPEND
        // ========================================================

        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            openActivity(
                SpendingActivity::class.java
            )
        }


        // ========================================================
        // LOGOUT
        // ========================================================

        findViewById<ImageView>(
            R.id.logoutButton
        ).setOnClickListener {

            val sharedPreferences =
                getSharedPreferences(
                    "PetCarePrefs",
                    MODE_PRIVATE
                )

            sharedPreferences.edit()
                .clear()
                .apply()


            val intent =
                Intent(
                    this@DashboardActivity,
                    MainActivity::class.java
                )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)

            finish()
        }
    }


    // ============================================================
    // OPEN ACTIVITY
    // ============================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this@DashboardActivity,
                activityClass
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
    // OBSERVE DASHBOARD DATA
    // ============================================================

    private fun observeDashboardData() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                // ==================================================
                // PETS
                // ==================================================

                launch {

                    petRepository
                        .getPets(userId)
                        .collect { pets ->

                            updatePets(
                                pets
                            )
                        }
                }


                // ==================================================
                // TASKS
                // ==================================================

                launch {

                    taskRepository
                        .getTasksForUser(userId)
                        .collect { tasks ->

                            val todayTasks =
                                getTodayTasks(tasks)

                            updateChecklist(
                                todayTasks
                            )
                        }
                }


                // ==================================================
                // PLACES
                // ==================================================

                launch {

                    placeRepository
                        .getPlaces(userId)
                        .collect { places ->

                            updatePlaces(
                                places.size
                            )
                        }
                }


                // ==================================================
                // EXPENSES
                // ==================================================

                launch {

                    expenseRepository
                        .getAllTimeTotal(userId)
                        .collect { total ->

                            updateSpending(
                                total
                            )
                        }
                }
            }
        }
    }


    // ============================================================
    // GET TODAY'S TASKS
    // ============================================================

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


            if (startDate == null) {

                return@filter true
            }


            if (startDate.after(today)) {

                return@filter false
            }


            val startCalendar =
                Calendar.getInstance().apply {

                    time = startDate
                }


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


    // ============================================================
    // CHECK SAME DATE
    // ============================================================

    private fun isSameDate(
        firstDate: Date,
        secondDate: Date
    ): Boolean {

        val dateFormat =
            SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.getDefault()
            )

        return dateFormat.format(firstDate) ==
                dateFormat.format(secondDate)
    }


    // ============================================================
    // TODAY STRING
    // ============================================================

    private fun getTodayString(): String {

        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(
            Date()
        )
    }


    // ============================================================
    // COMPLETED TODAY
    // ============================================================

    private fun isCompletedToday(
        task: Task
    ): Boolean {

        return task.completed &&
                task.completedDate == getTodayString()
    }


    // ============================================================
    // UPDATE PETS
    // ============================================================

    private fun updatePets(
        pets: List<Pet>
    ) {

        // --------------------------------------------------------
        // COUNT
        // --------------------------------------------------------

        petCountText.text =
            pets.size.toString()


        // --------------------------------------------------------
        // CLEAR OLD CARDS
        // --------------------------------------------------------

        myPetsContainer.removeAllViews()


        // --------------------------------------------------------
        // NO PETS
        // --------------------------------------------------------

        if (pets.isEmpty()) {

            addEmptyPetCard()

            addAddPetCard()

            return
        }


        // --------------------------------------------------------
        // ADD ALL PETS
        // --------------------------------------------------------

        pets.forEach { pet ->

            addPetCard(
                pet
            )
        }


        // --------------------------------------------------------
        // ADD PET BUTTON
        // --------------------------------------------------------

        addAddPetCard()
    }


    // ============================================================
    // CREATE PET CARD
    // ============================================================

    private fun addPetCard(
        pet: Pet
    ) {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        card.setPadding(
            10,
            10,
            10,
            10
        )

        val params =
            LinearLayout.LayoutParams(
                dp(145),
                dp(135)
            )

        params.marginEnd =
            dp(12)

        card.layoutParams =
            params


        // CARD BACKGROUND

        card.background =
            roundedBackground(
                Color.WHITE,
                24f
            )


        card.isClickable =
            true

        card.isFocusable =
            true


        // --------------------------------------------------------
// PET ICON
// --------------------------------------------------------

        val icon = ImageView(this)

        icon.setImageResource(
            R.drawable.ic_pet_profile2
        )

        icon.setPadding(
            dp(12),
            dp(12),
            dp(12),
            dp(12)
        )

// GREEN CIRCULAR BACKGROUND
        icon.background = roundedBackground(
            Color.rgb(
                46,
                139,
                87
            ),
            50f
        )

// WHITE PAW ICON
        icon.setColorFilter(
            Color.WHITE
        )

        val iconParams = LinearLayout.LayoutParams(
            dp(46),
            dp(46)
        )

        icon.layoutParams = iconParams

        card.addView(icon)

        // --------------------------------------------------------
        // PET NAME
        // --------------------------------------------------------

        val name =
            TextView(this)

        name.text =
            pet.name

        name.setTextColor(
            Color.rgb(
                30,
                45,
                35
            )
        )

        name.textSize =
            15f

        name.gravity =
            Gravity.CENTER

        name.setPadding(
            0,
            dp(7),
            0,
            0
        )

        card.addView(
            name
        )


        // --------------------------------------------------------
        // BREED / SPECIES
        // --------------------------------------------------------

        val breed =
            TextView(this)

        breed.text =
            buildPetSubtitle(
                pet.species,
                pet.breed
            )

        breed.setTextColor(
            Color.rgb(
                105,
                120,
                110
            )
        )

        breed.textSize =
            13f

        breed.gravity =
            Gravity.CENTER

        card.addView(
            breed
        )


        // --------------------------------------------------------
        // OPEN PET
        // --------------------------------------------------------

        card.setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    PetDetailsActivity::class.java
                )

            intent.putExtra(
                "user_id",
                userId
            )

            intent.putExtra(
                "pet_id",
                pet.id
            )

            startActivity(
                intent
            )
        }


        myPetsContainer.addView(
            card
        )
    }


    // ============================================================
    // EMPTY PET CARD
    // ============================================================

    // ============================================================
// EMPTY PET CARD
// ============================================================

    private fun addEmptyPetCard() {

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.gravity =
            Gravity.CENTER

        val params =
            LinearLayout.LayoutParams(
                dp(145),
                dp(135)
            )

        params.marginEnd =
            dp(12)

        card.layoutParams =
            params

        card.background =
            roundedBackground(
                Color.WHITE,
                24f
            )


        // --------------------------------------------------------
        // NO PETS TEXT
        // --------------------------------------------------------

        val text =
            TextView(this)

        text.text =
            "No pets"

        text.textSize =
            15f

        text.setTextColor(
            Color.DKGRAY
        )

        text.layoutParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        text.gravity =
            Gravity.CENTER

        text.setPadding(
            0,
            0,
            0,
            0
        )

        card.addView(
            text
        )


        myPetsContainer.addView(
            card
        )
    }

    // ============================================================
    // ADD PET CARD
    // ============================================================

    private fun addAddPetCard() {

        val card =
            TextView(this)

        card.layoutParams =
            LinearLayout.LayoutParams(
                dp(145),
                dp(135)
            ).apply {

                marginEnd =
                    dp(12)
            }

        card.gravity =
            Gravity.CENTER

        card.text =
            "+ Add a pet"

        card.textSize =
            15f

        card.setTextColor(
            Color.rgb(
                40,
                125,
                75
            )
        )

        card.background =
            roundedBackground(
                Color.rgb(
                    250,
                    252,
                    247
                ),
                24f
            )


        card.setOnClickListener {

            val intent =
                Intent(
                    this@DashboardActivity,
                    AddPetActivity::class.java
                )

            intent.putExtra(
                "user_id",
                userId
            )

            intent.putExtra(
                "source",
                "dashboard"
            )

            startActivity(
                intent
            )
        }


        myPetsContainer.addView(
            card
        )
    }


    // ============================================================
    // UPDATE PLACES
    // ============================================================

    private fun updatePlaces(
        count: Int
    ) {

        placeCountText.text =
            count.toString()
    }


    // ============================================================
    // UPDATE SPENDING
    // ============================================================

    private fun updateSpending(
        total: Double
    ) {

        spendText.text =
            String.format(
                Locale.UK,
                "%.2f",
                total
            )
    }


    // ============================================================
    // UPDATE CHECKLIST
    // ============================================================

    private fun updateChecklist(
        tasks: List<Task>
    ) {

        val total =
            tasks.size


        val completed =
            tasks.count { task ->

                isCompletedToday(
                    task
                )
            }


        // --------------------------------------------------------
        // 1/3 DONE
        // --------------------------------------------------------

        checklistDoneText.text =
            "$completed/$total done"


        // --------------------------------------------------------
        // PERCENTAGE
        // --------------------------------------------------------

        val percentage =
            if (total == 0) {

                0

            } else {

                (completed * 100) / total
            }


        checklistPercentText.text =
            "$percentage%"


        // --------------------------------------------------------
        // PROGRESS BAR
        // --------------------------------------------------------

        progressBar.pivotX =
            0f

        progressBar.scaleX =
            if (percentage == 0) {

                0f

            } else {

                percentage / 100f
            }


        // --------------------------------------------------------
        // STILL DUE
        // --------------------------------------------------------

        updateStillDue(
            tasks
        )
    }


    // ============================================================
    // STILL DUE
    // ============================================================

    private fun updateStillDue(
        tasks: List<Task>
    ) {

        stillDueContainer.removeAllViews()


        val incompleteTasks =
            tasks.filter { task ->

                !isCompletedToday(
                    task
                )
            }


        // --------------------------------------------------------
        // NOTHING DUE
        // --------------------------------------------------------

        if (incompleteTasks.isEmpty()) {

            stillDueText.visibility =
                View.VISIBLE

            return
        }


        stillDueText.visibility =
            View.GONE


        // --------------------------------------------------------
        // ADD TASK CARDS
        // --------------------------------------------------------

        incompleteTasks
            .take(5)
            .forEach { task ->

                addStillDueCard(
                    task
                )
            }
    }


    // ============================================================
// CREATE STILL DUE CARD
// ============================================================

    private fun addStillDueCard(
        task: Task
    ) {

        val card = LinearLayout(this)

        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL

        card.setPadding(
            dp(14),
            dp(10),
            dp(14),
            dp(10)
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(68)
        )

        params.bottomMargin = dp(9)

        card.layoutParams = params

        card.background = roundedBackground(
            Color.WHITE,
            24f
        )

        // --------------------------------------------------------
        // ICON CIRCLE
        // --------------------------------------------------------

        val icon = ImageView(this)

        icon.layoutParams = LinearLayout.LayoutParams(
            dp(42),
            dp(42)
        )

        icon.setPadding(
            dp(9),
            dp(9),
            dp(9),
            dp(9)
        )

        icon.setImageResource(
            getTaskIcon(task)
        )

        icon.setColorFilter(
            Color.rgb(
                40,
                125,
                75
            )
        )

        icon.background = roundedBackground(
            Color.rgb(
                46,
                139,
                87
            ),
            50f
        )

        icon.setColorFilter(
            Color.WHITE
        )
        card.addView(icon)

        // --------------------------------------------------------
        // TASK INFORMATION
        // --------------------------------------------------------

        val information = LinearLayout(this)

        information.orientation = LinearLayout.VERTICAL

        information.setPadding(
            dp(14),
            0,
            0,
            0
        )

        information.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        // --------------------------------------------------------
        // TASK TITLE
        // --------------------------------------------------------

        val title = TextView(this)

        title.text = task.title

        title.textSize = 15f

        title.setTextColor(
            Color.rgb(
                35,
                45,
                38
            )
        )

        information.addView(title)

        // --------------------------------------------------------
        // TASK TIME
        // --------------------------------------------------------

        val time = TextView(this)

        time.text = buildTaskTimeText(task)

        time.textSize = 13f

        time.setTextColor(
            Color.rgb(
                105,
                120,
                110
            )
        )

        information.addView(time)

        card.addView(information)

        stillDueContainer.addView(card)
    }

    // ============================================================
// TASK ICON
// ============================================================

    private fun getTaskIcon(
        task: Task
    ): Int {

        return when (
            task.category.trim().lowercase(Locale.getDefault())
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

            else ->
                R.drawable.ic_category_healthcare
        }
    }

    // ============================================================
    // TASK TIME
    // ============================================================

    private fun buildTaskTimeText(
        task: Task
    ): String {

        /*
         * If your Task entity has a time field,
         * replace this with the actual task.time value.
         */

        return "Due today"
    }


    // ============================================================
    // PET SUBTITLE
    // ============================================================

    private fun buildPetSubtitle(
        species: String,
        breed: String
    ): String {

        return listOf(
            species,
            breed
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(
                " · "
            )
    }


    // ============================================================
    // CREATE ROUNDED BACKGROUND
    // ============================================================

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(
                color
            )

            cornerRadius =
                dp(radius.toInt()).toFloat()
        }
    }


    // ============================================================
    // DP CONVERSION
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}