package com.example.prorab.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.prorab.data.ProfileData

@Composable
fun ProfileDialog(
    data: ProfileData,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String?) -> Unit
) {
    // Инициализируем поля текущими данными
    var companyName by remember { mutableStateOf(data.companyName) }
    var phone by remember { mutableStateOf(data.phone) }

    // Если в базе пусто - будет null, если есть - будет Uri
    var logoUri by remember { mutableStateOf<Uri?>(data.logoUri?.let { Uri.parse(it) }) }
    var stampUri by remember { mutableStateOf<Uri?>(data.stampUri?.let { Uri.parse(it) }) }

    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) logoUri = uri
    }

    val stampLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) stampUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки фирмы") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ПОЛЯ ВВОДА (Можно стереть текст, чтобы удалить)
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Название организации") },
                    placeholder = { Text("Например: ООО СтройМастер") },
                    leadingIcon = { Icon(Icons.Default.Business, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Телефон") },
                    placeholder = { Text("+7 (999) 000-00-00") },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Divider()
                Text("Оформление:", fontSize = 14.sp, color = Color.Gray)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Блок ЛОГОТИП (с возможностью удаления)
                    ImagePickerItem(
                        uri = logoUri,
                        label = "Логотип (слева)",
                        onClick = { logoLauncher.launch("image/*") },
                        onRemove = { logoUri = null } // Удаляем лого
                    )

                    // Блок ПЕЧАТЬ (с возможностью удаления)
                    ImagePickerItem(
                        uri = stampUri,
                        label = "Печать (справа)",
                        onClick = { stampLauncher.launch("image/*") },
                        onRemove = { stampUri = null } // Удаляем печать
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Если строка пустая - сохраняем как пустую строку.
                // Если URI null - сохраняем null.
                onSave(
                    companyName.trim(),
                    phone.trim(),
                    logoUri?.toString(),
                    stampUri?.toString()
                )
            }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun ImagePickerItem(
    uri: Uri?,
    label: String,
    onClick: () -> Unit,
    onRemove: () -> Unit // Функция удаления
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(90.dp) // Чуть увеличил размер
        ) {
            // Сама рамка с картинкой
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(6.dp) // Отступ, чтобы крестик влез
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .clickable { onClick() },
                contentAlignment = Alignment.Center
            ) {
                if (uri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit, // Fit - чтобы лого не обрезалось
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color.Gray)
                }
            }

            // КНОПКА УДАЛЕНИЯ (Крестик) - показываем только если есть картинка
            if (uri != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd) // Правый верхний угол
                        .size(24.dp) // Маленький размер
                        .background(Color.White, CircleShape) // Белый фон под крестиком
                        .border(1.dp, Color.LightGray, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить",
                        tint = Color.Red,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}