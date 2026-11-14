package app.logbook

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun WelcomeScreen(name: String, onName: (String) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_logbook), "Logbook mark", Modifier.size(70.dp))
        Spacer(Modifier.height(22.dp))
        Text("A little room\nfor your thoughts.", fontFamily = LogbookHand, fontSize = 42.sp, lineHeight = 46.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(14.dp))
        Text("No account. No cloud. Just a private place to catch a thought, find it later, and get it out of your head.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        EntryField(name, onName, "What should I call you?", false, 48.dp)
        Spacer(Modifier.height(18.dp))
        PrimaryAction("Make it mine", MaterialTheme.colorScheme.primary, onContinue)
        Spacer(Modifier.height(12.dp))
        Text("Your writing stays on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun IntroScreen(onStart: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val cards = listOf(
        Triple(Icons.Rounded.EditNote, "Catch it", "Write the thought first. Sort it after."),
        Triple(Icons.Rounded.Search, "Find it", "Search words, categories, links and tasks from one place."),
        Triple(Icons.Rounded.Lock, "Keep it yours", "Everything stays encrypted and offline on this device.")
    )
    val card = cards[page]
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(cards.size) { index -> Box(Modifier.height(5.dp).weight(1f).clip(CircleShape).background(if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) }
        }
        Spacer(Modifier.height(28.dp))
        androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(R.drawable.ic_logbook), "Logbook mark", Modifier.size(58.dp))
        Spacer(Modifier.height(18.dp))
        Text(card.second, fontFamily = LogbookHand, fontSize = 42.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(card.third, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        DewSurface(Modifier.fillMaxWidth(), tone = MaterialTheme.colorScheme.primaryContainer) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(card.first, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(14.dp))
                Text("No account. No cloud. Just a private mind space.", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(30.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (page > 0) OutlinedButton(onClick = { page-- }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Back") }
            PrimaryAction(if (page == cards.lastIndex) "Open Logbook" else "Next", MaterialTheme.colorScheme.primary, { if (page == cards.lastIndex) onStart() else page++ }, true)
        }
        Spacer(Modifier.height(12.dp))
        Text("Swipe through the essentials in three quick steps.", Modifier.align(Alignment.CenterHorizontally), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IntroCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, detail: String) {
    DewSurface(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(17.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(13.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun HomeScreen(name: String, memories: List<Memory>, todos: List<Todo>, onCapture: () -> Unit, onMemory: (Memory) -> Unit, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(greeting(name), fontFamily = LogbookHand, fontSize = 34.sp, lineHeight = 40.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        DewSurface(Modifier.fillMaxWidth(), tone = MaterialTheme.colorScheme.primaryContainer, onClick = onCapture) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Make room for a thought", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(5.dp))
                    Text("Capture first. Decide what it means after.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(13.dp))
                    Text("Write a thought  →", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
                Box(Modifier.size(54.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.EditNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("TODAY", memories.count { isToday(it.created) }.toString(), "thoughts caught", Icons.Rounded.AutoAwesome, Modifier.weight(1f))
            StatTile("IN MOTION", todos.count { !it.completed }.toString(), "open tasks", Icons.Rounded.CheckCircle, Modifier.weight(1f))
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent thoughts", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            TextButton(onClick = onSearch, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("See all") }
        }
        if (memories.isEmpty()) {
            DewSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp)) {
                    Text("The page is yours.", fontFamily = LogbookHand, fontSize = 27.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text("A line, a link, a feeling, a half-formed idea. Start wherever you are.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else memories.take(4).forEachIndexed { index, memory ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            MemoryCard(memory) { onMemory(memory) }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, caption: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    DewSurface(modifier, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontFamily = LogbookBody, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Text(caption, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun SearchScreen(
    memories: List<Memory>, todos: List<Todo>, query: String, onQuery: (String) -> Unit,
    category: String, onCategory: (String) -> Unit, onMemory: (Memory) -> Unit,
    onDelete: (Memory) -> Unit, onArchive: (Memory) -> Unit, onDeleteTask: (Todo) -> Unit,
    onArchiveOlder: () -> Unit, archivedIds: Set<String>
) {
    var showArchived by remember { mutableStateOf(false) }
    val visibleMemories = memories.filter { (it.id in archivedIds) == showArchived }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Find anything", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            TextButton(onClick = { onArchiveOlder(); showArchived = false }) { Text("Archive 30d") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            EntryField(query, onQuery, "Search thoughts, tasks, links…", false, 50.dp, Modifier.weight(1f))
            Spacer(Modifier.width(6.dp))
            FilterChip(showArchived, { showArchived = !showArchived }, { Text(if (showArchived) "Archived" else "Live") })
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CategoryChip("All", category == "all", MaterialTheme.colorScheme.primary) { onCategory("all") }
            LogbookCategories.forEach { (key, label) -> CategoryChip(label, category == key, MaterialTheme.colorScheme.primary) { onCategory(key) } }
        }
        Spacer(Modifier.height(10.dp))
        val results = SmartNlp.search(visibleMemories, query, category)
        val taskResults = todos.filter { query.isBlank() || it.text.contains(query, true) }
            .sortedWith(compareBy<Todo> { it.completed }.thenBy { it.due ?: Long.MAX_VALUE })
        if (query.isBlank() && category == "all" && !showArchived) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyPanel("Your thoughts and tasks will be easy to find.", "Search across everything from one place, or pick a category to browse.") }
        } else if (results.isEmpty() && taskResults.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { EmptyPanel("Nothing matches just yet.", "Try a shorter phrase or another category.") }
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(results, key = { it.id }) { memory -> SwipeMemoryCard(memory, onMemory, onDelete, onArchive) }
            items(taskResults, key = { "task-" + it.id }) { task -> TodoCard(task, {}, { onDeleteTask(task) }) }
        }
    }
}

@Composable
private fun SwipeMemoryCard(memory: Memory, onMemory: (Memory) -> Unit, onDelete: (Memory) -> Unit, onArchive: (Memory) -> Unit) {
    var drag by remember { mutableFloatStateOf(0f) }
    MemoryCard(memory, Modifier.pointerInput(memory.id) {
        detectHorizontalDragGestures(onHorizontalDrag = { _, amount -> drag += amount }, onDragEnd = {
            when { drag < -120f -> onDelete(memory); drag > 120f -> onArchive(memory) }
            drag = 0f
        })
    }) { onMemory(memory) }
}

@Composable
internal fun TasksScreen(todos: List<Todo>, draft: String, onDraft: (String) -> Unit, onAdd: () -> Unit, onToggle: (Todo) -> Unit, onDelete: (Todo) -> Unit) {
    val ordered = todos.sortedWith(compareBy<Todo> { it.completed }.thenBy { it.due ?: Long.MAX_VALUE }.thenByDescending { it.created })
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Things in motion", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        Text("A gentle place for what comes next.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(13.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryField(draft, onDraft, "Add a task — call Sam tomorrow", false, 50.dp, Modifier.weight(1f))
            IconButton(onClick = onAdd, modifier = Modifier.size(50.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Rounded.Add, "Add task", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Spacer(Modifier.height(13.dp))
        if (ordered.isEmpty()) Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyPanel("Nothing is asking for you yet.", "Add a task here, or find a saved thought in universal Search.")
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(ordered, key = { it.id }) { task -> TodoCard(task, { onToggle(task) }, { onDelete(task) }) }
        }
    }
}

@Composable
internal fun SettingsScreen(
    name: String, onName: (String) -> Unit, dark: Boolean, onDark: (Boolean) -> Unit,
    reducedEffects: Boolean, onReducedEffects: (Boolean) -> Unit, accentIndex: Int, onAccent: (Int) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onClear: () -> Unit, onIntro: () -> Unit, onHelp: () -> Unit,
    startReminder: String, onStartReminder: (String) -> Unit, endReminder: String, onEndReminder: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Make it feel like yours", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(14.dp))
        SettingsGroup("Appearance") {
            SettingToggle(Icons.Rounded.DarkMode, "Dark, warm ink", "Keep the same Logbook colors after dark.", dark, onDark)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingToggle(Icons.Rounded.WaterDrop, "Liquid-glass finish", "Soft sheen, a fine rim and lift on touch.", !reducedEffects, { onReducedEffects(!it) })
            Text("Low-resource mode turns off gloss and shadows for a quieter, lighter surface.", Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Text("Choose your accent", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(5.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccentNames.forEachIndexed { index, label ->
                    val swatch = accentColor(index, dark)
                    Box(Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics { contentDescription = "$label accent"; selected = index == accentIndex }
                        .clickable(role = Role.RadioButton) { onAccent(index) }, contentAlignment = Alignment.Center) {
                        Box(Modifier.size(31.dp).clip(CircleShape).background(swatch))
                        if (index == accentIndex) Box(Modifier.size(36.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape))
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("Your profile") {
            Text("Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(7.dp))
            EntryField(name, onName, "A name for the greeting", false, 48.dp)
            Spacer(Modifier.height(10.dp))
            Text("Task reminder window", style = MaterialTheme.typography.labelLarge)
            Text("Use these as your daily start and end defaults when planning tasks.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EntryField(startReminder, onStartReminder, "Start · 09:00", false, 48.dp, Modifier.weight(1f))
                EntryField(endReminder, onEndReminder, "End · 18:00", false, 48.dp, Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("Your writing stays yours") {
            Text("Thoughts are stored in an encrypted database on this device. Logbook has no account, network AI, sync, ads or analytics. JSON files contain private writing; choose a destination you trust.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SecondaryAction("Export private JSON", Icons.Rounded.FileDownload, onExport, Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            SecondaryAction("Import Logbook JSON", Icons.Rounded.FileUpload, onImport, Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                Text("Delete thoughts and tasks", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("About Logbook") {
            Text("A private mind space. Local-first by design; voice uses only Android’s on-device recognizer when available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onHelp, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Icon(Icons.Rounded.HelpOutline, null); Spacer(Modifier.width(8.dp)); Text("Open detailed guide") }
            TextButton(onClick = onIntro, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("Replay the three-step intro") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun ComposerScreen(text: String, onText: (String) -> Unit, onBack: () -> Unit, onAnalyze: () -> Unit, onMic: () -> Unit, onStop: () -> Unit, micStatus: String) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text("Let it land.", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Text("A messy first draft is welcome. You’ll review everything before it’s saved.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(13.dp))
        EntryField(text, onText, "A link, an idea, something you felt, a detail to remember…", true, 190.dp)
        Text("${text.length} / 2,000", Modifier.align(Alignment.End).padding(top = 5.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(7.dp))
        Text(micStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryAction("Speak on device", Icons.Rounded.Mic, onMic, Modifier.weight(1f))
            SecondaryAction("Stop", Icons.Rounded.StopCircle, onStop, Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        PrimaryAction("Review this thought", MaterialTheme.colorScheme.primary, onAnalyze, text.isNotBlank())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("Keep writing later") }
    }
}

@Composable
internal fun PreviewScreen(
    text: String, onText: (String) -> Unit, tags: List<String>, onToggleTag: (String) -> Unit,
    taskSuggested: Boolean, addTask: Boolean, onAddTask: (Boolean) -> Unit,
    onBack: () -> Unit, onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.ArrowBack, "Back to editing") }
            Text("A little scan…", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Text("Read it once. Change anything that doesn’t feel right.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(13.dp))
        EntryField(text, onText, "Your thought", true, 130.dp)
        Spacer(Modifier.height(15.dp))
        Text("Suggested shelves", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
        Text("Pick the labels that help you find it later.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        LogbookCategories.keys.toList().chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { key ->
                    CategoryChip(LogbookCategories[key].orEmpty(), key in tags, MaterialTheme.colorScheme.primary, Modifier.weight(1f)) { onToggleTag(key) }
                }
            }
        }
        val attrs = remember(text) { SmartNlp.analyze(text).second }
        if (attrs.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("Noticed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp)); Text(attrs.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (taskSuggested) {
            Spacer(Modifier.height(12.dp))
            DewSurface(Modifier.fillMaxWidth(), tone = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onAddTask(!addTask) }.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(addTask, onCheckedChange = onAddTask)
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("This sounds like a next step", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text("Also put it in Things in motion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        PrimaryAction(if (addTask) "Save thought + task" else "Save to Logbook", MaterialTheme.colorScheme.primary, onSave, text.isNotBlank())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("Back to editing") }
    }
}

internal fun accentColor(index: Int, dark: Boolean): Color {
    val colors = if (dark) listOf(Color(0xFFF39159), Color(0xFFE9C476), Color(0xFFADB8BB), Color(0xFFF08B6F), Color(0xFFEAA99A), Color(0xFFDCEB82))
    else listOf(Color(0xFFE8752A), Color(0xFFC8943A), Color(0xFF7D8587), Color(0xFF9F4030), Color(0xFFD98478), Color(0xFFA5B45E))
    return colors[index.coerceIn(0, colors.lastIndex)]
}
