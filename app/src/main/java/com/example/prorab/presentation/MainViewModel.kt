package com.example.prorab.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prorab.data.AppRepository
import com.example.prorab.data.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Теперь используем репозиторий вместо DAO
    private val repository = AppRepository(application)

    // Список проектов теперь приходит от репозитория
    // Используем stateIn, чтобы конвертировать Flow в StateFlow для UI
    val projects: StateFlow<List<Project>> = repository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProject(name: String) {
        viewModelScope.launch {
            repository.addProject(name)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }
}