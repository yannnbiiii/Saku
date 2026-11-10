package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.repository.BudgetRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ExpenseRepository
import com.example.ui.components.BudgetStatus
import com.example.ui.components.CategoryExpenseSummary
import com.example.ui.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FinanceViewModel(
    private val userId: String,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(Formatters.currentMonth())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterCategoryId = MutableStateFlow<String?>(null)
    val filterCategoryId: StateFlow<String?> = _filterCategoryId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private val _categoriesState = MutableStateFlow<List<Category>>(
        listOf(
            Category(id = "cat_1", name = "Makanan & Minuman", iconName = "restaurant", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_2", name = "Transportasi", iconName = "commute", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_3", name = "Hiburan", iconName = "movie", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_4", name = "Tagihan", iconName = "receipt", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_5", name = "Belanja", iconName = "shopping_bag", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_6", name = "Kesehatan", iconName = "medical", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_7", name = "Pendidikan", iconName = "school", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_8", name = "Tabungan & Investasi", iconName = "savings", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_9", name = "Tempat Tinggal", iconName = "home", colorHex = "#10B981", isDefault = true),
            Category(id = "cat_10", name = "Lain-lain", iconName = "category", colorHex = "#10B981", isDefault = true)
        )
    )
    val categories: StateFlow<List<Category>> = _categoriesState.asStateFlow()

    private val _expensesState = MutableStateFlow<List<Expense>>(emptyList())
    val allExpenses: StateFlow<List<Expense>> = _expensesState.asStateFlow()

    private val _budgetsState = MutableStateFlow<List<Budget>>(emptyList())
    val allBudgets: StateFlow<List<Budget>> = _budgetsState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                categoryRepository.seedDefaultsIfEmpty(userId)
            } catch (e: Exception) {
                // Ignore cloud sync error
            }
        }
        if (_categoriesState.value.isEmpty()) {
            _categoriesState.value = listOf(
                Category(id = "cat_1", name = "Makanan & Minuman", iconName = "restaurant", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_2", name = "Transportasi", iconName = "commute", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_3", name = "Hiburan", iconName = "movie", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_4", name = "Tagihan", iconName = "receipt", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_5", name = "Belanja", iconName = "shopping_bag", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_6", name = "Kesehatan", iconName = "medical", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_7", name = "Pendidikan", iconName = "school", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_8", name = "Tabungan & Investasi", iconName = "savings", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_9", name = "Tempat Tinggal", iconName = "home", colorHex = "#10B981", isDefault = true),
                Category(id = "cat_10", name = "Lain-lain", iconName = "category", colorHex = "#10B981", isDefault = true)
            )
        }
    }

    // Filtered monthly expenses
    val monthlyExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _selectedMonth,
        _searchQuery,
        _filterCategoryId
    ) { expenses, month, query, catFilter ->
        expenses.filter { exp ->
            exp.date.startsWith(month) &&
                (catFilter == null || exp.categoryId == catFilter) &&
                (query.isBlank() || exp.title.contains(query, ignoreCase = true) || exp.notes.contains(query, ignoreCase = true))
        }.sortedByDescending { it.date }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Total expenses in the selected month
    val totalMonthlyExpense: StateFlow<Double> = combine(
        allExpenses,
        _selectedMonth
    ) { expenses, month ->
        expenses.filter { it.date.startsWith(month) }.sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = 0.0
    )

    // Category Expense Summaries for Bar Chart
    val categorySummaries: StateFlow<List<CategoryExpenseSummary>> = combine(
        allExpenses,
        categories,
        _selectedMonth
    ) { expenses, cats, month ->
        val currentMonthExpenses = expenses.filter { it.date.startsWith(month) }
        val total = currentMonthExpenses.sumOf { it.amount }
        if (total <= 0.0) return@combine emptyList()

        val grouped = currentMonthExpenses.groupBy { it.categoryId }
        val summaries = grouped.map { (catId, items) ->
            val cat = cats.firstOrNull { it.id == catId }
            val catTotal = items.sumOf { it.amount }
            CategoryExpenseSummary(
                categoryId = catId,
                categoryName = cat?.name ?: items.firstOrNull()?.categoryName ?: "Lainnya",
                iconName = cat?.iconName ?: "category",
                colorHex = cat?.colorHex ?: "#10B981",
                totalAmount = catTotal,
                percentage = (catTotal / total).toFloat()
            )
        }.sortedByDescending { it.totalAmount }

        summaries
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Category Budgets and Warnings for the selected month
    val budgetStatuses: StateFlow<List<BudgetStatus>> = combine(
        allBudgets,
        allExpenses,
        categories,
        _selectedMonth
    ) { budgets, expenses, cats, month ->
        val currentMonthBudgets = budgets.filter { it.month == month }
        val currentMonthExpenses = expenses.filter { it.date.startsWith(month) }

        currentMonthBudgets.map { budget ->
            val spent = currentMonthExpenses.filter { it.categoryId == budget.categoryId }.sumOf { it.amount }
            val cat = cats.firstOrNull { it.id == budget.categoryId }
            BudgetStatus(
                budget = budget,
                spent = spent,
                iconName = cat?.iconName ?: "category",
                colorHex = cat?.colorHex ?: "#10B981"
            )
        }.sortedByDescending { it.ratio }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Budget warnings (alert list for banner and dashboard)
    val budgetAlerts: StateFlow<List<BudgetStatus>> = budgetStatuses.combine(_selectedMonth) { statuses, _ ->
        statuses.filter { it.isNearLimit || it.isExceeded }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterCategoryId(catId: String?) {
        _filterCategoryId.value = catId
    }

    fun previousMonth() {
        changeMonthOffset(-1)
    }

    fun nextMonth() {
        changeMonthOffset(1)
    }

    private fun changeMonthOffset(offset: Int) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(_selectedMonth.value) ?: return
            val cal = Calendar.getInstance().apply { time = date }
            cal.add(Calendar.MONTH, offset)
            _selectedMonth.value = sdf.format(cal.time)
        } catch (e: Exception) {
            // Ignore
        }
    }

    // Expense CRUD
    fun addExpense(title: String, amount: Double, categoryId: String, categoryName: String, date: String, notes: String) {
        val exp = Expense(
            id = "exp_${System.currentTimeMillis()}",
            userId = userId,
            title = title,
            amount = amount,
            categoryId = categoryId,
            categoryName = categoryName,
            date = date,
            notes = notes
        )
        val current = _expensesState.value.toMutableList()
        current.add(0, exp)
        _expensesState.value = current
        _userMessage.value = "Pengeluaran berhasil dicatat"

        viewModelScope.launch {
            try {
                expenseRepository.addExpense(exp, userId)
            } catch (e: Exception) {
                // Local save succeeded
            }
        }
    }

    fun updateExpense(expense: Expense) {
        val current = _expensesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == expense.id }
        if (index >= 0) {
            current[index] = expense
            _expensesState.value = current
            _userMessage.value = "Pengeluaran berhasil diperbarui"
        }
        viewModelScope.launch {
            try {
                expenseRepository.updateExpense(expense, userId)
            } catch (e: Exception) {}
        }
    }

    fun deleteExpense(expenseId: String) {
        val current = _expensesState.value.toMutableList()
        current.removeAll { it.id == expenseId }
        _expensesState.value = current
        _userMessage.value = "Pengeluaran berhasil dihapus"
        viewModelScope.launch {
            try {
                if (!expenseId.startsWith("exp_")) {
                    expenseRepository.deleteExpense(expenseId, userId)
                }
            } catch (e: Exception) {}
        }
    }

    // Category CRUD
    fun addCategory(name: String, iconName: String, colorHex: String) {
        val current = _categoriesState.value.toMutableList()
        if (current.none { it.name.equals(name, ignoreCase = true) }) {
            val newCat = Category(
                id = "cat_${System.currentTimeMillis()}",
                userId = userId,
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            current.add(newCat)
            _categoriesState.value = current
            _userMessage.value = "Kategori '$name' berhasil ditambahkan"
        }
        viewModelScope.launch {
            try {
                categoryRepository.addCategory(Category(userId = userId, name = name, iconName = iconName, colorHex = colorHex, isDefault = false), userId)
            } catch (e: Exception) {}
        }
    }

    fun updateCategory(category: Category) {
        val current = _categoriesState.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category
            _categoriesState.value = current
            _userMessage.value = "Kategori berhasil diperbarui"
        }
        viewModelScope.launch {
            try {
                categoryRepository.updateCategory(category, userId)
            } catch (e: Exception) {}
        }
    }

    fun deleteCategory(categoryId: String) {
        val current = _categoriesState.value.toMutableList()
        current.removeAll { it.id == categoryId }
        _categoriesState.value = current
        _userMessage.value = "Kategori berhasil dihapus"
        viewModelScope.launch {
            try {
                if (categoryId.isNotBlank() && !categoryId.startsWith("cat_")) {
                    categoryRepository.deleteCategory(categoryId, userId)
                }
            } catch (e: Exception) {}
        }
    }

    // Budget CRUD
    fun saveBudget(categoryId: String, categoryName: String, monthlyLimit: Double, month: String) {
        val budget = Budget(
            id = "bud_${System.currentTimeMillis()}",
            userId = userId,
            categoryId = categoryId,
            categoryName = categoryName,
            monthlyLimit = monthlyLimit,
            month = month
        )
        val current = _budgetsState.value.toMutableList()
        current.removeAll { it.categoryId == categoryId && it.month == month }
        current.add(budget)
        _budgetsState.value = current
        _userMessage.value = "Anggaran bulanan untuk '$categoryName' berhasil disimpan"

        viewModelScope.launch {
            try {
                budgetRepository.saveBudget(budget, userId)
            } catch (e: Exception) {}
        }
    }

    fun deleteBudget(budgetId: String) {
        val current = _budgetsState.value.toMutableList()
        current.removeAll { it.id == budgetId }
        _budgetsState.value = current
        _userMessage.value = "Anggaran berhasil dihapus"
        viewModelScope.launch {
            try {
                if (!budgetId.startsWith("bud_")) {
                    budgetRepository.deleteBudget(budgetId, userId)
                }
            } catch (e: Exception) {}
        }
    }
}

class FinanceViewModelFactory(
    private val userId: String,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val budgetRepository: BudgetRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return FinanceViewModel(userId, expenseRepository, categoryRepository, budgetRepository) as T
    }
}
