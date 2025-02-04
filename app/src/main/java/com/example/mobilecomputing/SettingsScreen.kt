package com.example.mobilecomputing

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@SuppressLint("CoroutineCreationDuringComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val db = remember { getDatabase(context) }
    val userDao = remember { db.userDao() }

    var userName by remember { mutableStateOf(TextFieldValue(" ")) }
    var isEditing by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val imagePath = ImageSave.saveImageToInternalStorage(context, it)
            imagePath?.let { path ->
                selectedImageUri = Uri.fromFile(File(path))
                coroutineScope.launch(Dispatchers.IO) {
                    val users = userDao.getAll()
                    val user = users.firstOrNull()
                    if (user != null) {
                        userDao.delete(user)
                        userDao.insertAll(User(id = 0, name = user.name, profilePictureUri = path))
                    }
                }
            }
        }
    }
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {
            val users = userDao.getAll()
            val user = users.firstOrNull()
            userName = TextFieldValue(user?.name ?: "No user found")
            selectedImageUri = user?.profilePictureUri?.let { Uri.parse(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back button"
                        )
                    }
                },
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profile Picture",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(16.dp)
                ) {

                    if (selectedImageUri != null) {
                        Image(
                            painter = rememberAsyncImagePainter(selectedImageUri),
                            contentDescription = "Selected profile picture",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.profile_picture),
                            contentDescription = "Default profile picture",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }
                }

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text("Change Picture")
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isEditing) {
                    TextField(
                        value = userName,
                        onValueChange = { userName = it },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val users = userDao.getAll()
                            val user = users.firstOrNull()
                            val currentUri = user?.profilePictureUri

                            if (user != null) {
                                userDao.delete(user)
                            }
                            userDao.insertAll(User(
                                id = 0,
                                name = userName.text,
                                profilePictureUri = currentUri
                            ))
                        }
                        isEditing = false
                    }) {
                        Text("Save")
                    }
                } else {
                    Text(
                        text = userName.text,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clickable { isEditing = true }
                            .padding(8.dp)
                    )
                }
            }
        }
    )
}

object ImageSave {
    fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
        return try {
            // Create unique filename
            val filename = "profile_${UUID.randomUUID()}.jpg"
            val file = File(context.filesDir, filename)

            // Copy the image
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}