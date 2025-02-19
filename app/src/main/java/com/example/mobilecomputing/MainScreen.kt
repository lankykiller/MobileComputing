package com.example.mobilecomputing

import android.content.Context
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.mobilecomputing.ui.theme.MobileComputingTheme
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.awaitCancellation
import java.io.File

data class Message(val author: String, val body: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(messages: SampleData, navController: NavHostController) {

    val context = LocalContext.current
    val db = remember { getDatabase(context) }
    val userDao = remember { db.userDao() }
    val messageDao = remember { db.messageDao() }

    var userName by remember { mutableStateOf<String?>(null) }
    var profilePicture by remember { mutableStateOf<String?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var newMessageText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()


    var brightnessSensor by remember { mutableStateOf(0f) }
    var brightnessText by remember { mutableStateOf("Unknown") }

    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val lightSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    var currentBackground by remember { mutableStateOf(R.drawable.background_normal) }

    fun updateBackgroundResource(brightness: Float): Int {
        return when (brightness.toInt()) {
            0 -> R.drawable.background_pitch_black
            in 1..10 -> R.drawable.background_dark
            in 11..50 -> R.drawable.background_grey
            in 51..5000 -> R.drawable.background_normal
            in 5001..25000 -> R.drawable.background_bright
            else -> R.drawable.background_very_bright
        }
    }

    DisposableEffect(sensorManager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                    brightnessSensor = event.values[0]
                    brightnessText = when (brightnessSensor.toInt()) {
                        0 -> "Pitch black"
                        in 1..10 -> "Dark"
                        in 11..50 -> "Grey"
                        in 51..5000 -> "Basic"
                        in 5001..25000 -> "Bright"
                        else -> "Too bright"
                    }
                    currentBackground = updateBackgroundResource(brightnessSensor)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Load user data
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) {

            val messageCount = messageDao.getCount()
            if (messageCount == 0) {
                val sampleMessages = SampleData.conversationSample.map {
                    MessageEntity(author = it.author, body = it.body)
                }
                messageDao.insertAll(sampleMessages)
            }

            val dbMessages = messageDao.getAllMessages()
            messages = dbMessages.map { Message(it.author, it.body) }

            val users = userDao.getAll()
            if (users.isEmpty()) {
                val firstUser = SampleData.conversationSample.first().author
                val firstProfileUri = null
                userDao.insertAll(User(id = 0, name = firstUser, firstProfileUri))
                userName = firstUser
            }
            else {
                userName = users.firstOrNull()?.name
                profilePicture = users.firstOrNull()?.profilePictureUri
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        Image(
            painter = painterResource(currentBackground),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(text = "Conversation")
                        Row {
                            Text(
                                text = "Brightness: ${String.format("%.1f", brightnessSensor)} lux $brightnessText",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                },
                actions = {
                    Button(onClick = { navController.navigate("settingsScreen") }) {
                        Text(text = "Settings")
                    }
                }
            )
            Box(modifier = Modifier.weight(1f)) {
                Conversation(messages = messages, userName = userName, profilePicture = profilePicture)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = newMessageText,
                    onValueChange = { newMessageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message") }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        if (newMessageText.isNotBlank() && userName != null) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val newMessage = MessageEntity(
                                    author = userName!!,
                                    body = newMessageText
                                )
                                messageDao.insertMessage(newMessage)

                                val dbMessages = messageDao.getAllMessages()
                                messages = dbMessages.map { Message(it.author, it.body) }

                                newMessageText = ""
                            }
                        }
                    }
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
fun MessageCard(msg: Message, userName: String?, profilePicture: String?) {

    Row(modifier = Modifier.padding(all = 8.dp)) {
        val imageUri = profilePicture?.let { Uri.parse(it) }
        val picPainter = if (imageUri != null) {
            rememberAsyncImagePainter(
                model = File(imageUri.path),
                placeholder = null,
                error = null
            )
        } else {
            painterResource(R.drawable.profile_picture)
        }

        Image(
            painter = picPainter,
            contentDescription = "Contact profile picture",
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))
        var isExpanded by remember { mutableStateOf(false) }
        val surfaceColor by animateColorAsState(
            if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        )

        Column(modifier = Modifier.clickable { isExpanded = !isExpanded }) {
            Text(
                text = userName ?: msg.author,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 1.dp,
                color = surfaceColor,
                modifier = Modifier.animateContentSize().padding(1.dp)
            ) {
                Text(
                    text = msg.body,
                    modifier = Modifier.padding(all = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1
                )
            }
        }
    }
}

fun getDatabase(context: Context): AppDatabase {
    return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "UserDB"
    ).build()
}

@Composable
fun Conversation(messages: List<Message>, userName: String?, profilePicture: String?) {
    LazyColumn {
        items(messages) { message ->
            MessageCard(message, userName, profilePicture)
        }
    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewMessageCard() {

    MobileComputingTheme {
        Surface {
            MessageCard(
                msg = Message("LeBron", "Jetpack Compose is great!"),
                userName = null,
                profilePicture = null
            )
        }
    }
}

@Preview
@Composable
fun PreviewConversation() {
    MobileComputingTheme {
        Conversation(SampleData.conversationSample, userName = null, profilePicture = null)
    }
}

