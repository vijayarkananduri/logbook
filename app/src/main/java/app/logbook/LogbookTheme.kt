package app.logbook

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal enum class LogbookScreen { Welcome, Intro, Home, Search, Tasks, Guide, Settings, Compose, Preview }
internal data class ChatLine(val fromUser: Boolean, val text: String)

internal val LogbookBody = FontFamily(
    Font(R.font.urbanist, FontWeight.Normal), Font(R.font.urbanist, FontWeight.Medium),
    Font(R.font.urbanist, FontWeight.SemiBold), Font(R.font.urbanist, FontWeight.Bold)
)
internal val LogbookHand = FontFamily(Font(R.font.bebas_neue, FontWeight.Normal))
internal val LogbookCategories = linkedMapOf(
    "ideas" to "Ideas", "projects" to "Projects", "emotions" to "Feelings",
    "links" to "Links", "code" to "Code", "life" to "Life",
    "persons" to "People", "refs" to "References"
)


private val Paper = Color(0xFFF5F0E8)
private val Ink = Color(0xFF2C241D)
private val Night = Color(0xFF1C1916)
private val NightInk = Color(0xFFF3EADF)
private val BaseTypography = Typography()
private val LogbookTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(fontFamily = LogbookHand),
    displayMedium = BaseTypography.displayMedium.copy(fontFamily = LogbookHand),
    displaySmall = BaseTypography.displaySmall.copy(fontFamily = LogbookHand),
    headlineLarge = BaseTypography.headlineLarge.copy(fontFamily = LogbookHand),
    headlineMedium = BaseTypography.headlineMedium.copy(fontFamily = LogbookHand),
    headlineSmall = BaseTypography.headlineSmall.copy(fontFamily = LogbookHand),
    titleLarge = BaseTypography.titleLarge.copy(fontFamily = LogbookBody),
    titleMedium = BaseTypography.titleMedium.copy(fontFamily = LogbookBody),
    titleSmall = BaseTypography.titleSmall.copy(fontFamily = LogbookBody),
    bodyLarge = BaseTypography.bodyLarge.copy(fontFamily = LogbookBody, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = BaseTypography.bodyMedium.copy(fontFamily = LogbookBody, fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = BaseTypography.bodySmall.copy(fontFamily = LogbookBody, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = BaseTypography.labelLarge.copy(fontFamily = LogbookBody, fontWeight = FontWeight.SemiBold),
    labelMedium = BaseTypography.labelMedium.copy(fontFamily = LogbookBody)
)
private val LocalReducedEffects = compositionLocalOf { true }

@Composable
internal fun LogbookTheme(dark: Boolean, content: @Composable () -> Unit) {
    val accent = if (dark) Color(0xFFEF9A70) else Color(0xFFB6532D)
    val onAccent = if (dark) Color(0xFF2D1E17) else Color.White
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = Color(0xFF493126), onPrimaryContainer = NightInk,
        secondary = Color(0xFF9BA67E), onSecondary = Color(0xFF25291E),
        tertiary = Color(0xFFD88970), onTertiary = Night,
        background = Night, onBackground = NightInk,
        surface = Color(0xFF26221F), onSurface = NightInk,
        surfaceVariant = Color(0xFF332D28), onSurfaceVariant = Color(0xFFC5B9AC),
        outline = Color(0xFF5A4D43), outlineVariant = Color(0xFF453B33)
    ) else lightColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = Color(0xFFF3DED3), onPrimaryContainer = Ink,
        secondary = Color(0xFF667454), onSecondary = Color.White,
        tertiary = Color(0xFF9F4937), onTertiary = Color.White,
        background = Paper, onBackground = Ink,
        surface = Color(0xFFFFFCF8), onSurface = Ink,
        surfaceVariant = Color(0xFFECE5DC), onSurfaceVariant = Color(0xFF70665D),
        outline = Color(0xFFD8CEC2), outlineVariant = Color(0xFFE4DCD2)
    )
    CompositionLocalProvider(LocalReducedEffects provides true) {
        MaterialTheme(colorScheme = scheme, typography = LogbookTypography, shapes = Shapes(
            small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)
        ), content = content)
    }
}

@Composable
internal fun LogbookBackdrop(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
}

@Composable
internal fun DewSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    tone: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier.clip(shape)
            .background(tone)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
    ) { content() }
}

@Composable
internal fun BrandBar(name: String, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 5.dp, bottom = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("logbook", style = MaterialTheme.typography.titleMedium, fontFamily = LogbookBody, letterSpacing = 0.4.sp, color = MaterialTheme.colorScheme.onSurface)
            Text("your private mind space", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onSettings, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
internal fun EntryField(value: String, onValue: (String) -> Unit, hint: String, multiline: Boolean, minHeight: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val fill = MaterialTheme.colorScheme.surface.copy(alpha = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) 0.94f else 0.90f)
    Column(modifier.fillMaxWidth().clip(shape).background(fill).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.78f), shape).padding(horizontal = 15.dp, vertical = 12.dp)) {
        Box(Modifier.fillMaxWidth().heightIn(min = minHeight)) {
            if (value.isEmpty()) Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f), style = MaterialTheme.typography.bodyMedium)
            BasicTextField(
                value, onValue,
                Modifier.fillMaxWidth().heightIn(min = minHeight),
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontFamily = LogbookBody, fontSize = 16.sp, lineHeight = 23.sp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                singleLine = !multiline, maxLines = if (multiline) 8 else 1,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (multiline) ImeAction.Default else ImeAction.Done)
            )
        }
    }
}

@Composable
internal fun PrimaryAction(label: String, accent: Color, onClick: () -> Unit, enabled: Boolean = true) {
    Button(onClick, Modifier.fillMaxWidth().defaultMinSize(minHeight = 52.dp), enabled = enabled, shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = if (accent.luminance() > 0.34f) Ink else Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp, focusedElevation = 0.dp, hoveredElevation = 0.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun SecondaryAction(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(onClick, modifier.defaultMinSize(minHeight = 48.dp), shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun CategoryChip(label: String, selected: Boolean, accent: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.defaultMinSize(minHeight = 48.dp).clip(CircleShape)
        .background(if (selected) accent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f))
        .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 13.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        Text(label, color = if (selected) (if (accent.luminance() > 0.34f) Ink else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
internal fun MemoryCard(memory: Memory, onClick: () -> Unit) {
    DewSurface(Modifier.fillMaxWidth(), onClick = onClick, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(memory.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, maxLines = 4, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(memory.tags.take(3).joinToString("  · ") { LogbookCategories[it] ?: it }, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(memory.created), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun TodoCard(task: Todo, onToggle: () -> Unit, onDelete: () -> Unit) {
    DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 5.dp, end = 7.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(task.completed, onCheckedChange = { onToggle() }, modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                Text(task.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                task.due?.let { due ->
                    Spacer(Modifier.height(3.dp))
                    Text((if (!task.completed && due < System.currentTimeMillis()) "Overdue · " else "") + formatDue(due),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (due < System.currentTimeMillis() && !task.completed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.DeleteOutline, "Delete task", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
internal fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp)); content()
        }
    }
}

@Composable
internal fun SettingToggle(icon: ImageVector, title: String, description: String, value: Boolean, onValue: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = value, onCheckedChange = onValue, modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp))
    }
}

@Composable
internal fun BottomNavigation(screen: LogbookScreen, onSelect: (LogbookScreen) -> Unit) {
    val destinations = listOf(
        Triple(LogbookScreen.Home, "Home", Icons.Rounded.Home),
        Triple(LogbookScreen.Tasks, "Tasks", Icons.Rounded.CheckCircle),
        Triple(LogbookScreen.Guide, "Guide", Icons.Rounded.Chat)
    )
    val selectedScreen = when (screen) {
        LogbookScreen.Search, LogbookScreen.Settings -> LogbookScreen.Home
        else -> screen
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(RoundedCornerShape(18.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        destinations.forEach { (destination, label, icon) ->
            val selected = selectedScreen == destination
            Column(Modifier.weight(1f).heightIn(min = 50.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                .clickable(role = Role.Tab) { onSelect(destination) }
                .padding(vertical = 5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Icon(icon, null, tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Text(label, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
        }
    }
}

@Composable
internal fun EmptyPanel(title: String, detail: String) {
    DewSurface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(22.dp)) {
            Text(title, fontFamily = LogbookHand, fontSize = 27.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(5.dp)); Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal fun formatDate(timestamp: Long): String = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))
internal fun formatDue(timestamp: Long): String = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(timestamp))
internal fun isToday(timestamp: Long): Boolean {
    val value = Calendar.getInstance().apply { timeInMillis = timestamp }
    val now = Calendar.getInstance()
    return value.get(Calendar.YEAR) == now.get(Calendar.YEAR) && value.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
}
internal fun greeting(name: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val part = when (hour) { in 5..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }
    return "$part, ${name.trim().ifBlank { "friend" }}."
}
