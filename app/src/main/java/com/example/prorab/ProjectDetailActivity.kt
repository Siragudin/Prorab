package com.example.prorab

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.prorab.data.Project
import com.example.prorab.data.Record
import com.example.prorab.presentation.ProfileViewModel
import com.example.prorab.presentation.ProjectDetailViewModel
import com.example.prorab.utils.PdfGenerator
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProjectDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val projectId = intent.getIntExtra("PROJECT_ID", -1)
        val projectName = intent.getStringExtra("PROJECT_NAME") ?: "Объект"

        val viewModel: ProjectDetailViewModel by viewModels {
            ProjectDetailViewModel.Factory(application, projectId)
        }
        val profileViewModel: ProfileViewModel by viewModels()

        setContent {
            MaterialTheme {
                val profileData by profileViewModel.profileState.collectAsStateWithLifecycle()
                DetailScreen(projectId, projectName, viewModel, profileData) { finish() }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    projectId: Int,
    projectName: String,
    viewModel: ProjectDetailViewModel,
    profileData: com.example.prorab.data.ProfileData,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val recordsForDay by viewModel.recordsForSelectedDate.collectAsState()
    val allRecords by viewModel.allProjectRecordsState.collectAsState()
    val datesWithData by viewModel.datesWithData.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var recordToEdit by remember { mutableStateOf<Record?>(null) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showPdfDateDialog by remember { mutableStateOf(false) }
    val dateRangePickerState = rememberDateRangePickerState()
    var recordToDelete by remember { mutableStateOf<Record?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Переменная для окна подсказки
    var showInfoHintDialog by remember { mutableStateOf(false) }

    // ОКНО С ПОДСКАЗКОЙ ИНФО (Новое)
    if (showInfoHintDialog) {
        AlertDialog(
            onDismissRequest = { showInfoHintDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF263238), modifier = Modifier.padding(end = 8.dp))
                    Text("Как пользоваться датами?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "1️⃣ *Как сделать отчет:* Чтобы сформировать PDF-отчет, нажмите на иконку документа и выберите период. Если нужен отчет строго за один день, нажмите на этот день в календаре *дважды*.",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "2️⃣ *Записи задним числом:* Если нужно добавить работу или расход за другой день, сначала выберите этот день в верхней горизонтальной ленте (или нажмите на дату вверху, чтобы открыть календарь), а затем нажмите кнопку *«+»*.",
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoHintDialog = false }) {
                    Text("Понятно", fontWeight = FontWeight.Bold, color = Color(0xFF263238))
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удаление") },
            text = { Text("Удалить запись?") },
            confirmButton = { TextButton(onClick = { recordToDelete?.let { viewModel.deleteRecord(it) }; showDeleteConfirmDialog = false; recordToDelete = null }) { Text("Удалить", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Отмена") } }
        )
    }

    if (showRecordDialog) {
        RecordDialog(
            initialType = recordToEdit?.type ?: selectedTab,
            initialTitle = recordToEdit?.title ?: "",
            initialQuantity = recordToEdit?.quantity ?: 0.0,
            initialUnit = recordToEdit?.unit ?: "шт",
            initialPrice = recordToEdit?.unitPrice ?: 0.0,
            initialAmount = recordToEdit?.amount ?: 0.0,
            initialDate = recordToEdit?.date ?: selectedDate,
            isEditing = recordToEdit != null,
            onDismiss = { showRecordDialog = false; recordToEdit = null },
            onConfirm = { type, title, qty, unit, price, amount, customDate ->
                if (recordToEdit == null) {
                    viewModel.addRecord(type, title, qty, unit, price, amount, customDate)
                } else {
                    viewModel.updateRecord(recordToEdit!!, type, title, qty, unit, price, amount, customDate)
                }
                viewModel.updateDate(customDate)
                showRecordDialog = false
                recordToEdit = null
            }
        )
    }

    if (showPdfDateDialog) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showPdfDateDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showPdfDateDialog = false
                    val start = dateRangePickerState.selectedStartDateMillis
                    val end = dateRangePickerState.selectedEndDateMillis

                    if (start != null) {
                        val calStart = Calendar.getInstance().apply {
                            timeInMillis = start
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis

                        val calEnd = Calendar.getInstance().apply {
                            timeInMillis = end ?: start
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis

                        val filtered = allRecords.filter { it.date in calStart..calEnd }

                        if (filtered.isNotEmpty()) {
                            val f = PdfGenerator.generateReport(context, Project(projectId, projectName), filtered, profileData, calStart, calEnd)
                            if (f != null) sharePdfFile(context, f) else Toast.makeText(context, "Ошибка", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Нет записей за этот период", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Выберите период", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Сформировать") }
            },
            dismissButton = { TextButton(onClick = { showPdfDateDialog = false }) { Text("Отмена") } }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Выберите период", Modifier.padding(16.dp)) },
                headline = {},
                showModeToggle = false,
                modifier = Modifier.height(500.dp)
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(projectName, color = Color.White, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Назад", tint = Color.White) } },
                actions = {
                    // КНОПКА ПОДСКАЗКИ С ИКОНКОЙ i ВЕРНУЛАСЬ СЮДА
                    IconButton(onClick = { showInfoHintDialog = true }) { Icon(Icons.Default.Info, "Подсказка", tint = Color.White) }
                    IconButton(onClick = { showPdfDateDialog = true }) { Icon(Icons.Default.Description, "PDF", tint = Color.White) }
                    IconButton(onClick = { shareReportText(context, projectName, selectedDate, recordsForDay) }) { Icon(Icons.Default.Share, "Текст", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF263238))
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { recordToEdit = null; showRecordDialog = true }, containerColor = Color(0xFF263238)) { Icon(Icons.Default.Add, "Добавить", tint = Color.White) }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Surface(
                color = Color(0xFF263238),
                onClick = {
                    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    DatePickerDialog(context, { _, year, month, dayOfMonth ->
                        val target = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                        viewModel.updateDate(target.timeInMillis)
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    val currentDayFormatter = SimpleDateFormat("d MMMM, EEEE", Locale("ru"))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, bottom = 4.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = currentDayFormatter.format(Date(selectedDate)), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.CalendarToday, "Календарь", tint = Color.White)
                    }

                    if (datesWithData.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(datesWithData) { dateMillis ->
                                val isSelected = isSameDay(dateMillis, selectedDate)
                                val chipFormatter = SimpleDateFormat("E, dd MMM", Locale("ru"))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White else Color(0xFF37474F))
                                        .clickable { viewModel.updateDate(dateMillis) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chipFormatter.format(Date(dateMillis)),
                                        color = if (isSelected) Color(0xFF263238) else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Нет записей. Нажмите +, чтобы добавить первую работу.", color = Color.LightGray, fontSize = 13.sp, fontStyle = FontStyle.Italic)
                        }
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = Color.White, contentColor = Color(0xFF263238)) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("РАБОТЫ") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("РАСХОДЫ") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("ИНФО") })
            }

            val filteredRecords = recordsForDay.filter { it.type == selectedTab }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredRecords) { record ->
                    RecordItemCard(
                        record = record,
                        onEdit = { recordToEdit = record; showRecordDialog = true },
                        onDelete = { recordToDelete = record; showDeleteConfirmDialog = true }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDialog(
    initialType: Int,
    initialTitle: String,
    initialQuantity: Double,
    initialUnit: String,
    initialPrice: Double,
    initialAmount: Double,
    initialDate: Long,
    isEditing: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, Double, String, Double, Double, Long) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf(initialTitle) }
    var quantityStr by remember { mutableStateOf(if (initialQuantity == 0.0) "" else initialQuantity.toString()) }
    var unit by remember { mutableStateOf(initialUnit) }
    var priceStr by remember { mutableStateOf(if (initialPrice == 0.0) "" else initialPrice.toString()) }
    var selectedCustomDate by remember { mutableStateOf(initialDate) }

    val context = LocalContext.current
    val quantity = quantityStr.toDoubleOrNull() ?: 0.0
    val price = priceStr.toDoubleOrNull() ?: 0.0
    val computedAmount = if (type == 2) 0.0 else (quantity * price)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Редактировать" else "Добавить") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { type = 0 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (type == 0) Color(0xFF263238) else Color.LightGray)) { Text("Работа", fontSize = 11.sp) }
                    Button(onClick = { type = 1 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (type == 1) Color(0xFF263238) else Color.LightGray)) { Text("Расход", fontSize = 11.sp) }
                    Button(onClick = { type = 2 }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = if (type == 2) Color(0xFF263238) else Color.LightGray)) { Text("Инфо", fontSize = 11.sp) }
                }

                if (type == 2) {
                    // ПРОСТАЯ ФОРМА ДЛЯ ЗАМЕТОК (ДЛЯ ВКЛАДКИ ИНФО)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Текст заметки / Описание работ") },
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // СТАНДАРТНАЯ СМЕТНАЯ ФОРМА (ДЛЯ РАБОТ И РАСХОДОВ)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Название") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = quantityStr,
                            onValueChange = { quantityStr = it },
                            label = { Text("Кол-во") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier.weight(1f)
                        )

                        UnitDropDownField(
                            selectedUnit = unit,
                            onUnitSelected = { unit = it },
                            modifier = Modifier.weight(1.2f)
                        )

                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Цена/ед") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(value = if (computedAmount > 0) "$computedAmount ₽" else "0 ₽", onValueChange = {}, label = { Text("Итого сумма") }, readOnly = true, modifier = Modifier.fillMaxWidth())
                }

                val dateFormatter = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
                OutlinedTextField(
                    value = dateFormatter.format(Date(selectedCustomDate)),
                    onValueChange = {},
                    label = { Text("Дата записи") },
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, "Календарь") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        val calendar = Calendar.getInstance().apply { timeInMillis = selectedCustomDate }
                        DatePickerDialog(context, { _, year, month, dayOfMonth ->
                            val targetCalendar = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                            selectedCustomDate = targetCalendar.timeInMillis
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = { if (title.isNotBlank()) onConfirm(type, title, quantity, unit, price, computedAmount, selectedCustomDate) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitDropDownField(
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val units = listOf("шт", "пм", "м²", "м³", "л", "рейс", "кг", "т", "см")

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedUnit,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ед. изм.") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { unitItem ->
                DropdownMenuItem(text = { Text(text = unitItem) }, onClick = { onUnitSelected(unitItem); expanded = false })
            }
        }
    }
}

@Composable
fun RecordItemCard(record: Record, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(record.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (record.type != 2) {
                    Text("${record.quantity} x ${record.unitPrice} ₽ (${record.unit})", color = Color.Gray, fontSize = 12.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (record.type != 2) {
                    Text("${record.amount} ₽", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Редактировать", tint = Color.Gray) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Удалить", tint = Color.LightGray) }
            }
        }
    }
}

fun isSameDay(date1: Long, date2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun sharePdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться сметой"))
}

fun shareReportText(context: Context, projectName: String, date: Long, records: List<Record>) {
    val formatter = SimpleDateFormat("dd.MM.yyyy", Locale("ru"))
    val currentDayFormatter = SimpleDateFormat("d MMMM, EEEE", Locale("ru"))

    // Красивая шапка сообщения
    var text = "🧱 *Объект:* ${projectName.uppercase()}\n"
    text += "📅 *Дата:* ${currentDayFormatter.format(Date(date))}\n"
    text += "────────────────────\n\n"

    // 1. БЛОК РАБОТ (type == 0)
    val works = records.filter { it.type == 0 }
    if (works.isNotEmpty()) {
        text += "🛠 *РАБОТЫ:*\n"
        works.forEach {
            text += "• ${it.title} — ${it.quantity} ${it.unit} × ${it.unitPrice} ₽ = *${it.amount} ₽*\n"
        }
        text += "\n"
    }

    // 2. БЛОК РАСХОДОВ (type == 1)
    val expenses = records.filter { it.type == 1 }
    if (expenses.isNotEmpty()) {
        text += "📉 *РАСХОДЫ:*\n"
        expenses.forEach {
            text += "• ${it.title} — ${it.quantity} ${it.unit} × ${it.unitPrice} ₽ = *${it.amount} ₽*\n"
        }
        text += "\n"
    }

    // ИТОГОВАЯ СУММА ЗА ДЕНЬ (Смета: Работы + Расходы)
    val totalDayAmount = works.sumOf { it.amount } + expenses.sumOf { it.amount }
    if (totalDayAmount > 0) {
        text += "💰 *ИТОГО ЗА ДЕНЬ:* *${totalDayAmount} ₽*\n"
        text += "────────────────────\n\n"
    }

    // 3. БЛОК ЗАМЕТОК / ИНФО (type == 2)
    val infoRecords = records.filter { it.type == 2 }
    if (infoRecords.isNotEmpty()) {
        text += "📝 *ЗАМЕТКИ / ОПИСАНИЕ:*\n"
        infoRecords.forEach {
            text += "• ${it.title}\n"
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Поделиться отчетом"))
}

