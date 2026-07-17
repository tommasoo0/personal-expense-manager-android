package com.expenses.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.expenses.mobile.data.AccountEntity
import com.expenses.mobile.data.AppDatabase
import com.expenses.mobile.data.CategoryEntity
import com.expenses.mobile.data.TransactionEntity
import com.expenses.mobile.ui.theme.GestoreSpeseAndroidTheme
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private const val TYPE_EXPENSE = "EXPENSE"
private const val TYPE_INCOME = "INCOME"
private const val SALARY_MONTH_START_DAY = 10
private val defaultCategories = listOf("Cibo", "Trasporti", "Casa", "Salute", "Intrattenimento", "Stipendio", "Altro")
private val defaultAccounts = listOf("Contanti", "Conto Bancario")
private val monthLabels = listOf("Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic")
private val chartPalette = listOf(
    Color(0xFF2D6CDF),
    Color(0xFF16A163),
    Color(0xFFF59E0B),
    Color(0xFFD84A4A),
    Color(0xFF7C3AED),
    Color(0xFF0891B2),
    Color(0xFFDB2777),
    Color(0xFF5F8D22)
)
private val chartIncomeColor = Color(0xFF16865C)
private val chartExpenseColor = Color(0xFFD84A4A)
private val dashboardCardColor = Color(0xFF103B4D)
private enum class ReferenceKind { Category, Account }
private data class CategoryTotal(val label: String, val value: Long, val color: Color)
private data class MonthlyTotal(val label: String, val income: Long, val expenses: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GestoreSpeseAndroidTheme {
                ExpenseApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseApp() {
    val context = LocalContext.current
    val database = remember { AppDatabase.get(context) }
    val dao = remember(database) { database.transactionDao() }
    val referenceDao = remember(database) { database.referenceDao() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedMonth by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(currentYear()) }
    var selectedAccount by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var showCharts by remember { mutableStateOf(false) }
    var showReferenceManager by remember { mutableStateOf(false) }
    var deletingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val transactions by dao.observeFiltered(null, null, selectedAccount)
        .collectAsState(initial = emptyList())
    val savedCategories by referenceDao.observeCategories().collectAsState(initial = emptyList())
    val savedAccounts by referenceDao.observeAccounts().collectAsState(initial = emptyList())
    val savedYears by dao.observeYears().collectAsState(initial = emptyList())

    val categories = remember(savedCategories) { savedCategories }
    val accounts = remember(savedAccounts) { savedAccounts }
    val years = remember(savedYears) {
        (listOf(currentYear()) + savedYears)
            .filter { it.isNotBlank() }
            .distinct()
            .sortedDescending()
    }
    val periodTransactions = remember(transactions, selectedMonth, selectedYear) {
        transactions.filterBySalaryPeriod(selectedMonth, selectedYear)
    }
    val periodLabel = remember(selectedMonth, selectedYear) {
        salaryPeriodLabel(selectedMonth, selectedYear)
    }
    val chartFilteredTransactions = remember(periodTransactions, searchQuery) {
        periodTransactions.filterBySearch(searchQuery)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            errorMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestore Spese",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    TextButton(onClick = { showCharts = true }) {
                        Text("Grafici")
                    }
                    TextButton(onClick = { showReferenceManager = true }) {
                        Text("Gestisci")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingTransaction = null
                    showEditor = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("Aggiungi")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        ExpenseHomeScreen(
            transactions = periodTransactions,
            dashboardTransactions = periodTransactions,
            dashboardTitle = periodLabel,
            categories = categories,
            accounts = accounts,
            years = years,
            selectedMonth = selectedMonth,
            selectedYear = selectedYear,
            selectedAccount = selectedAccount,
            searchQuery = searchQuery,
            onMonthSelected = { selectedMonth = it },
            onYearSelected = { selectedYear = it },
            onAccountSelected = { selectedAccount = it },
            onSearchQueryChange = { searchQuery = it },
            onEdit = {
                editingTransaction = it
                showEditor = true
            },
            onDelete = { deletingTransaction = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        )
    }

    if (showCharts) {
        ChartsDialog(
            categoryTransactions = chartFilteredTransactions,
            monthlyTransactions = chartFilteredTransactions,
            selectedYear = selectedYear,
            selectedMonth = selectedMonth,
            onDismiss = { showCharts = false }
        )
    }

    if (showEditor) {
        TransactionEditorDialog(
            transaction = editingTransaction,
            categories = categories,
            accounts = accounts,
            onDismiss = { showEditor = false },
            onSave = { transaction ->
                scope.launch {
                    try {
                        if (transaction.id == 0L) {
                            dao.insert(transaction)
                        } else {
                            dao.update(transaction)
                        }
                        referenceDao.insertCategory(CategoryEntity(transaction.category))
                        referenceDao.insertAccount(AccountEntity(transaction.account))
                        showEditor = false
                    } catch (error: Exception) {
                        errorMessage = "Errore salvataggio: ${error.message}"
                    }
                }
            }
        )
    }

    if (showReferenceManager) {
        ReferenceManagerDialog(
            categories = categories,
            accounts = accounts,
            onDismiss = { showReferenceManager = false },
            onAddCategory = { name ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.insertCategory(CategoryEntity(name.trim())) },
                        onError = { errorMessage = it }
                    )
                }
            },
            onRenameCategory = { oldName, newName ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.renameCategory(oldName, newName.trim()) },
                        onError = { errorMessage = it }
                    )
                }
            },
            onDeleteCategory = { name ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.deleteCategoryByName(name) },
                        onError = { errorMessage = it }
                    )
                }
            },
            onAddAccount = { name ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.insertAccount(AccountEntity(name.trim())) },
                        onError = { errorMessage = it }
                    )
                }
            },
            onRenameAccount = { oldName, newName ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.renameAccount(oldName, newName.trim()) },
                        onError = { errorMessage = it }
                    )
                }
            },
            onDeleteAccount = { name ->
                scope.launch {
                    runReferenceAction(
                        action = { referenceDao.deleteAccountByName(name) },
                        onError = { errorMessage = it }
                    )
                }
            }
        )
    }

    deletingTransaction?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deletingTransaction = null },
            title = { Text("Eliminare la transazione?") },
            text = {
                Text("${transaction.date} - ${transaction.description} - ${formatCurrency(transaction.amountCents)}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                dao.delete(transaction)
                                deletingTransaction = null
                            } catch (error: Exception) {
                                deletingTransaction = null
                                errorMessage = "Errore eliminazione: ${error.message}"
                            }
                        }
                    }
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingTransaction = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@Composable
private fun ChartsDialog(
    categoryTransactions: List<TransactionEntity>,
    monthlyTransactions: List<TransactionEntity>,
    selectedYear: String?,
    selectedMonth: String?,
    onDismiss: () -> Unit
) {
    val categoryTotals = remember(categoryTransactions) {
        categoryTransactions.toCategoryTotals()
    }
    val monthlyTotals = remember(monthlyTransactions, selectedYear, selectedMonth) {
        monthlyTransactions.toMonthlyTotals(selectedYear, selectedMonth)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grafici") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ChartCard(title = "Uscite per categoria") {
                    CategoryPieChart(categoryTotals = categoryTotals)
                    ChartLegend(categoryTotals = categoryTotals)
                }
                ChartCard(title = chartPeriodTitle(selectedMonth, selectedYear)) {
                    MonthlyBarsChart(monthlyTotals = monthlyTotals)
                    MonthlyTotalsLegend(monthlyTotals = monthlyTotals)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )
}

@Composable
private fun ChartCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                content()
            }
        )
    }
}

@Composable
private fun CategoryPieChart(categoryTotals: List<CategoryTotal>) {
    val totalAmount = categoryTotals.sumOf { it.value }

    if (totalAmount == 0L) {
        Text(
            "Nessuna uscita da mostrare.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
    ) {
        val diameter = min(size.width, size.height)
        val chartSize = Size(diameter, diameter)
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        var startAngle = -90f

        categoryTotals.forEach { total ->
            val sweepAngle = total.value.toFloat() / totalAmount.toFloat() * 360f
            drawArc(
                color = total.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = topLeft,
                size = chartSize
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun ChartLegend(categoryTotals: List<CategoryTotal>) {
    if (categoryTotals.isEmpty()) {
        return
    }

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoryTotals.forEach { total ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(total.color, RoundedCornerShape(3.dp))
                )
                Text(
                    "${total.label} ${formatCurrency(total.value)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun MonthlyBarsChart(monthlyTotals: List<MonthlyTotal>) {
    val maxAmount = monthlyTotals.maxOfOrNull { max(it.income, it.expenses) } ?: 0L

    if (maxAmount == 0L) {
        Text(
            "Nessun movimento da mostrare.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val groupCount = monthlyTotals.size.coerceAtLeast(1)
        val groupWidth = size.width / groupCount
        val barGap = 2.dp.toPx()
        val barWidth = (groupWidth * 0.28f).coerceAtMost(18.dp.toPx())
        val maxBarHeight = size.height * 0.9f
        val baseY = size.height

        monthlyTotals.forEachIndexed { index, total ->
            val centerX = groupWidth * index + groupWidth / 2f
            val incomeHeight = total.income.toFloat() / maxAmount.toFloat() * maxBarHeight
            val expenseHeight = total.expenses.toFloat() / maxAmount.toFloat() * maxBarHeight

            if (incomeHeight > 0f) {
                drawRect(
                    color = chartIncomeColor,
                    topLeft = Offset(centerX - barWidth - barGap, baseY - incomeHeight),
                    size = Size(barWidth, incomeHeight)
                )
            }
            if (expenseHeight > 0f) {
                drawRect(
                    color = chartExpenseColor,
                    topLeft = Offset(centerX + barGap, baseY - expenseHeight),
                    size = Size(barWidth, expenseHeight)
                )
            }
        }
    }
}

@Composable
private fun MonthlyTotalsLegend(monthlyTotals: List<MonthlyTotal>) {
    val visibleTotals = monthlyTotals.filter { it.income > 0L || it.expenses > 0L }
    if (visibleTotals.isEmpty()) {
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ChartColorLabel(color = chartIncomeColor, label = "Entrate")
        ChartColorLabel(color = chartExpenseColor, label = "Uscite")
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        visibleTotals.forEach { total ->
            AssistChip(
                onClick = {},
                label = {
                    Text("${total.label} +${formatCurrency(total.income)} -${formatCurrency(total.expenses)}")
                }
            )
        }
    }
}

@Composable
private fun ChartColorLabel(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private suspend fun runReferenceAction(
    action: suspend () -> Unit,
    onError: (String) -> Unit
) {
    try {
        action()
    } catch (error: Exception) {
        onError("Errore gestione valori: ${error.message}")
    }
}

@Composable
private fun ReferenceManagerDialog(
    categories: List<String>,
    accounts: List<String>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddAccount: (String) -> Unit,
    onRenameAccount: (String, String) -> Unit,
    onDeleteAccount: (String) -> Unit
) {
    var editingValue by remember { mutableStateOf<Pair<ReferenceKind, String>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categorie e conti") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                ReferenceSection(
                    title = "Categorie",
                    values = categories,
                    onAdd = onAddCategory,
                    onRename = { editingValue = ReferenceKind.Category to it },
                    onDelete = onDeleteCategory
                )
                ReferenceSection(
                    title = "Conti",
                    values = accounts,
                    onAdd = onAddAccount,
                    onRename = { editingValue = ReferenceKind.Account to it },
                    onDelete = onDeleteAccount
                )
                Text(
                    "Rinominando un valore vengono aggiornate anche le transazioni esistenti.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Chiudi")
            }
        }
    )

    editingValue?.let { (kind, oldName) ->
        RenameReferenceDialog(
            title = if (kind == ReferenceKind.Category) "Rinomina categoria" else "Rinomina conto",
            oldName = oldName,
            onDismiss = { editingValue = null },
            onConfirm = { newName ->
                if (kind == ReferenceKind.Category) {
                    onRenameCategory(oldName, newName)
                } else {
                    onRenameAccount(oldName, newName)
                }
                editingValue = null
            }
        )
    }
}

@Composable
private fun ReferenceSection(
    title: String,
    values: List<String>,
    onAdd: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var newValue by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newValue,
                onValueChange = {
                    newValue = it
                    validationError = null
                },
                label = { Text("Nuovo") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    val cleanValue = newValue.trim()
                    when {
                        cleanValue.isBlank() -> validationError = "Inserisci un nome."
                        values.any { it.equals(cleanValue, ignoreCase = true) } -> validationError = "Valore già presente."
                        else -> {
                            onAdd(cleanValue)
                            newValue = ""
                        }
                    }
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Aggiungi")
            }
        }
        validationError?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (values.isEmpty()) {
            Text(
                "Nessun valore salvato.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            values.forEach { value ->
                ReferenceRow(
                    value = value,
                    onRename = { onRename(value) },
                    onDelete = { onDelete(value) }
                )
            }
        }
    }
}

@Composable
private fun ReferenceRow(
    value: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(value, modifier = Modifier.weight(1f))
            TextButton(onClick = onRename) {
                Text("Rinomina")
            }
            TextButton(onClick = onDelete) {
                Text("Rimuovi")
            }
        }
    }
}

@Composable
private fun RenameReferenceDialog(
    title: String,
    oldName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember(oldName) { mutableStateOf(oldName) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = {
                        newName = it
                        validationError = null
                    },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cleanName = newName.trim()
                    if (cleanName.isBlank()) {
                        validationError = "Inserisci un nome."
                    } else {
                        onConfirm(cleanName)
                    }
                }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun ExpenseHomeScreen(
    transactions: List<TransactionEntity>,
    dashboardTransactions: List<TransactionEntity>,
    dashboardTitle: String,
    categories: List<String>,
    accounts: List<String>,
    years: List<String>,
    selectedMonth: String?,
    selectedYear: String?,
    selectedAccount: String?,
    searchQuery: String,
    onMonthSelected: (String?) -> Unit,
    onYearSelected: (String?) -> Unit,
    onAccountSelected: (String?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleTransactions = remember(transactions, searchQuery) {
        transactions.filterBySearch(searchQuery)
    }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            DashboardSection(
                dashboardTransactions = dashboardTransactions,
                title = dashboardTitle
            )
        }

        item {
            SearchField(
                query = searchQuery,
                onQueryChange = onSearchQueryChange
            )
        }

        item {
            FilterRow(
                years = years,
                accounts = accounts,
                selectedMonth = selectedMonth,
                selectedYear = selectedYear,
                selectedAccount = selectedAccount,
                onMonthSelected = onMonthSelected,
                onYearSelected = onYearSelected,
                onAccountSelected = onAccountSelected
            )
        }

        if (visibleTransactions.isEmpty()) {
            item {
                EmptyState(
                    message = if (searchQuery.isBlank()) {
                        "Nessuna transazione per questi filtri."
                    } else {
                        "Nessun risultato per \"$searchQuery\"."
                    }
                )
            }
        } else {
            items(visibleTransactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    onEdit = { onEdit(transaction) },
                    onDelete = { onDelete(transaction) }
                )
            }
        }
    }
}

@Composable
private fun DashboardSection(
    dashboardTransactions: List<TransactionEntity>,
    title: String
) {
    val balance = dashboardTransactions.fold(0L) { total, transaction ->
        if (transaction.type == TYPE_INCOME) {
            total + transaction.amountCents
        } else {
            total - transaction.amountCents
        }
    }
    val income = dashboardTransactions.filter { it.type == TYPE_INCOME }.sumOf { it.amountCents }
    val expenses = dashboardTransactions.filter { it.type == TYPE_EXPENSE }.sumOf { it.amountCents }
    val topCategory = dashboardTransactions
        .filter { it.type == TYPE_EXPENSE }
        .groupBy { it.category }
        .mapValues { (_, transactions) -> transactions.sumOf { it.amountCents } }
        .maxByOrNull { it.value }

    Card(
        colors = CardDefaults.cardColors(containerColor = dashboardCardColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                title,
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = formatCurrency(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricChip("Entrate", formatCurrency(income))
                MetricChip("Uscite", formatCurrency(expenses))
                MetricChip(
                    "Categoria top",
                    topCategory?.let { "${it.key} ${formatCurrency(it.value)}" } ?: "-"
                )
            }
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                label,
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text("Cerca") },
        placeholder = { Text("Descrizione, categoria, conto, importo") },
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        trailingIcon = {
            if (query.isNotBlank()) {
                TextButton(onClick = { onQueryChange("") }) {
                    Text("Pulisci")
                }
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FilterRow(
    years: List<String>,
    accounts: List<String>,
    selectedMonth: String?,
    selectedYear: String?,
    selectedAccount: String?,
    onMonthSelected: (String?) -> Unit,
    onYearSelected: (String?) -> Unit,
    onAccountSelected: (String?) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterDropdown(
            label = "Mese",
            selected = selectedMonth ?: "Tutti",
            options = listOf("Tutti") + (1..12).map { "%02d".format(it) },
            onSelected = { onMonthSelected(it.takeUnless { value -> value == "Tutti" }) }
        )
        FilterDropdown(
            label = "Anno",
            selected = selectedYear ?: "Tutti",
            options = listOf("Tutti") + years,
            onSelected = { onYearSelected(it.takeUnless { value -> value == "Tutti" }) }
        )
        FilterDropdown(
            label = "Conto",
            selected = selectedAccount ?: "Tutti",
            options = listOf("Tutti") + accounts,
            onSelected = { onAccountSelected(it.takeUnless { value -> value == "Tutti" }) }
        )
    }
}

@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text("$label: $selected")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelected(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isIncome = transaction.type == TYPE_INCOME
    val amountPrefix = if (isIncome) "+" else "-"
    val amountColor = if (isIncome) {
        chartIncomeColor
    } else {
        chartExpenseColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(74.dp)
                    .background(amountColor, RoundedCornerShape(6.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            transaction.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${transaction.date} \u00B7 ${transaction.category} \u00B7 ${transaction.account}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "$amountPrefix${formatCurrency(transaction.amountCents)}",
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onEdit) {
                        Text("Modifica")
                    }
                    TextButton(onClick = onDelete) {
                        Text("Elimina")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 34.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 34.dp)
        ) {
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
@Composable
private fun TransactionEditorDialog(
    transaction: TransactionEntity?,
    categories: List<String>,
    accounts: List<String>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var date by remember(transaction) { mutableStateOf(transaction?.date ?: todayIsoDate()) }
    var description by remember(transaction) { mutableStateOf(transaction?.description.orEmpty()) }
    var amount by remember(transaction) {
        mutableStateOf(transaction?.let { formatPlainAmount(it.amountCents) }.orEmpty())
    }
    var category by remember(transaction) { mutableStateOf(transaction?.category ?: categories.firstOrNull().orEmpty()) }
    var account by remember(transaction) { mutableStateOf(transaction?.account ?: accounts.firstOrNull().orEmpty()) }
    var type by remember(transaction) { mutableStateOf(transaction?.type ?: TYPE_EXPENSE) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (transaction == null) "Nuova transazione" else "Modifica transazione") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Data") },
                    placeholder = { Text("2026-05-08") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrizione") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importo") },
                    placeholder = { Text("12.50") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                SuggestionTextField(
                    label = "Categoria",
                    value = category,
                    suggestions = categories,
                    onValueChange = { category = it }
                )
                SuggestionTextField(
                    label = "Conto",
                    value = account,
                    suggestions = accounts,
                    onValueChange = { account = it }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TYPE_EXPENSE,
                        onClick = { type = TYPE_EXPENSE },
                        label = { Text("Uscita") }
                    )
                    FilterChip(
                        selected = type == TYPE_INCOME,
                        onClick = { type = TYPE_INCOME },
                        label = { Text("Entrata") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedDate = normalizeDate(date)
                    val parsedAmount = parseAmountToCents(amount)
                    when {
                        parsedDate == null -> validationError = "Inserisci una data valida in formato yyyy-MM-dd."
                        description.isBlank() -> validationError = "Inserisci una descrizione."
                        parsedAmount == null -> validationError = "Inserisci un importo valido."
                        category.isBlank() -> validationError = "Inserisci una categoria."
                        account.isBlank() -> validationError = "Inserisci un conto."
                        else -> onSave(
                            TransactionEntity(
                                id = transaction?.id ?: 0,
                                date = parsedDate,
                                description = description.trim(),
                                amountCents = parsedAmount,
                                category = category.trim(),
                                type = type,
                                account = account.trim()
                            )
                        )
                    }
                }
            ) {
                Text("Salva")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
private fun SuggestionTextField(
    label: String,
    value: String,
    suggestions: List<String>,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            suggestions.take(6).forEach { suggestion ->
                AssistChip(
                    onClick = { onValueChange(suggestion) },
                    label = { Text(suggestion) }
                )
            }
        }
    }
}

private fun todayIsoDate(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
}

private fun currentYear(): String {
    return SimpleDateFormat("yyyy", Locale.ROOT).format(Date())
}

private fun normalizeDate(value: String): String? {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
    }
    return try {
        formatter.format(formatter.parse(value.trim()) ?: return null)
    } catch (_: ParseException) {
        null
    }
}

private fun parseAmountToCents(value: String): Long? {
    val normalized = value.trim().let {
        if (it.contains(",") && it.contains(".")) {
            it.replace(".", "").replace(",", ".")
        } else {
            it.replace(",", ".")
        }
    }
    return try {
        val amount = BigDecimal(normalized).setScale(2, RoundingMode.HALF_UP)
        if (amount <= BigDecimal.ZERO) {
            null
        } else {
            amount.movePointRight(2).longValueExact()
        }
    } catch (_: NumberFormatException) {
        null
    } catch (_: ArithmeticException) {
        null
    }
}

private fun formatCurrency(cents: Long): String {
    return "€" + formatPlainAmount(cents)
}

private fun formatPlainAmount(cents: Long): String {
    return BigDecimal.valueOf(cents, 2)
        .setScale(2, RoundingMode.HALF_UP)
        .toPlainString()
}

private fun List<TransactionEntity>.filterBySearch(query: String): List<TransactionEntity> {
    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    if (normalizedQuery.isBlank()) {
        return this
    }

    return filter { transaction ->
        listOf(
            transaction.description,
            transaction.category,
            transaction.account,
            transaction.typeLabel(),
            transaction.date,
            formatPlainAmount(transaction.amountCents)
        ).any { value -> value.lowercase(Locale.ROOT).contains(normalizedQuery) }
    }
}

private fun TransactionEntity.typeLabel(): String {
    return if (type == TYPE_INCOME) "entrata income" else "uscita expense"
}

private fun List<TransactionEntity>.filterBySalaryPeriod(
    selectedMonth: String?,
    selectedYear: String?
): List<TransactionEntity> {
    if (selectedMonth == null && selectedYear == null) {
        return this
    }

    return filter { transaction ->
        val key = transaction.salaryMonthKey() ?: return@filter false
        (selectedMonth == null || key.month == selectedMonth) &&
            (selectedYear == null || key.year == selectedYear)
    }
}

private data class SalaryMonthKey(val year: String, val month: String)

private fun TransactionEntity.salaryMonthKey(): SalaryMonthKey? {
    val calendar = parseIsoDateToCalendar(date) ?: return null
    if (calendar.get(Calendar.DAY_OF_MONTH) < SALARY_MONTH_START_DAY) {
        calendar.add(Calendar.MONTH, -1)
    }
    return SalaryMonthKey(
        year = "%04d".format(calendar.get(Calendar.YEAR)),
        month = "%02d".format(calendar.get(Calendar.MONTH) + 1)
    )
}

private fun parseIsoDateToCalendar(value: String): Calendar? {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        isLenient = false
    }
    return try {
        Calendar.getInstance(Locale.ROOT).apply {
            time = formatter.parse(value) ?: return null
        }
    } catch (_: ParseException) {
        null
    }
}

private fun salaryPeriodLabel(selectedMonth: String?, selectedYear: String?): String {
    return when {
        selectedMonth != null && selectedYear != null -> {
            "Periodo ${monthName(selectedMonth)} $selectedYear"
        }
        selectedMonth != null -> {
            "Periodo ${monthName(selectedMonth)}"
        }
        selectedYear != null -> {
            "Anno $selectedYear"
        }
        else -> "Tutti i movimenti"
    }
}

private fun chartPeriodTitle(selectedMonth: String?, selectedYear: String?): String {
    return if (selectedMonth != null) {
        "Entrate e uscite ${salaryPeriodLabel(selectedMonth, selectedYear)}"
    } else {
        selectedYear?.let { "Entrate e uscite $it" } ?: "Entrate e uscite"
    }
}

private fun monthName(month: String): String {
    val index = month.toIntOrNull()?.minus(1)
    return index?.takeIf { it in monthLabels.indices }?.let { monthLabels[it] } ?: month
}

private fun List<TransactionEntity>.toCategoryTotals(): List<CategoryTotal> {
    return filter { it.type == TYPE_EXPENSE }
        .groupBy { it.category.ifBlank { "Senza categoria" } }
        .mapValues { (_, transactions) -> transactions.sumOf { it.amountCents } }
        .toList()
        .sortedByDescending { (_, amount) -> amount }
        .mapIndexed { index, (category, amount) ->
            CategoryTotal(
                label = category,
                value = amount,
                color = chartPalette[index % chartPalette.size]
            )
        }
}

private fun List<TransactionEntity>.toMonthlyTotals(
    selectedYear: String?,
    selectedMonth: String?
): List<MonthlyTotal> {
    selectedMonth?.let { month ->
        return listOf(toMonthlyTotal(salaryPeriodLabel(month, selectedYear)))
    }

    if (selectedYear != null) {
        return (1..12).map { month ->
            val monthKey = "%02d".format(month)
            filterBySalaryPeriod(monthKey, selectedYear).toMonthlyTotal(monthLabels[month - 1])
        }
    }

    return mapNotNull { transaction ->
        transaction.salaryMonthKey()
    }
        .distinct()
        .sortedWith(compareBy<SalaryMonthKey> { it.year }.thenBy { it.month })
        .takeLast(12)
        .map { key ->
            filterBySalaryPeriod(key.month, key.year).toMonthlyTotal(salaryPeriodLabel(key.month, key.year))
        }
}

private fun List<TransactionEntity>.toMonthlyTotal(label: String): MonthlyTotal {
    return MonthlyTotal(
        label = label,
        income = filter { it.type == TYPE_INCOME }.sumOf { it.amountCents },
        expenses = filter { it.type == TYPE_EXPENSE }.sumOf { it.amountCents }
    )
}

@Preview(showBackground = true)
@Composable
private fun ExpenseHomePreview() {
    GestoreSpeseAndroidTheme {
        ExpenseHomeScreen(
            transactions = listOf(
                TransactionEntity(
                    id = 1,
                    date = "2026-05-08",
                    description = "Spesa supermercato",
                    amountCents = 2530,
                    category = "Cibo",
                    type = TYPE_EXPENSE,
                    account = "Contanti"
                ),
                TransactionEntity(
                    id = 2,
                    date = "2026-05-08",
                    description = "Stipendio",
                    amountCents = 120000,
                    category = "Stipendio",
                    type = TYPE_INCOME,
                    account = "Conto Bancario"
                )
            ),
            dashboardTransactions = listOf(
                TransactionEntity(
                    id = 1,
                    date = "2026-05-08",
                    description = "Spesa supermercato",
                    amountCents = 2530,
                    category = "Cibo",
                    type = TYPE_EXPENSE,
                    account = "Contanti"
                )
            ),
            dashboardTitle = "Periodo Mag 2026",
            categories = defaultCategories,
            accounts = defaultAccounts,
            years = listOf("2026"),
            selectedMonth = null,
            selectedYear = "2026",
            selectedAccount = null,
            searchQuery = "",
            onMonthSelected = {},
            onYearSelected = {},
            onAccountSelected = {},
            onSearchQueryChange = {},
            onEdit = {},
            onDelete = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
