package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Budget
import com.example.data.model.Category
import com.example.data.model.Expense
import com.example.data.repository.BudgetRepository
import com.example.data.repository.CategoryRepository
import com.example.data.repository.ExpenseRepository
import com.example.ui.auth.signOutUser
import com.example.ui.dialogs.AddEditCategoryDialog
import com.example.ui.dialogs.AddEditExpenseDialog
import com.example.ui.dialogs.ExportCsvDialog
import com.example.ui.dialogs.SetBudgetDialog
import com.example.ui.dialogs.UserProfileDialog
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.FinanceViewModelFactory
import kotlinx.coroutines.launch
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureScreen(
    currentUserId: String,
    isDarkTheme: Boolean = true,
    onToggleDarkTheme: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val expenseRepository = remember { ExpenseRepository(context) }
    val categoryRepository = remember { CategoryRepository(context) }
    val budgetRepository = remember { BudgetRepository(context) }

    val viewModel: FinanceViewModel = viewModel(
        key = currentUserId,
        factory = FinanceViewModelFactory(currentUserId, expenseRepository, categoryRepository, budgetRepository)
    )

    var currentTab by remember { mutableIntStateOf(0) } // 0: Ringkasan, 1: Pengeluaran, 2: Anggaran, 3: Kategori
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var showSetBudgetDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<Budget?>(null) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var userAlias by remember { mutableStateOf("") }
    var showUserProfileDialog by remember { mutableStateOf(false) }

    val categories by viewModel.categories.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val monthlyExpenses by viewModel.monthlyExpenses.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearUserMessage()
        }
    }

    val currentUser = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showUserProfileDialog = true }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Saku",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.5).sp
                                )
                            )
                            val displayUser = userAlias.ifBlank {
                                val firebaseEmail = currentUser?.email
                                val firebaseName = currentUser?.displayName
                                when {
                                    !firebaseName.isNullOrBlank() -> firebaseName
                                    !firebaseEmail.isNullOrBlank() -> firebaseEmail
                                    currentUserId.startsWith("google_") -> {
                                        currentUserId.removePrefix("google_")
                                            .replace("_at_", "@")
                                            .replace("_", ".")
                                    }
                                    currentUserId.startsWith("email_") -> {
                                        currentUserId.removePrefix("email_")
                                            .replace("_at_", "@")
                                            .replace("_", ".")
                                    }
                                    currentUserId == "user_saku_demo" -> "Mode Demo"
                                    else -> "Pengguna"
                                }
                            }
                            Text(
                                text = displayUser,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Ubah Tema",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_csv_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Ekspor CSV",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { showSignOutDialog = true },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Keluar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Insights, contentDescription = "Ringkasan") },
                    label = { Text("Ringkasan") },
                    modifier = Modifier.testTag("nav_dashboard")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Pengeluaran") },
                    label = { Text("Pengeluaran") },
                    modifier = Modifier.testTag("nav_expenses")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Anggaran") },
                    label = { Text("Anggaran") },
                    modifier = Modifier.testTag("nav_budgets")
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Category, contentDescription = "Kategori") },
                    label = { Text("Kategori") },
                    modifier = Modifier.testTag("nav_categories")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 0 || currentTab == 1) {
                FloatingActionButton(
                    onClick = { showAddExpenseDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_add_expense")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Pengeluaran"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentTab) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToExpenses = { currentTab = 1 },
                    onNavigateToBudgets = { currentTab = 2 },
                    onEditExpense = { exp -> expenseToEdit = exp },
                    onAddExpense = { showAddExpenseDialog = true },
                    onExportCsv = { showExportDialog = true }
                )
                1 -> ExpensesScreen(
                    viewModel = viewModel,
                    onEditExpense = { exp -> expenseToEdit = exp },
                    onExportCsv = { showExportDialog = true }
                )
                2 -> BudgetScreen(
                    viewModel = viewModel,
                    onSetBudget = { b ->
                        budgetToEdit = b
                        showSetBudgetDialog = true
                    }
                )
                3 -> CategoryManagementScreen(
                    viewModel = viewModel,
                    onAddCategory = { showAddCategoryDialog = true },
                    onEditCategory = { cat ->
                        categoryToEdit = cat
                        showAddCategoryDialog = true
                    }
                )
            }
        }
    }

    // Dialog: Add / Edit Expense
    if (showAddExpenseDialog || expenseToEdit != null) {
        AddEditExpenseDialog(
            categories = categories,
            existingExpense = expenseToEdit,
            onDismiss = {
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onSave = { title, amount, categoryId, categoryName, date, notes ->
                if (expenseToEdit != null) {
                    val updated = expenseToEdit!!.copy(
                        title = title,
                        amount = amount,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        date = date,
                        notes = notes
                    )
                    viewModel.updateExpense(updated)
                } else {
                    viewModel.addExpense(title, amount, categoryId, categoryName, date, notes)
                }
                showAddExpenseDialog = false
                expenseToEdit = null
            },
            onAddCategory = { name, iconName, colorHex ->
                viewModel.addCategory(name, iconName, colorHex)
            }
        )
    }

    // Dialog: Add / Edit Category
    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            existingCategory = categoryToEdit,
            onDismiss = {
                showAddCategoryDialog = false
                categoryToEdit = null
            },
            onSave = { name, iconName, colorHex ->
                if (categoryToEdit != null) {
                    val updated = categoryToEdit!!.copy(
                        name = name,
                        iconName = iconName,
                        colorHex = colorHex
                    )
                    viewModel.updateCategory(updated)
                } else {
                    viewModel.addCategory(name, iconName, colorHex)
                }
                showAddCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    // Dialog: Set / Edit Budget
    if (showSetBudgetDialog) {
        SetBudgetDialog(
            categories = categories,
            month = selectedMonth,
            existingBudget = budgetToEdit,
            onDismiss = {
                showSetBudgetDialog = false
                budgetToEdit = null
            },
            onSave = { categoryId, categoryName, monthlyLimit, month ->
                viewModel.saveBudget(categoryId, categoryName, monthlyLimit, month)
                showSetBudgetDialog = false
                budgetToEdit = null
            },
            onAddCategory = { name, iconName, colorHex ->
                viewModel.addCategory(name, iconName, colorHex)
            }
        )
    }

    // Dialog: Sign Out Confirmation
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Keluar Akun?") },
            text = { Text("Apakah Anda yakin ingin keluar dari akun ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        signOutUser(credentialManager, coroutineScope) {
                            onSignOut()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Keluar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog: Export CSV
    if (showExportDialog) {
        ExportCsvDialog(
            selectedMonth = selectedMonth,
            monthlyExpenses = monthlyExpenses,
            allExpenses = allExpenses,
            onDismiss = { showExportDialog = false },
            onExportComplete = { msg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(msg)
                }
            }
        )
    }

    // Dialog: User Profile & Security
    if (showUserProfileDialog) {
        val userEmail = currentUser?.email ?: when {
            currentUserId.startsWith("google_") -> currentUserId.removePrefix("google_").replace("_at_", "@").replace("_", ".")
            currentUserId.startsWith("email_") -> currentUserId.removePrefix("email_").replace("_at_", "@").replace("_", ".")
            else -> "Akun Demo / Tamu"
        }
        UserProfileDialog(
            userEmail = userEmail,
            currentAlias = userAlias,
            onDismiss = { showUserProfileDialog = false },
            onSaveAlias = { newAlias ->
                userAlias = newAlias
            }
        )
    }
}
