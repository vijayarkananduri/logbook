package app.logbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val store = remember { LocalStore(applicationContext) }
            LogbookApp(store)
        }
    }
}

@Composable
private fun LogbookApp(store: LocalStore) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("settings", 0) }
    var name by remember { mutableStateOf(prefs.getString("name", "").orEmpty()) }
    var themeMode by remember(prefs) {
        mutableStateOf(prefs.getString("theme_mode", null)?.takeIf { it in setOf("system", "light", "dark") }
            ?: if (prefs.getBoolean("dark", false)) "dark" else "system")
    }
    val dark = when (themeMode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    var screen by remember { mutableStateOf(if (prefs.getBoolean("onboarded", name.isNotBlank())) LogbookScreen.Home else LogbookScreen.Welcome) }
    var memories by remember { mutableStateOf(emptyList<Memory>()) }
    var todos by remember { mutableStateOf(emptyList<Todo>()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var composerText by remember { mutableStateOf("") }
    var previewTags by remember { mutableStateOf(listOf("ideas")) }
    var addTaskWithMemory by remember { mutableStateOf(false) }
    var editingMemory by remember { mutableStateOf<Memory?>(null) }
    var selectedMemory by remember { mutableStateOf<Memory?>(null) }
    var memoryToDelete by remember { mutableStateOf<Memory?>(null) }
    var taskToDelete by remember { mutableStateOf<Todo?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var taskDraft by remember { mutableStateOf("") }
    var assistantDraft by remember { mutableStateOf("") }
    val chat = remember { mutableStateListOf(ChatLine(false, "I work right here on your device. I can find a thought, show your tasks, or add a reminder when you ask.")) }
    var banner by remember { mutableStateOf("") }
    var micStatus by remember { mutableStateOf("Voice is optional; typing always works.") }
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    val scope = rememberCoroutineScope()

    fun toast(message: String) {
        scope.launch {
            banner = message
            delay(2600)
            if (banner == message) banner = ""
        }
    }

    fun refresh() {
        scope.launch {
            val result = withContext(Dispatchers.IO) { store.memories() to store.todos() }
            memories = result.first
            todos = result.second
        }
    }

    LaunchedEffect(store) {
        val result = withContext(Dispatchers.IO) { store.memories() to store.todos() }
        memories = result.first
        todos = result.second
    }

    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            scope.launch(Dispatchers.IO) { LocalStore(context).todos().filter { !it.completed }.forEach { scheduleReminder(context, it) } }
            toast("Notifications are on for your local reminders")
        } else toast("Task saved. Allow notifications in Android settings to receive reminders.")
    }
    fun askForNotificationsIfNeeded(hasDueTime: Boolean) {
        if (hasDueTime && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun saveTask(text: String, memoryId: String? = null) {
        val task = Todo(SmartNlp.id("td"), text.trim(), SmartNlp.due(text), memoryId = memoryId)
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    store.saveTodo(task)
                    scheduleReminder(context, task)
                }
                refresh()
                askForNotificationsIfNeeded(task.due != null)
                toast(if (task.due != null) "Task saved with a local reminder" else "Task saved")
            }.onFailure { toast("Could not save that task") }
        }
    }

    fun beginVoiceCapture() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            micStatus = "On-device voice needs Android 12 or later. Type your thought here instead."
            return
        }
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            micStatus = "No on-device speech model is installed. Your writing stays available below."
            return
        }
        runCatching {
            recognizer?.destroy()
            val speech = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            recognizer = speech
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { micStatus = "Listening on this device…" }
                override fun onBeginningOfSpeech() { micStatus = "Listening… speak naturally." }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { micStatus = "Finishing your words…" }
                override fun onError(error: Int) {
                    micStatus = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn’t catch that. Try again or type instead."
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice is busy. Wait a moment, then try again."
                        else -> "Voice isn’t available right now. Your text is still here."
                    }
                }
                override fun onResults(results: Bundle?) {
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (heard.isNotBlank()) composerText = (composerText + " " + heard).trim().take(2000)
                    micStatus = if (heard.isBlank()) "No words detected. You can type instead." else "Transcript added. Give it a quick read."
                }
                override fun onPartialResults(results: Bundle?) {
                    val heard = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (heard.isNotBlank()) micStatus = "Hearing: $heard"
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
            speech.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            )
        }.onFailure { micStatus = "Voice could not start on this device. Type your thought instead." }
    }

    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginVoiceCapture() else micStatus = "Microphone permission is off. You can still type your thought."
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(store.export(name)) }
                        ?: error("Could not open the selected destination")
                }
                toast("Private JSON export saved")
            }.onFailure { toast("Export failed. Your Logbook is unchanged.") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val importedName = withContext(Dispatchers.IO) {
                    val contents = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                        ?: error("Could not read the selected file")
                    store.import(contents, name)
                }
                name = importedName
                prefs.edit().putString("name", importedName).apply()
                refresh()
                toast("Import complete. Existing matching IDs were kept safe.")
            }.onFailure { toast("Import failed. Check that this is a Logbook JSON file.") }
        }
    }

    fun startCapture(existing: Memory? = null) {
        editingMemory = existing
        composerText = existing?.text.orEmpty()
        previewTags = existing?.tags?.ifEmpty { listOf("ideas") } ?: listOf("ideas")
        addTaskWithMemory = false
        micStatus = "Voice is optional; typing always works."
        screen = LogbookScreen.Compose
    }

    fun saveThought(asTask: Boolean) {
        val body = composerText.trim()
        if (body.isBlank()) return
        val analysis = SmartNlp.analyze(body)
        val existing = editingMemory
        val memory = Memory(
            id = existing?.id ?: SmartNlp.id("lb"), text = body,
            tags = previewTags.ifEmpty { analysis.first }, attributes = analysis.second,
            created = existing?.created ?: System.currentTimeMillis()
        )
        val task = if (asTask) Todo(SmartNlp.id("td"), SmartNlp.taskText(body), SmartNlp.due(body), memoryId = memory.id) else null
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    if (task == null) store.save(memory) else store.saveAndCreateTodo(memory, task)
                    if (task != null) scheduleReminder(context, task)
                }
                refresh()
                if (task != null) askForNotificationsIfNeeded(task.due != null)
                editingMemory = null
                composerText = ""
                screen = LogbookScreen.Home
                toast(if (task == null) "Thought tucked safely away" else "Thought saved and task added")
            }.onFailure { toast("Could not save this thought") }
        }
    }

    fun localAnswer(question: String): String {
        val q = question.trim()
        val lower = q.lowercase(Locale.ROOT)
        if (SmartNlp.isTaskCommand(q)) {
            val taskText = SmartNlp.taskText(q)
            if (taskText.isBlank()) return "Tell me what you want to remember, for example: ‘Remind me to call Sam tomorrow.’"
            saveTask(taskText)
            val due = SmartNlp.due(taskText)?.let { " for ${formatDue(it)}" }.orEmpty()
            return "Added ‘$taskText’ to your tasks$due."
        }
        if (listOf("my tasks", "show tasks", "show my tasks", "to-do", "todo list").any { lower.contains(it) }) {
            val open = todos.filterNot { it.completed }
            return if (open.isEmpty()) "You’re all clear. No open tasks right now."
            else open.take(8).joinToString("\n") { "• ${it.text}${it.due?.let { due -> " — ${formatDue(due)}" }.orEmpty()}" }
        }
        if (listOf("find", "search", "look for", "when did i", "what did i write", "show me").any { lower.contains(it) }) {
            val term = q.replace(Regex("(?i)^(find|search(?: for)?|look for|show me|when did i|what did i write about)\\s*"), "")
                .replace(Regex("(?i)^(a |the )?(note|memory|thought)s?\\s*(about )?"), "").trim()
            val matches = SmartNlp.search(memories, term, "all")
            return if (matches.isEmpty()) "I couldn’t find a matching thought. Try a different word or open Search to browse by category."
            else matches.take(3).joinToString("\n\n") { "“${it.text.take(180)}${if (it.text.length > 180) "…" else ""}”" }
        }
        if (lower.contains("today") || lower.contains("captured") || lower.contains("summary")) {
            val todayCount = memories.count { isToday(it.created) }
            val openTasks = todos.count { !it.completed }
            return "Today you’ve caught $todayCount ${if (todayCount == 1) "thought" else "thoughts"}. You have $openTasks open ${if (openTasks == 1) "task" else "tasks"}. Everything stays on this device."
        }
        if (lower in listOf("hi", "hello", "help", "what can you do?")) return "I can search saved thoughts, show open tasks, or add a task when you say ‘Remind me to…’. I don’t use a cloud AI."
        return "I’m a small local helper, not a cloud AI. Try ‘Find my notes about ideas’, ‘Show my tasks’, or ‘Remind me to call Alex tomorrow.’"
    }

    BackHandler(enabled = screen != LogbookScreen.Home && screen != LogbookScreen.Welcome) {
        screen = when (screen) {
            LogbookScreen.Preview -> LogbookScreen.Compose
            LogbookScreen.Compose -> LogbookScreen.Home
            LogbookScreen.Intro -> LogbookScreen.Welcome
            else -> LogbookScreen.Home
        }
    }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }

    LogbookTheme(dark) {
        val systemBarColor = MaterialTheme.colorScheme.background.toArgb()
        SideEffect {
            (context as? ComponentActivity)?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, false)
                window.statusBarColor = systemBarColor
                window.navigationBarColor = systemBarColor
                val bars = WindowCompat.getInsetsController(window, window.decorView)
                bars.isAppearanceLightStatusBars = !dark
                bars.isAppearanceLightNavigationBars = !dark
            }
        }
        LogbookBackdrop {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                when (screen) {
                    LogbookScreen.Welcome -> WelcomeScreen(name, { name = it.take(40) }) {
                        name = name.trim()
                        prefs.edit().putString("name", name).apply()
                        screen = LogbookScreen.Intro
                    }
                    LogbookScreen.Intro -> IntroScreen {
                        prefs.edit().putBoolean("onboarded", true).apply()
                        screen = LogbookScreen.Home
                    }
                    LogbookScreen.Compose -> ComposerScreen(
                        text = composerText,
                        onText = { composerText = it.take(2000) },
                        onBack = { screen = LogbookScreen.Home },
                        onAnalyze = {
                            val detected = SmartNlp.analyze(composerText).first
                            previewTags = editingMemory?.tags?.ifEmpty { detected } ?: detected
                            addTaskWithMemory = SmartNlp.taskCandidate(composerText)
                            screen = LogbookScreen.Preview
                        },
                        onMic = {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) micStatus = "On-device voice needs Android 12 or later. Type your thought instead."
                            else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onStop = { recognizer?.stopListening(); micStatus = "Transcript ready to review." },
                        micStatus = micStatus
                    )
                    LogbookScreen.Preview -> PreviewScreen(
                        text = composerText,
                        onText = { composerText = it.take(2000) },
                        tags = previewTags,
                        onToggleTag = { tag -> previewTags = if (tag in previewTags) { if (previewTags.size > 1) previewTags - tag else previewTags } else previewTags + tag },
                        taskSuggested = SmartNlp.taskCandidate(composerText),
                        addTask = addTaskWithMemory,
                        onAddTask = { addTaskWithMemory = it },
                        onBack = { screen = LogbookScreen.Compose },
                        onSave = { saveThought(addTaskWithMemory) }
                    )
                    else -> Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            BrandBar(name) { screen = LogbookScreen.Settings }
                            AnimatedContent(targetState = screen, modifier = Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "logbook-screen") { route ->
                                when (route) {
                                    LogbookScreen.Home -> HomeScreen(name, memories, todos, { selectedMemory = it }, { memoryToDelete = it }, { screen = LogbookScreen.Search })
                                    LogbookScreen.Search -> SearchScreen(memories, query, { query = it }, category, { category = it }, { selectedMemory = it }, { memoryToDelete = it })
                                    LogbookScreen.Tasks -> TasksScreen(todos, taskDraft, { taskDraft = it },
                                        { if (taskDraft.isNotBlank()) { saveTask(taskDraft); taskDraft = "" } },
                                        { task -> val updated = task.copy(completed = !task.completed); scope.launch {
                                            withContext(Dispatchers.IO) { store.saveTodo(updated); if (updated.completed) cancelReminder(context, updated.id) else scheduleReminder(context, updated) }; refresh()
                                        } }, { task -> taskToDelete = task })
                                    LogbookScreen.Guide -> GuideScreen(chat, assistantDraft, { assistantDraft = it }) {
                                        val question = assistantDraft.trim()
                                        if (question.isNotBlank()) { chat.add(ChatLine(true, question)); chat.add(ChatLine(false, localAnswer(question))); assistantDraft = "" }
                                    }
                                    LogbookScreen.Settings -> SettingsScreen(name = name,
                                        onName = { value -> name = value.take(40); prefs.edit().putString("name", name).apply() }, themeMode = themeMode,
                                        onThemeMode = { value -> themeMode = value; prefs.edit().putString("theme_mode", value).apply() },
                                        onExport = { exportLauncher.launch("logbook_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json") },
                                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) }, onClear = { confirmClear = true }, onIntro = { screen = LogbookScreen.Intro })
                                    else -> Unit
                                }
                            }
                            BottomNavigation(screen) { screen = it }
                        }
                        if (screen != LogbookScreen.Settings) Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 82.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
                            if (screen != LogbookScreen.Search) SmallFloatingActionButton(onClick = { screen = LogbookScreen.Search }, containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.primary) { Icon(Icons.Rounded.Search, "Search") }
                            FloatingActionButton(onClick = { startCapture() }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Icon(Icons.Rounded.Add, "Add a thought") }
                        }
                    }
                }
            }
            if (banner.isNotBlank()) Text(
                banner,
                Modifier.align(Alignment.BottomCenter).padding(horizontal = 24.dp, vertical = 18.dp)
                    .clip(MaterialTheme.shapes.medium).background(if (dark) Color(0xFF40372E) else Color(0xFF2C241D))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        selectedMemory?.let { memory ->
            AlertDialog(
                onDismissRequest = { selectedMemory = null },
                title = { Text("A thought you kept", fontFamily = LogbookHand, fontSize = 27.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(memory.text, style = MaterialTheme.typography.bodyLarge)
                        Text(memory.tags.joinToString("  · ") { LogbookCategories[it] ?: it }, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        if (memory.attributes.isNotEmpty()) Text(memory.attributes.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(formatDate(memory.created), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                },
                confirmButton = {
                    Row {
                        TextButton(onClick = { selectedMemory = null; startCapture(memory) }) { Text("Edit") }
                        TextButton(onClick = { selectedMemory = null }) { Text("Done") }
                    }
                },
                dismissButton = { TextButton(onClick = { memoryToDelete = memory; selectedMemory = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } }
            )
        }
        memoryToDelete?.let { memory -> AlertDialog(
            onDismissRequest = { memoryToDelete = null }, title = { Text("Delete this thought?") },
            text = { Text("The thought will be removed. Any linked task will stay, but will be unlinked from it.") },
            confirmButton = { TextButton(onClick = { val id = memory.id; memoryToDelete = null; scope.launch {
                runCatching { withContext(Dispatchers.IO) { store.deleteMemory(id) }; refresh(); toast("Thought deleted") }.onFailure { toast("Could not delete that thought") }
            } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { memoryToDelete = null }) { Text("Keep it") } }
        ) }
        taskToDelete?.let { task -> AlertDialog(
            onDismissRequest = { taskToDelete = null }, title = { Text("Delete this task?") }, text = { Text("This also cancels its local reminder.") },
            confirmButton = { TextButton(onClick = { taskToDelete = null; scope.launch {
                runCatching { withContext(Dispatchers.IO) { cancelReminder(context, task.id); store.deleteTodo(task.id) }; refresh(); toast("Task removed") }.onFailure { toast("Could not remove that task") }
            } }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { taskToDelete = null }) { Text("Keep it") } }
        ) }
        if (confirmClear) AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear your Logbook?") },
            text = { Text("All saved thoughts and tasks on this device will be deleted. Export a JSON copy first if you may want them later.") },
            confirmButton = { TextButton(onClick = {
                confirmClear = false
                scope.launch {
                    withContext(Dispatchers.IO) { cancelAllReminders(context, todos); store.clear() }
                    memories = emptyList()
                    todos = emptyList()
                    toast("Your local Logbook is clear")
                }
            }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
