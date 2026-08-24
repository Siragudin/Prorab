@file:Suppress("DEPRECATION")

package com.example.prorab


import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.prorab.data.ProfileData
import com.example.prorab.data.Project
import com.example.prorab.presentation.MainViewModel
import com.example.prorab.presentation.ProfileDialog
import com.example.prorab.presentation.ProfileViewModel
import com.example.prorab.presentation.auth.AuthViewModel
import com.example.prorab.presentation.auth.GoogleAuthClient
import com.example.prorab.presentation.auth.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private val googleAuthUiClient by lazy {
        GoogleAuthClient(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mainViewModel: MainViewModel by viewModels()

        setContent {
            MaterialTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = {
                        showSplash = false
                    })
                } else {
                    val projectsList by mainViewModel.projects.collectAsState()

                    val authViewModel = viewModel<AuthViewModel>()
                    val authState by authViewModel.state.collectAsStateWithLifecycle()
                    var userDataState by remember { mutableStateOf(googleAuthUiClient.getSignedInUser()) }

                    val profileViewModel = viewModel<ProfileViewModel>()
                    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()

                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.StartIntentSenderForResult(),
                        onResult = { result ->
                            if (result.resultCode == RESULT_OK) {
                                lifecycleScope.launch {
                                    val signInResult = googleAuthUiClient.signInWithIntent(intent = result.data ?: return@launch)
                                    authViewModel.onSignInResult(signInResult)
                                    if (signInResult.data != null) userDataState = signInResult.data
                                }
                            }
                        }
                    )

                    LaunchedEffect(key1 = authState.isSignInSuccessful) {
                        if (authState.isSignInSuccessful) {
                            Toast.makeText(applicationContext, "Вход выполнен!", Toast.LENGTH_LONG).show()
                            authViewModel.resetState()
                        }
                    }

                    ProrabMainScreen(
                        projects = projectsList,
                        userData = userDataState,
                        profileData = profileState,
                        onAddProject = { name -> mainViewModel.addProject(name) },
                        onDeleteProject = { project -> mainViewModel.deleteProject(project) },
                        onSignInClick = {
                            lifecycleScope.launch {
                                val signInIntentSender = googleAuthUiClient.signIn()
                                launcher.launch(IntentSenderRequest.Builder(signInIntentSender ?: return@launch).build())
                            }
                        },
                        onSignOutClick = {
                            lifecycleScope.launch {
                                googleAuthUiClient.signOut()
                                userDataState = null
                                authViewModel.resetState()
                                Toast.makeText(applicationContext, "Вышли из аккаунта", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSaveProfile = { name, phone, logo, stamp ->
                            profileViewModel.saveProfile(name, phone, logo, stamp)
                        }
                    )
                }
            }
        }



    }
}

@Composable
fun ProrabMainScreen(
    projects: List<Project>,
    userData: UserData?,
    profileData: ProfileData,
    onAddProject: (String) -> Unit,
    onDeleteProject: (Project) -> Unit,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSaveProfile: (String, String, String?, String?) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showProfileSettingsDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // --- ДИАЛОГИ ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить объект?") },
            text = { Text("Вы собираетесь удалить объект «${projectToDelete?.name}».\n\nВсе записи по этому объекту тоже будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    projectToDelete?.let { onDeleteProject(it) }
                    showDeleteDialog = false
                }) { Text("Удалить", color = Color.Red) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Выход из аккаунта") },
            text = { Text("Вы хотите выйти из аккаунта ${userData?.username}?") },
            confirmButton = {
                TextButton(onClick = {
                    onSignOutClick()
                    showSignOutDialog = false
                }) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text("Отмена") } }
        )
    }
    if (showProfileSettingsDialog) {
        ProfileDialog(
            data = profileData,
            onDismiss = { showProfileSettingsDialog = false },
            onSave = { name, phone, logo, stamp ->
                onSaveProfile(name, phone, logo, stamp)
                showProfileSettingsDialog = false
            }
        )
    }

    if (showAddDialog) {
        AddProjectDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onAddProject(name)
                showAddDialog = false
            }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("О приложении") },
            text = {
                Column {
                    Text("Прораб: Учет стройки")
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Версия: 2.0", fontSize = 12.sp, color = Color.Gray)
                    Text("Учет работ и материалов с облачной синхронизацией.", fontSize = 14.sp)
                    Text("Разработка: S`IT", fontSize = 14.sp)
                }
            },
            confirmButton = { TextButton(onClick = { showInfoDialog = false }) { Text("Закрыть") } }
        )
    }

    // --- ГЛАВНАЯ СТРУКТУРА ---
    Column(modifier = Modifier.fillMaxSize()) {

        HeaderSection(
            modifier = Modifier.weight(3f).fillMaxWidth(),
            userData = userData,
            onSignInClick = onSignInClick,
            onLogoutClick = { showSignOutDialog = true }
        )

        ProjectListSection(
            projects = projects,
            onProjectClick = { project ->
                val intent = Intent(context, ProjectDetailActivity::class.java).apply {
                    putExtra("PROJECT_ID", project.id)
                    putExtra("PROJECT_NAME", project.name)
                }
                context.startActivity(intent)
            },
            onDeleteClick = { project ->
                projectToDelete = project
                showDeleteDialog = true
            },
            modifier = Modifier.weight(5f).fillMaxWidth()
        )

        BottomControlSection(
            onAddClick = { showAddDialog = true },
            onSettingsClick = { showProfileSettingsDialog = true },
            onInfoClick = { showInfoDialog = true },
            modifier = Modifier.weight(2f).fillMaxWidth()
        )
    }
}

@Composable
fun HeaderSection(modifier: Modifier = Modifier, userData: UserData?, onSignInClick: () -> Unit, onLogoutClick: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var timeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { timeMillis = System.currentTimeMillis(); delay(1000) } }
    val date = Date(timeMillis)
    val dateText = SimpleDateFormat("dd MMMM", Locale("ru")).format(date)
    val dayText = SimpleDateFormat("EEEE", Locale("ru")).format(date)
    val timeText = SimpleDateFormat("HH:mm", Locale("ru")).format(date)

    Box(modifier = modifier
        .background(Color(0xFF263238))
        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp, top = 48.dp)
    ) {
        Box(modifier = Modifier.align(Alignment.TopEnd)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { if (userData == null) onSignInClick() else showMenu = true }.padding(4.dp)) {
                if (userData != null) Text(text = userData.username?.split(" ")?.firstOrNull() ?: "Я", color = Color.White, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Профиль", tint = if (userData != null) Color.Green else Color.White, modifier = Modifier.size(36.dp))
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) { DropdownMenuItem(text = { Text("Выйти из аккаунта", color = Color.Red) }, onClick = { showMenu = false; onLogoutClick() }) }
        }
        Row(modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text(text = dateText, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold); Text(text = dayText.replaceFirstChar { it.uppercase() }, color = Color(0xFFB0BEC5), fontSize = 18.sp) }
            Text(text = timeText, color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Light)
        }
    }
}

@Composable
fun BottomControlSection(
    onAddClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(Color(0xFFF5F5F5)).padding(24.dp)) {
        FloatingActionButton(
            onClick = onSettingsClick,
            containerColor = Color.White,
            contentColor = Color(0xFF263238),
            modifier = Modifier.align(Alignment.BottomStart)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Настройки")
        }

        IconButton(
            onClick = onInfoClick,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Icon(Icons.Default.Info, contentDescription = "Инфо", tint = Color.Gray)
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = Color(0xFF263238),
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить")
        }
    }
}

@Composable
fun ProjectListSection(projects: List<Project>, onProjectClick: (Project) -> Unit, onDeleteClick: (Project) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFFF5F5F5)).padding(16.dp), contentAlignment = Alignment.Center) {
        if (projects.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Нет активных объектов", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray); Spacer(modifier = Modifier.height(8.dp)); Text("Нажмите + внизу справа", fontSize = 14.sp, color = Color.Gray) }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp, start = 16.dp, end = 16.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        name = project.name,
                        dateCreated = project.dateCreated,
                        onClick = { onProjectClick(project) },
                        onDelete = { onDeleteClick(project) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProjectCard(
    name: String,
    dateCreated: Long,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(dateCreated) {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(dateCreated))
    }

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.padding(20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Text(text = "Создан: $dateStr", fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = Color.LightGray) }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Новый объект") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Название") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = text.isNotBlank(),
                onClick = { if (text.isNotBlank()) onConfirm(text) }
            ) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
