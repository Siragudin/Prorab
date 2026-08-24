package com.example.prorab.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_COMPANY_NAME = stringPreferencesKey("company_name")
        val KEY_PHONE = stringPreferencesKey("company_phone") // НОВОЕ: Телефон
        val KEY_LOGO_URI = stringPreferencesKey("logo_uri")
        val KEY_STAMP_URI = stringPreferencesKey("stamp_uri") // НОВОЕ: Печать
    }

    val profileData: Flow<ProfileData> = context.dataStore.data
        .map { preferences ->
            ProfileData(
                companyName = preferences[KEY_COMPANY_NAME] ?: "",
                phone = preferences[KEY_PHONE] ?: "",
                logoUri = preferences[KEY_LOGO_URI],
                stampUri = preferences[KEY_STAMP_URI]
            )
        }

    suspend fun saveProfile(name: String, phone: String, logoUri: String?, stampUri: String?) {
        context.dataStore.edit { preferences ->
            preferences[KEY_COMPANY_NAME] = name
            preferences[KEY_PHONE] = phone
            if (logoUri != null) preferences[KEY_LOGO_URI] = logoUri
            if (stampUri != null) preferences[KEY_STAMP_URI] = stampUri
        }
    }
}

data class ProfileData(
    val companyName: String,
    val phone: String,
    val logoUri: String?,
    val stampUri: String?
)