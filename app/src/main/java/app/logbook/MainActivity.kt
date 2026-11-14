package app.logbook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.app.TimePickerDialog
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
    var morningEnabled by remember { mutableStateOf(prefs.getBoolean("morning_enabled", true)) }
    var eveningEnabled by remember { mutableStateOf(prefs.getBoolean("evening_enabled", true)) }
    var morningHour by remember { mutableIntStateOf(prefs.getInt("morning_hour", 8)) }
    var morningMinute by remember { mutableIntStateOf(prefs.getInt("morning_minute", 0)) }
    var eveningHour by remember { mutableIntStateOf(prefs.getInt("evening_hour", 20)) }
    var eveningMinute by remember { mutableIntStateOf(prefs.getInt("evening_minute", 0)) }
    val dark = when (themeMode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    var screen by remember { mutableStateOf(if (prefs.getBoolean("onboarded", name.isNotBlank())) LogbookScreen.Home else LogbookScreen.Welcome) }
    var memories by remember { mutableStateOf(emptyList<Memory>()) }
    var todos by remember { mutableStateOf(emptyList<Todo>()) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var selectedHub by remember { mutableStateOf<EntityHub?>(null) }
    var showSplash by remember { mutableStateOf(true) }
    var customTagDraft by remember { mutableStateOf("") }
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
        scheduleFriendlyNudge(context, "morning", morningHour, morningMinute, morningEnabled)
        scheduleFriendlyNudge(context, "evening", eveningHour, eveningMinute, eveningEnabled)
    }

    fun pickNudgeTime(kind: String) {
        val hour = if (kind == "morning") morningHour else eveningHour
        val minute = if (kind == "morning") morningMinute else eveningMinute
        TimePickerDialog(context, { _, selectedHour, selectedMinute ->
            if (kind == "morning") { morningHour = selectedHour; morningMinute = selectedMinute }
            else { eveningHour = selectedHour; eveningMinute = selectedMinute }
            prefs.edit().putInt("${kind}_hour", selectedHour).putInt("${kind}_minute", selectedMinute).apply()
            scheduleFriendlyNudge(context, kind, selectedHour, selectedMinute, true)
        }, hour, minute, false).show()
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
            }.onFailure { toast("Export failed. Your logbook is unchanged.") }
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
            }.onFailure { toast("Import failed. Check that this is a logbook JSON file.") }
        }
    }

    fun startCapture(existing: Memory? = null) {
        editingMemory = existing
        composerText = existing?.text.orEmpty()
        previewTags = existing?.tags?.ifEmpty { listOf("ideas") } ?: listOf("ideas")
        customTagDraft = ""
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
                customTagDraft = ""
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
            if (taskText.isBlank()) return "Sure—what should I put on your list? For example, ‘Remind me to call Sam tomorrow.’"
            saveTask(taskText)
            val due = SmartNlp.due(taskText)?.let { " for ${formatDue(it)}" }.orEmpty()
            return "Got it. I added ‘$taskText’ to Things in motion$due."
        }
        val asksForTasks = listOf("task", "to-do", "todo", "reminder", "next step").any { lower.contains(it) } &&
            listOf("show", "list", "what", "have", "due", "left", "pending", "remaining", "my").any { lower.contains(it) }
        if (asksForTasks) {
            val open = todos.filterNot { it.completed }
            return if (open.isEmpty()) "You’re all clear—there aren’t any open tasks right now."
            else "Here’s what’s still in motion:\n" + open.take(8).joinToString("\n") { "• ${it.text}${it.due?.let { due -> " · ${formatDue(due)}" }.orEmpty()}" }
        }
        if (listOf("tag", "label", "categor", "organize").any { lower.contains(it) }) {
            val tagNames = LogbookCategories.values.joinToString(", ")
            return "I suggest labels from words and phrases in your note—like links, dates, names, action wording, and topic terms. You can keep several, remove any, or add your own. Current shelves include $tagNames. All matching happens on this device."
        }
        val asksToSearch = Regex("(?i)\\b(find|search|look for|look up|show me|what did i (?:write|save|note) about|what have i written about|do you remember what i wrote about)\\b").containsMatchIn(q)
        if (asksToSearch) {
            val term = q.replace(Regex("(?i)^.*?\\b(?:find|search(?: for)?|look for|look up|show me|what did i (?:write|save|note) about|what have i written about|do you remember what i wrote about)\\s*"), "")
                .replace(Regex("(?i)^(?:my )?(?:notes?|memories|thoughts?)\\s*(?:about|on|for)?\\s*"), "")
                .trim().trimEnd('?', '.', '!')
            if (term.isBlank()) return "What should I look for? A name, phrase, link, or topic is enough."
            val matches = SmartNlp.search(memories, term, "all")
            return if (matches.isEmpty()) "I didn’t find a note matching ‘$term’. Try a shorter phrase, or tap a tag in Search to browse that shelf."
            else "I found ${matches.size} ${if (matches.size == 1) "thought" else "thoughts"} that may fit ‘$term’:\n" + matches.take(3).joinToString("\n\n") { "“${it.text.take(180)}${if (it.text.length > 180) "…" else ""}” · ${it.tags.joinToString(" · ") { tag -> LogbookCategories[tag] ?: tag }}" }
        }
        if (lower.contains("today") || lower.contains("captured") || lower.contains("summary")) {
            val todayCount = memories.count { isToday(it.created) }
            val openTasks = todos.count { !it.completed }
            return "A quick check-in: you’ve saved $todayCount ${if (todayCount == 1) "thought" else "thoughts"} today, and $openTasks ${if (openTasks == 1) "task is" else "tasks are"} still open."
        }
        if (Regex("(?i)\\b(hi|hello|hey|good morning|good evening)\\b").containsMatchIn(lower)) {
            return "Hey${name.takeIf { it.isNotBlank() }?.let { ", $it"}.orEmpty()}—what’s on your mind? You can ask me to find a thought, check what’s still open, or help you remember something."
        }
        if (Regex("(?i)\\b(thanks|thank you|cheers)\\b").containsMatchIn(lower)) return "Anytime. I’m here whenever you want to catch or find a thought."
        if (listOf("help", "what can you do", "how do i use").any { lower.contains(it) }) return "Talk naturally. Ask ‘what did I save about the trip?’, ‘what’s still on my list?’, or ‘remind me to email Maya Friday’. I can also explain how tags were suggested. Your notes never leave this device."
        return "I’m following along. I use local matching rather than a cloud chatbot, but you can phrase things naturally—ask about a note, a person, a topic, or what you need to do next."
    }

    BackHandler(enabled = screen != LogbookScreen.Home && screen != LogbookScreen.Welcome) {
        if (screen == LogbookScreen.Hubs && selectedHub != null) selectedHub = null
        else screen = when (screen) {
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
                        micStatus = micStatus,
                        memories = memories,
                        excludeId = editingMemory?.id,
                        onRelated = { tag -> query = ""; category = tag; screen = LogbookScreen.Search }
                    )
                    LogbookScreen.Preview -> PreviewScreen(
                        text = composerText,
                        onText = { value ->
                            val clipped = value.take(2000)
                            val oldSuggestions = SmartNlp.analyze(composerText).first
                            val newSuggestions = SmartNlp.analyze(clipped).first
                            previewTags = (previewTags.filterNot { it in oldSuggestions } + newSuggestions).distinct()
                            composerText = clipped
                        },
                        tags = previewTags,
                        onToggleTag = { tag -> previewTags = if (tag in previewTags) { if (previewTags.size > 1) previewTags - tag else previewTags } else previewTags + tag },
                        customTagDraft = customTagDraft,
                        onCustomTagDraft = { customTagDraft = it.take(40) },
                        onAddCustomTag = {
                            val tag = customTagDraft.trim().lowercase(Locale.ROOT)
                                .replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(32)
                            if (tag.isNotBlank() && tag !in previewTags) previewTags = previewTags + tag
                            customTagDraft = ""
                        },
                        taskSuggested = SmartNlp.taskCandidate(composerText),
                        addTask = addTaskWithMemory,
                        onAddTask = { addTaskWithMemory = it },
                        onBack = { screen = LogbookScreen.Compose },
                        onSave = { saveThought(addTaskWithMemory) },
                        similar = SmartNlp.similarMemories(composerText, memories, editingMemory?.id, 2),
                        onRelated = { tag -> query = ""; category = tag; screen = LogbookScreen.Search }
                    )
                    else -> Box(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize()) {
                            BrandBar { screen = LogbookScreen.Guide }
                            AnimatedContent(targetState = screen, modifier = Modifier.weight(1f), transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "logbook-screen") { route ->
                                when (route) {
                                    LogbookScreen.Home -> HomeScreen(name, memories, todos, { selectedMemory = it }, { memoryToDelete = it },
                                        { query = ""; category = "all"; screen = LogbookScreen.Search }, { screen = LogbookScreen.Tasks },
                                        { task -> val updated = task.copy(completed = true); scope.launch {
                                            withContext(Dispatchers.IO) { store.saveTodo(updated); cancelReminder(context, updated.id) }; refresh()
                                        } }, { tag -> query = ""; category = tag; screen = LogbookScreen.Search })
                                    LogbookScreen.Search -> SearchScreen(memories, query, { query = it }, category, { category = it }, { selectedMemory = it }, { memoryToDelete = it })
                                    LogbookScreen.Hubs -> HubsScreen(memories, selectedHub, { selectedHub = it }, { selectedMemory = it })
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
                                        morningEnabled = morningEnabled, eveningEnabled = eveningEnabled,
                                        morningTime = String.format(Locale.getDefault(), "%02d:%02d", morningHour, morningMinute),
                                        eveningTime = String.format(Locale.getDefault(), "%02d:%02d", eveningHour, eveningMinute),
                                        onMorningEnabled = { value -> morningEnabled = value; prefs.edit().putBoolean("morning_enabled", value).apply(); scheduleFriendlyNudge(context, "morning", morningHour, morningMinute, value) },
                                        onEveningEnabled = { value -> eveningEnabled = value; prefs.edit().putBoolean("evening_enabled", value).apply(); scheduleFriendlyNudge(context, "evening", eveningHour, eveningMinute, value) },
                                        onMorningTime = { pickNudgeTime("morning") }, onEveningTime = { pickNudgeTime("evening") },
                                        onExport = { exportLauncher.launch("logbook_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())}.json") },
                                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*")) }, onClear = { confirmClear = true }, onIntro = { screen = LogbookScreen.Intro })
                                    else -> Unit
                                }
                            }
                            BottomNavigation(screen) { screen = it }
                        }
                        if (screen == LogbookScreen.Home || screen == LogbookScreen.Search) Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 82.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.End) {
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
            if (showSplash) BootSplash { showSplash = false }
        }

        selectedMemory?.let { memory ->
            StickyNoteSheet(memory, onDismiss = { selectedMemory = null }, onEdit = { selectedMemory = null; startCapture(memory) }, onDelete = { memoryToDelete = memory; selectedMemory = null }, onDone = { selectedMemory = null })
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
            title = { Text("Clear your logbook?") },
            text = { Text("All saved thoughts and tasks on this device will be deleted. Export a JSON copy first if you may want them later.") },
            confirmButton = { TextButton(onClick = {
                confirmClear = false
                scope.launch {
                    withContext(Dispatchers.IO) { cancelAllReminders(context, todos); store.clear() }
                    memories = emptyList()
                    todos = emptyList()
                    toast("Your local logbook is clear")
                }
            }) { Text("Delete everything", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}
