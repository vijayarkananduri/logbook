package app.logbook

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import kotlinx.coroutines.delay

@Composable
internal fun WelcomeScreen(name: String, onName: (String) -> Unit, onContinue: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("logbook", style = MaterialTheme.typography.headlineSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.primary)
        Text("your private mind space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("A place to put it down.", fontFamily = LogbookHand, fontSize = 36.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text("logbook helps you remember what matters without asking you to organize everything first.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        IntroCard(Icons.Rounded.EditNote, "Catch it", "Write or try on-device voice. Read the transcript before you save it.")
        Spacer(Modifier.height(12.dp))
        IntroCard(Icons.Rounded.AutoAwesome, "Let it sort itself", "A small, predictable on-device helper suggests categories, links, dates and names. You stay in charge.")
        Spacer(Modifier.height(12.dp))
        IntroCard(Icons.Rounded.Lock, "Keep it yours", "Thoughts are encrypted on this device. There is no account, sync, analytics or remote AI.")
        Spacer(Modifier.height(28.dp))
        PrimaryAction("Open logbook", MaterialTheme.colorScheme.primary, onStart)
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
internal fun HomeScreen(name: String, memories: List<Memory>, todos: List<Todo>, onMemory: (Memory) -> Unit, onDelete: (Memory) -> Unit, onSearch: () -> Unit, onTasks: () -> Unit, onToggleTask: (Todo) -> Unit, onTagClick: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp).padding(bottom = 88.dp)) {
        Text(greeting(name), fontFamily = LogbookHand, fontSize = 34.sp, lineHeight = 40.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("TODAY", memories.count { isToday(it.created) }.toString(), "thoughts caught", Icons.Rounded.AutoAwesome, Modifier.weight(1f))
            StatTile("IN MOTION", todos.count { !it.completed }.toString(), "open tasks", Icons.Rounded.CheckCircle, Modifier.weight(1f))
        }
        val openTasks = todos.filterNot { it.completed }.take(2)
        if (openTasks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Things in motion", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                TextButton(onClick = onTasks) { Text("See all") }
            }
            DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    openTasks.forEachIndexed { index, task ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = false, onCheckedChange = { onToggleTask(task) })
                            Column(Modifier.weight(1f)) {
                                Text(task.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                task.due?.let { Text(formatDue(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                        }
                        if (index < openTasks.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                    }
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Recent thoughts", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            TextButton(onClick = onSearch, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) { Text("See all") }
        }
        if (memories.isEmpty()) {
            DewSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(19.dp)) {
                    Text("Nothing here yet.", fontFamily = LogbookHand, fontSize = 27.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(3.dp))
                    Text("Use the + button whenever you want to save a thought.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else memories.take(5).forEachIndexed { index, memory ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            MemoryCard(memory, { onMemory(memory) }, { onDelete(memory) }, onTagClick)
        }
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
internal fun SearchScreen(memories: List<Memory>, query: String, onQuery: (String) -> Unit, category: String, onCategory: (String) -> Unit, onMemory: (Memory) -> Unit, onDelete: (Memory) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Find a thought", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(10.dp))
        EntryField(query, onQuery, "Search words, links, people…", false, 50.dp)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CategoryChip("All", category == "all", MaterialTheme.colorScheme.primary) { onCategory("all") }
            LogbookCategories.forEach { (key, label) -> CategoryChip(label, category == key, MaterialTheme.colorScheme.primary) { onCategory(key) } }
        }
        Spacer(Modifier.height(10.dp))
        val results = SmartNlp.search(memories, query, category)
        when {
            query.isBlank() && category == "all" -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel("Your thoughts will be easy to find.", "Search any word, or pick a category to browse what you have saved.")
            }
            results.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyPanel("Nothing matches just yet.", "Try a shorter phrase or another category. Your saved thoughts are still here.")
            }
            else -> LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(results, key = { it.id }) { memory -> MemoryCard(memory, { onMemory(memory) }, { onDelete(memory) }, onCategory) }
            }
        }
    }
}

@Composable
internal fun HubsScreen(memories: List<Memory>, selectedHub: EntityHub?, onSelectHub: (EntityHub?) -> Unit, onMemory: (Memory) -> Unit) {
    val hubs = remember(memories) { SmartNlp.hubs(memories) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        if (selectedHub == null) {
            Text("Your thought hubs", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("People, projects, and topics that keep showing up.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(14.dp))
            if (hubs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyPanel("Your hubs are taking shape.", "Names and recurring topics will gather here as you save thoughts.")
                }
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                listOf("people", "projects", "topics").forEach { type ->
                    val matching = hubs.filter { it.type == type }
                    if (matching.isNotEmpty()) {
                        item(key = "header-$type") {
                            Text(type.replaceFirstChar { it.uppercase() }, Modifier.padding(top = 8.dp, bottom = 2.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(matching, key = { "${it.type}-${it.name.lowercase(Locale.ROOT)}" }) { hub -> HubCard(hub) { onSelectHub(hub) } }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSelectHub(null) }, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.ArrowBack, "Back to hubs") }
                Column {
                    Text(selectedHub.name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${selectedHub.mentions} ${if (selectedHub.mentions == 1) "mention" else "mentions"} · ${selectedHub.type}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (selectedHub.memories.isEmpty()) {
                EmptyPanel("No thoughts linked yet.", "Save a note mentioning ${selectedHub.name} to grow this hub.")
            } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
                items(selectedHub.memories, key = { it.id }) { memory ->
                    DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), onClick = { onMemory(memory) }) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(memory.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(8.dp))
                            Text(formatDate(memory.created), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HubCard(hub: EntityHub, onClick: () -> Unit) {
    DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), onClick = onClick) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            val icon = when (hub.type) {
                "projects" -> Icons.Rounded.FolderOpen
                "topics" -> Icons.Rounded.Tag
                else -> Icons.Rounded.PersonOutline
            }
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(hub.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${hub.memories.size} linked ${if (hub.memories.size == 1) "thought" else "thoughts"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
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
            EmptyPanel("Nothing is asking for you yet.", "Add a task here, or say ‘Remind me to…’ in the local guide.")
        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 88.dp)) {
            items(ordered, key = { it.id }) { task -> TodoCard(task, { onToggle(task) }, { onDelete(task) }) }
        }
    }
}

@Composable
internal fun GuideScreen(chat: List<ChatLine>, draft: String, onDraft: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Guide", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chat.forEach { line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
                    DewSurface(Modifier.fillMaxWidth(if (line.fromUser) 0.82f else 0.90f), tone = if (line.fromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                        Text(line.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EntryField(draft, onDraft, "Ask or find…", false, 42.dp, Modifier.weight(1f))
            IconButton(onClick = onSend, modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Rounded.ArrowUpward, "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text("Private · on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, start = 3.dp))
    }
}

@Composable
internal fun SettingsScreen(
    name: String, onName: (String) -> Unit, themeMode: String, onThemeMode: (String) -> Unit,
    onExport: () -> Unit, onImport: () -> Unit, onClear: () -> Unit, onIntro: () -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text("Make it feel like yours", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(14.dp))
        SettingsGroup("Appearance") {
            Text("Theme", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text("Follow your device, or choose a steady light or dark look.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (mode, label) ->
                    FilterChip(selected = themeMode == mode, onClick = { onThemeMode(mode) }, label = { Text(label) }, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("Your profile") {
            Text("Name", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(7.dp))
            EntryField(name, onName, "A name for the greeting", false, 48.dp)
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("Your writing stays yours") {
            Text("Thoughts are stored in an encrypted database on this device. logbook has no account, network AI, sync, ads or analytics. JSON files contain private writing; choose a destination you trust.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            SecondaryAction("Export private JSON", Icons.Rounded.FileDownload, onExport, Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            SecondaryAction("Import logbook JSON", Icons.Rounded.FileUpload, onImport, Modifier.fillMaxWidth())
            Spacer(Modifier.height(7.dp))
            TextButton(onClick = onClear, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error); Spacer(Modifier.width(8.dp))
                Text("Delete thoughts and tasks", color = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsGroup("About logbook") {
            Text("A private mind space. Local-first by design; voice uses only Android’s on-device recognizer when available.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onIntro, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("See the welcome again") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
internal fun ComposerScreen(text: String, onText: (String) -> Unit, onBack: () -> Unit, onAnalyze: () -> Unit, onMic: () -> Unit, onStop: () -> Unit, micStatus: String, memories: List<Memory>, excludeId: String?, onRelated: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.ArrowBack, "Back") }
            Text("Let it land.", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Text("A messy first draft is welcome. You’ll review everything before it’s saved.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(13.dp))
        EntryField(text, onText, "A link, an idea, something you felt, a detail to remember…", true, 190.dp)
        val related = remember(text, memories, excludeId) { if (text.trim().length >= 8) SmartNlp.similarMemories(text, memories, excludeId, 2) else emptyList() }
        if (related.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            Text("Recent ideas", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text("This sounds close to something you kept. Tap to browse its shelf.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            related.forEach { match ->
                val memory = match.memory
                Spacer(Modifier.height(5.dp))
                DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = { onRelated(memory.tags.firstOrNull() ?: "all") }) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                        Text(memory.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text("${memory.tags.firstOrNull()?.let { LogbookCategories[it] ?: it } ?: "All thoughts"} · ${formatDate(memory.created)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
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
    onBack: () -> Unit, onSave: () -> Unit, similar: List<SimilarMemory>, onRelated: (String) -> Unit
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.ArrowBack, "Back to editing") }
            Text("A little scan…", fontFamily = LogbookHand, fontSize = 32.sp, color = MaterialTheme.colorScheme.onBackground)
        }
        Text("Read it once. Change anything that doesn’t feel right.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(13.dp))
        EntryField(text, onText, "Your thought", true, 130.dp)
        if (similar.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text("A similar thought", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("A small reminder of what you’ve already captured.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            similar.take(2).forEach { match ->
                val memory = match.memory
                Spacer(Modifier.height(5.dp))
                DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), onClick = { onRelated(memory.tags.firstOrNull() ?: "all") }) {
                    Column(Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                        Text(memory.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(memory.tags.firstOrNull()?.let { LogbookCategories[it] ?: it } ?: "All thoughts", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
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
        PrimaryAction(if (addTask) "Save thought + task" else "Save to logbook", MaterialTheme.colorScheme.primary, onSave, text.isNotBlank())
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp)) { Text("Back to editing") }
    }
}

@Composable
internal fun BootSplash(onDismiss: () -> Unit) {
    var phase by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        delay(120)
        phase = 1
        delay(760)
        phase = 2
        delay(520)
        onDismiss()
    }
    val wordAlpha by animateFloatAsState(if (phase >= 2) 1f else 0f, tween(420), label = "splash-word-alpha")
    val wordScale by animateFloatAsState(if (phase >= 2) 1f else 0.86f, tween(500), label = "splash-word-scale")
    val notes = listOf("an idea", "a person", "a link", "a next step")
    val starts = listOf((-92).dp to (-44).dp, 68.dp to (-48).dp, (-78).dp to 50.dp, 76.dp to 48.dp)
    val stacks = listOf((-32).dp to (-20).dp, 31.dp to (-19).dp, (-32).dp to 19.dp, 31.dp to 20.dp)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(142.dp), contentAlignment = Alignment.Center) {
                notes.forEachIndexed { index, note ->
                    val x by animateDpAsState(if (phase == 0) starts[index].first else stacks[index].first, tween(680, delayMillis = index * 55), label = "splash-note-x-$index")
                    val y by animateDpAsState(if (phase == 0) starts[index].second else stacks[index].second, tween(680, delayMillis = index * 55), label = "splash-note-y-$index")
                    val noteAlpha by animateFloatAsState(if (phase >= 2) 0f else 1f, tween(300), label = "splash-note-alpha-$index")
                    Text(note, Modifier.offset(x, y).alpha(noteAlpha).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                Text("logbook", Modifier.alpha(wordAlpha).scale(wordScale), style = MaterialTheme.typography.headlineLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = MaterialTheme.colorScheme.primary)
            }
            Text("your private mind space", Modifier.alpha(wordAlpha), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)) { Text("Skip") }
    }
}

internal fun accentColor(index: Int, dark: Boolean): Color {
    val colors = if (dark) listOf(Color(0xFFF39159), Color(0xFFE9C476), Color(0xFFADB8BB), Color(0xFFF08B6F), Color(0xFFEAA99A), Color(0xFFDCEB82))
    else listOf(Color(0xFFE8752A), Color(0xFFC8943A), Color(0xFF7D8587), Color(0xFF9F4030), Color(0xFFD98478), Color(0xFFA5B45E))
    return colors[index.coerceIn(0, colors.lastIndex)]
}
