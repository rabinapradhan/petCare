package com.example.petcare

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Expense
import com.example.petcare.data.entity.Pet
import com.example.petcare.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpendingActivity : AppCompatActivity() {


    // DATABASE / REPOSITORY

    private lateinit var repository: ExpenseRepository

    // Logged-in user
    private var userId: Long = -1L


    // UI

    private lateinit var monthTotalText: TextView
    private lateinit var allTimeTotalText: TextView

    private lateinit var emptyState: LinearLayout
    private lateinit var expenseListContainer: LinearLayout


    // DATE FORMATS
    // Date shown in the dialog
    // Example: 10/09/2026
    private val dateFormat =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

    // Date stored in Room
    // Example: 2026-09-10
    private val databaseDateFormat =
        SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        )


    // ON CREATE
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_spending)


        // Get logged-in user ID

        userId = intent.getLongExtra(
            "user_id",
            -1L
        )

        if (userId == -1L) {

            Toast.makeText(
                this,
                "User session not found",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }


        // Database

        val database =
            PetCareDatabase.getDatabase(this)

        repository =
            ExpenseRepository(
                database.expenseDao()
            )


        // Find views


        monthTotalText =
            findViewById(
                R.id.monthTotalText
            )

        allTimeTotalText =
            findViewById(
                R.id.allTimeTotalText
            )

        emptyState =
            findViewById(
                R.id.emptyState
            )

        expenseListContainer =
            findViewById(
                R.id.expenseListContainer
            )


        // Navigation

        setupNavigation()


        // Add expense button

        findViewById<View>(
            R.id.addExpenseButton
        ).setOnClickListener {

            showAddExpenseDialog()
        }


        // Observe Room database

        observeExpenses()
    }


    // OBSERVE EXPENSES

    private fun observeExpenses() {

        lifecycleScope.launch {

            // Current month
            // Example: 2026-09

            val currentMonth =
                SimpleDateFormat(
                    "yyyy-MM",
                    Locale.getDefault()
                ).format(
                    Calendar.getInstance().time
                )

            combine(

                // This month's total
                repository.getMonthlyTotal(
                    userId,
                    currentMonth
                ),

                // All-time total
                repository.getAllTimeTotal(
                    userId
                ),

                // Expense list
                repository.getExpenses(
                    userId
                )

            ) { month, allTime, expenses ->

                Triple(
                    month,
                    allTime,
                    expenses
                )

            }.collect { result ->

                val month =
                    result.first

                val allTime =
                    result.second

                val expenses =
                    result.third


                // Update monthly total


                monthTotalText.text =
                    String.format(
                        Locale.UK,
                        "Rs %.2f",
                        month
                    )

                // -------------------------------------------------
                // Update all-time total
                // -------------------------------------------------

                allTimeTotalText.text =
                    String.format(
                        Locale.UK,
                        "Rs %.2f",
                        allTime
                    )


                // Update expense list

                displayExpenses(
                    expenses
                )
            }
        }
    }


    // DISPLAY EXPENSES

    private fun displayExpenses(
        expenses: List<Expense>
    ) {

        // Remove old cards
        expenseListContainer.removeAllViews()


        // EMPTY STATE


        if (expenses.isEmpty()) {

            emptyState.visibility =
                View.VISIBLE

            return
        }


        emptyState.visibility =
            View.GONE


        // Inflate cards


        val inflater =
            LayoutInflater.from(this)

        expenses.forEach { expense ->

            val expenseView =
                inflater.inflate(
                    R.layout.item_expense,
                    expenseListContainer,
                    false
                )


            // Views


            val categoryText =
                expenseView.findViewById<TextView>(
                    R.id.expenseCategoryText
                )

            val detailsText =
                expenseView.findViewById<TextView>(
                    R.id.expenseDetailsText
                )

            val deleteButton =
                expenseView.findViewById< ImageButton>(
                    R.id.deleteExpenseButton
                )


            // First line
            //
            // Food · Rs 4,000.00


            categoryText.text =
                String.format(
                    Locale.US,
                    "%s · Rs %.2f",
                    expense.category,
                    expense.amount
                )


            // Get pet name

            lifecycleScope.launch {

                val petName: String

                if (expense.petId == null) {

                    petName = "All pets"

                } else {

                    val pet =
                        PetCareDatabase
                            .getDatabase(
                                this@SpendingActivity
                            )
                            .petDao()
                            .getPetById(
                                expense.petId
                            )

                    petName =
                        pet?.name
                            ?: "Unknown pet"
                }


                // Second line
                //
                // Max · 2026-09-10 · Kibble food

                detailsText.text =
                    if (expense.note.isBlank()) {

                        "$petName · ${expense.date}"

                    } else {

                        "$petName · ${expense.date} · ${expense.note}"
                    }
            }


            // Delete button

            deleteButton.setOnClickListener {

                showDeleteExpenseDialog(
                    expense
                )
            }


            // Add card to screen

            expenseListContainer.addView(
                expenseView
            )
        }
    }


    // ADD EXPENSE DIALOG


    private fun showAddExpenseDialog() {


        // Inflate dialog layout

        val dialogView =
            layoutInflater.inflate(
                R.layout.dialog_add_expense,
                null
            )


        // Create dialog


        val dialog =
            AlertDialog.Builder(this)
                .setView(dialogView)
                .create()


        // Find dialog views


        val amountInput =
            dialogView.findViewById<EditText>(
                R.id.expenseAmount
            )

        val categorySpinner =
            dialogView.findViewById<Spinner>(
                R.id.expenseCategory
            )

        val petSpinner =
            dialogView.findViewById<Spinner>(
                R.id.expensePet
            )

        val dateInput =
            dialogView.findViewById<EditText>(
                R.id.expenseDate
            )

        val noteInput =
            dialogView.findViewById<EditText>(
                R.id.expenseNote
            )


        val closeButton =
            dialogView.findViewById<ImageButton>(
                R.id.closeExpenseDialog
            )

        val saveButton =
            dialogView.findViewById<TextView>(
                R.id.saveExpenseButton
            )


        // CATEGORY SPINNER

        val categories =
            listOf(
                "Food",
                "Grooming",
                "Vet care",
                "Medication",
                "Toys",
                "Supplies",
                "Training",
                "Other"
            )

        val categoryAdapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )

        categorySpinner.adapter =
            categoryAdapter

        // DATE


        dateInput.setText(
            dateFormat.format(
                Calendar.getInstance().time
            )
        )

        dateInput.setOnClickListener {

            showDatePicker(
                dateInput
            )
        }

        // LOAD PETS

        lifecycleScope.launch {

            PetCareDatabase
                .getDatabase(
                    this@SpendingActivity
                )
                .petDao()
                .getPetsForUser(
                    userId
                )
                .collect { pets ->

                    setupPetSpinner(
                        petSpinner,
                        pets
                    )
                }
        }


        // CLOSE BUTTON


        closeButton.setOnClickListener {

            dialog.dismiss()
        }


        // SAVE BUTTON

        saveButton.setOnClickListener {

            saveExpense(
                dialog = dialog,
                amountInput = amountInput,
                categorySpinner = categorySpinner,
                petSpinner = petSpinner,
                dateInput = dateInput,
                noteInput = noteInput
            )
        }

        // SHOW DIALOG


        dialog.show()



        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )



        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.86).toInt(),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }


    // PET SPINNER


    private fun setupPetSpinner(
        spinner: Spinner,
        pets: List<Pet>
    ) {

        // First option
        val petNames =
            mutableListOf(
                "All pets"
            )

        // Add actual pet names
        petNames.addAll(
            pets.map { pet ->
                pet.name
            }
        )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                petNames
            )

        spinner.adapter =
            adapter

        // Keep actual Pet objects available
        // when the user selects one.

        spinner.tag =
            pets
    }


    // SAVE EXPENSE

    private fun saveExpense(
        dialog: AlertDialog,
        amountInput: EditText,
        categorySpinner: Spinner,
        petSpinner: Spinner,
        dateInput: EditText,
        noteInput: EditText
    ) {


        // AMOUNT


        val amountText =
            amountInput.text
                .toString()
                .trim()

        if (amountText.isBlank()) {

            amountInput.error =
                "Enter an amount"

            amountInput.requestFocus()

            return
        }

        val amount =
            amountText.toDoubleOrNull()

        if (amount == null || amount <= 0) {

            amountInput.error =
                "Enter a valid amount"

            amountInput.requestFocus()

            return
        }


        // CATEGORY

        val category =
            categorySpinner
                .selectedItem
                ?.toString()
                ?: "Other"


        // PET

        val selectedPetIndex =
            petSpinner.selectedItemPosition

        val pets =
            petSpinner.tag as? List<Pet>

        val selectedPet =
            if (selectedPetIndex == 0) {

                // All pets
                null

            } else {

                pets?.getOrNull(
                    selectedPetIndex - 1
                )
            }

        val petId =
            selectedPet?.id

     // DATE
        val selectedDate =
            parseDisplayDate(
                dateInput.text
                    .toString()
                    .trim()
            )

        if (selectedDate == null) {

            Toast.makeText(
                this,
                "Please select a valid date",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        // CREATE EXPENSE


        val expense =
            Expense(

                userId = userId,

                petId = petId,

                amount = amount,

                category = category,

                date =
                    databaseDateFormat.format(
                        selectedDate
                    ),

                note =
                    noteInput.text
                        .toString()
                        .trim()
            )


        // SAVE TO ROOM

        lifecycleScope.launch {

            try {

                repository.addExpense(
                    expense
                )


                // Close "Log an expense"


                dialog.dismiss()


                // Show success confirmation


                showExpenseSavedDialog()

            } catch (exception: Exception) {

                Toast.makeText(
                    this@SpendingActivity,
                    "Unable to save expense",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    // EXPENSE SAVED DIALOG


    private fun showExpenseSavedDialog() {

        val dialog =
            AlertDialog.Builder(this)
                .setTitle("Expense saved")
                .setMessage(
                    "Your expense has been successfully saved."
                )
                .setPositiveButton("OK") { dialogInterface, _ ->

                    dialogInterface.dismiss()
                }
                .create()

        dialog.show()
    }


    // DATE PICKER


    private fun showDatePicker(
        dateInput: EditText
    ) {

        val calendar =
            Calendar.getInstance()

        DatePickerDialog(

            this,

            { _, year, month, dayOfMonth ->

                calendar.set(
                    Calendar.YEAR,
                    year
                )

                calendar.set(
                    Calendar.MONTH,
                    month
                )

                calendar.set(
                    Calendar.DAY_OF_MONTH,
                    dayOfMonth
                )

                dateInput.setText(
                    dateFormat.format(
                        calendar.time
                    )
                )
            },

            calendar.get(
                Calendar.YEAR
            ),

            calendar.get(
                Calendar.MONTH
            ),

            calendar.get(
                Calendar.DAY_OF_MONTH
            )

        ).show()
    }


    // PARSE DATE

    private fun parseDisplayDate(
        value: String
    ) = try {

        dateFormat.parse(
            value
        )

    } catch (exception: Exception) {

        null
    }

    // DELETE EXPENSE


    private fun showDeleteExpenseDialog(
        expense: Expense
    ) {

        AlertDialog.Builder(this)
            .setTitle(
                "Delete expense?"
            )
            .setMessage(
                "This expense will be permanently removed."
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

                        repository.deleteExpense(
                            expense
                        )

                        Toast.makeText(
                            this@SpendingActivity,
                            "Expense deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (exception: Exception) {

                        Toast.makeText(
                            this@SpendingActivity,
                            "Unable to delete expense",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }


    // BOTTOM NAVIGATION

    private fun setupNavigation() {


        // HOME


        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            openActivity(
                DashboardActivity::class.java
            )
        }


        // PETS


        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            openActivity(
                PetsActivity::class.java
            )
        }

        // CHECKLIST


        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            openActivity(
                ChecklistActivity::class.java
            )
        }

        //
        // PLACES


        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {

            openActivity(
                PlacesActivity::class.java
            )
        }


        // SPEND


        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            // Already on Spending screen.
        }
    }


    // OPEN ACTIVITY


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
    }
}