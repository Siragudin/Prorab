package com.example.prorab.presentation

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.prorab.data.ProfileData
import com.example.prorab.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application)
    private val context = application.applicationContext

    val profileState = userPreferences.profileData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileData("", "", null, null)
        )

    fun saveProfile(name: String, phone: String, logoUriStr: String?, stampUriStr: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1. Обрабатываем Логотип
            val finalLogoPath = processImage(logoUriStr, "company_logo.jpg")

            // 2. Обрабатываем Печать
            val finalStampPath = processImage(stampUriStr, "company_stamp.jpg")

            // 3. Сохраняем пути к внутренним файлам в настройки
            userPreferences.saveProfile(name, phone, finalLogoPath, finalStampPath)
        }
    }

    // Умная функция: копирует картинку внутрь приложения
    private fun processImage(uriStr: String?, fileName: String): String? {
        // Если пользователь удалил картинку (пришел null)
        if (uriStr == null) {
            val file = File(context.filesDir, fileName)
            if (file.exists()) file.delete() // Удаляем старый файл
            return null
        }

        // Если это уже наш внутренний файл (мы его уже сохраняли раньше), ничего не делаем
        if (!uriStr.startsWith("content://")) {
            return uriStr
        }

        // Если это новая картинка из Галереи (content://...), копируем её себе
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uriStr))
            val file = File(context.filesDir, fileName)
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            // Возвращаем путь к нашему локальному файлу (file:///...)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null // Если ошибка копирования - не сохраняем
        }
    }
}