package com.example.prorab.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.prorab.data.AppRepository
import com.example.prorab.data.Record
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class ProjectDetailViewModel(application: Application, private val projectId: Int) : AndroidViewModel(application) {

    private val repository = AppRepository(application)

    // Принудительно очищаем стартовую системную дату от часов, минут и секунд
    private val _selectedDate = MutableStateFlow(normalizeDate(System.currentTimeMillis()))
    val selectedDate = _selectedDate.asStateFlow()

    private val allRecords = repository.getRecords(projectId)

    // Сравнение дней теперь работает без сбоев, так как обе даты нормализованы
    val recordsForSelectedDate = combine(allRecords, _selectedDate) { records, date ->
        records.filter { normalizeDate(it.date) == date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjectRecordsState = allRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ИСПРАВЛЕНО: Превращаем Set в List и принудительно сортируем даты по убыванию (свежие слева)
    // ИСПРАВЛЕНО: Принудительно очищаем даты от часов/минут и сортируем их как чистые числа
    // СОРТИРОВКА СЛЕВА НАПРАВО: Самые ранние даты будут в начале, свежие — в конце списка
    // ЖЕЛЕЗНЫЙ РАЗВОР ОТ СТАРЫХ К НОВЫМ (Слева направо)
    // СОРТИРОВКА СЛЕВА НАПРАВО (1, 2, 3 число...)
    // Просто собираем уникальные дни. Порядок автоматически берется из базы данных
    val datesWithData = allRecords.map { records ->
        records.map { normalizeDate(it.date) }.distinct()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // ИСПРАВЛЕНО: При переключении даты на главном экране тоже сбрасываем время в 00:00
    fun updateDate(newDate: Long) {
        _selectedDate.value = normalizeDate(newDate)
    }

    fun addRecord(type: Int, title: String, quantity: Double, unit: String, unitPrice: Double, amount: Double, customDate: Long) {
        viewModelScope.launch {
            // Устанавливаем дату, но жестко прописываем время: 00:01:00 (1 минута суток)
            val finalDateTime = Calendar.getInstance().apply {
                timeInMillis = customDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1) // <--- Добавили 1 минуту
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val record = Record(
                projectId = projectId,
                type = type,
                title = title,
                quantity = quantity,
                unit = unit,
                unitPrice = unitPrice,
                amount = amount,
                date = finalDateTime
            )
            repository.addRecord(record)
        }
    }

    fun updateRecord(record: Record, type: Int, title: String, quantity: Double, unit: String, unitPrice: Double, amount: Double, customDate: Long) {
        viewModelScope.launch {
            val finalDateTime = Calendar.getInstance().apply {
                timeInMillis = customDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 1) // <--- Добавили 1 минуту
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val updatedRecord = record.copy(
                type = type,
                title = title,
                quantity = quantity,
                unit = unit,
                unitPrice = unitPrice,
                amount = amount,
                date = finalDateTime
            )
            repository.updateRecord(updatedRecord)
        }
    }


    fun deleteRecord(record: Record) {
        viewModelScope.launch { repository.deleteRecord(record) }
    }

    // Убираем часы/минуты, оставляем только год-месяц-день для точного сравнения и сортировки
    private fun normalizeDate(date: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    class Factory(private val app: Application, private val projectId: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProjectDetailViewModel(app, projectId) as T
        }
    }
}
