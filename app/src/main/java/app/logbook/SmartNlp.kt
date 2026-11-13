package app.logbook

import java.util.Calendar
import java.util.Locale
import java.util.UUID

/** Predictable, deterministic helpers. This object never performs network or remote-model calls. */
object SmartNlp {
    val categories = listOf("ideas", "projects", "emotions", "links", "code", "life", "persons", "refs")

    private val signals = linkedMapOf(
        "code" to listOf("code", "bug", "function", "api", "python", "kotlin", "javascript", "compile", "repository", "github", "stack trace"),
        "links" to listOf("http://", "https://", "www.", "link to", "bookmark"),
        "ideas" to listOf("idea", "imagine", "what if", "maybe", "concept", "i think", "could be", "would be nice"),
        "projects" to listOf("project", "launch", "design", "roadmap", "ship", "prototype", "milestone", "working on"),
        "emotions" to listOf("feel", "feeling", "happy", "sad", "anxious", "excited", "worried", "love", "overwhelmed", "grateful"),
        "life" to listOf("remember", "need to", "call", "appointment", "buy", "today", "tomorrow", "family", "home"),
        "persons" to listOf("met ", "said ", "email from", "friend", "mom", "dad", "sarah", "alex", "sam"),
        "refs" to listOf("quote", "book", "article", "reference", "source", "read later", "paper")
    )
    private val taskStarts = Regex("(?i)^\\s*(remind me to|remember to|don['’]t forget to|todo:|task:|add (?:a )?task(?: to)?|create (?:a )?todo(?: to)?)\\s*")
    private val taskHints = listOf("need to", "have to", "must ", "don’t forget", "don't forget", "remind me", "remember to", "todo:", "task:", "call ", "buy ", "schedule ", "email ", "finish ")
    private val dateWords = Regex("(?i)\\b(today|tomorrow|tonight|next week|in \\d{1,2} days?|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b")

    fun analyze(text: String): Pair<List<String>, List<String>> {
        val normalized = text.lowercase(Locale.ROOT)
        val scores = linkedMapOf<String, Int>()
        signals.forEach { (category, words) ->
            scores[category] = words.sumOf { raw ->
                val signal = raw.trim()
                val count = if (signal == "http://" || signal == "https://" || signal == "www.") {
                    if (normalized.contains(signal)) 2 else 0
                } else Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(signal) + "(?![\\p{L}\\p{N}])").findAll(normalized).count()
                count * if (category == "links" && (signal.contains("://") || signal == "www.")) 3 else 1
            }
        }
        if (Regex("https?://\\S+", RegexOption.IGNORE_CASE).containsMatchIn(text)) scores["links"] = (scores["links"] ?: 0) + 3
        val people = entities(text)
        val tags = scores.filter { it.value > 0 }.keys.toMutableList()
        if (people.isNotEmpty() && "persons" !in tags) tags += "persons"
        if (tags.isEmpty()) tags += "ideas"
        return tags.distinct() to attributes(text)
    }

    fun attributes(text: String): List<String> {
        val values = linkedSetOf<String>()
        Regex("https?://[^\\s<>]+", RegexOption.IGNORE_CASE).findAll(text).forEach {
            values += it.value.trimEnd('.', ',', ';', ')', ']')
        }
        dateWords.findAll(text).forEach { values += it.value }
        entities(text).forEach { values += it }
        return values.take(12)
    }

    /** Proper-name mentions are stored as searchable attributes, not sent to a server. */
    fun entities(text: String): List<String> {
        val stop = setOf("I", "I’m", "I’ll", "The", "A", "An", "Today", "Tomorrow", "Tonight", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Remember", "Maybe", "Note", "Idea", "Logbook")
        return Regex("\\b[A-Z][a-z]+(?:[-'][A-Z]?[a-z]+)?(?:\\s+[A-Z][a-z]+){0,2}\\b")
            .findAll(text).map { it.value.trim() }.filter { it !in stop && it.length > 1 }.distinct().take(8).toList()
    }

    fun taskCandidate(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.ROOT)
        return taskStarts.containsMatchIn(text) || taskHints.any { normalized.contains(it) }
    }

    fun isTaskCommand(text: String): Boolean = taskStarts.containsMatchIn(text)

    fun taskText(text: String): String {
        val body = text.trim().replaceFirst(taskStarts, "").trim()
        return body.removePrefix("to ").removePrefix("To ").trim().ifBlank { text.trim() }
    }

    /** Only returns a due time when the writing contains an explicit date or clock time. */
    fun due(text: String): Long? {
        val lower = text.lowercase(Locale.ROOT)
        val now = Calendar.getInstance()
        val due = now.clone() as Calendar
        var hasDate = false
        var defaultHour = 9
        val inDays = Regex("\\bin (\\d{1,2}) days?\\b").find(lower)
        when {
            Regex("\\btomorrow\\b").containsMatchIn(lower) -> { due.add(Calendar.DAY_OF_YEAR, 1); hasDate = true }
            inDays != null -> { due.add(Calendar.DAY_OF_YEAR, (inDays.groupValues[1].toIntOrNull() ?: 1).coerceIn(1, 60)); hasDate = true }
            Regex("\\bnext week\\b").containsMatchIn(lower) -> { due.add(Calendar.DAY_OF_YEAR, 7); hasDate = true }
            Regex("\\b(today|tonight)\\b").containsMatchIn(lower) -> {
                hasDate = true
                defaultHour = if (lower.contains("tonight") || now.get(Calendar.HOUR_OF_DAY) >= 9) 20 else 9
            }
            else -> {
                val days = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
                val weekday = days.firstOrNull { Regex("\\b(?:next\\s+)?$it\\b").containsMatchIn(lower) }
                if (weekday != null) {
                    val target = days.indexOf(weekday) + 1
                    var offset = (target - now.get(Calendar.DAY_OF_WEEK) + 7) % 7
                    if (offset == 0 || Regex("\\bnext\\s+$weekday\\b").containsMatchIn(lower)) offset += 7
                    due.add(Calendar.DAY_OF_YEAR, offset)
                    hasDate = true
                }
            }
        }
        val ampm = Regex("\\b(?:at\\s*)?(1[0-2]|0?[1-9])(?::([0-5]\\d))?\\s*(am|pm)\\b").find(lower)
        val military = Regex("\\bat\\s+([01]?\\d|2[0-3]):([0-5]\\d)\\b").find(lower)
        val hour: Int
        val minute: Int
        when {
            ampm != null -> {
                val raw = ampm.groupValues[1].toInt()
                hour = raw % 12 + if (ampm.groupValues[3] == "pm") 12 else 0
                minute = ampm.groupValues[2].takeIf { it.isNotBlank() }?.toInt() ?: 0
                hasDate = true
            }
            military != null -> {
                hour = military.groupValues[1].toInt()
                minute = military.groupValues[2].toInt()
                hasDate = true
            }
            else -> { hour = defaultHour; minute = 0 }
        }
        if (!hasDate) return null
        due.set(Calendar.HOUR_OF_DAY, hour)
        due.set(Calendar.MINUTE, minute)
        due.set(Calendar.SECOND, 0)
        due.set(Calendar.MILLISECOND, 0)
        if (due.timeInMillis <= now.timeInMillis && !Regex("\\b(today|tonight)\\b").containsMatchIn(lower) && (ampm != null || military != null)) {
            due.add(Calendar.DAY_OF_YEAR, 1)
        }
        return due.timeInMillis
    }

    fun id(prefix: String): String = "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

    fun search(items: List<Memory>, query: String, category: String): List<Memory> {
        val normalized = query.trim().lowercase(Locale.ROOT)
        val filter = when (category) { "people" -> "persons"; "files" -> "refs"; else -> category }
        val candidates = if (filter == "all") items else items.filter { filter in it.tags }
        if (normalized.isEmpty()) return if (filter == "all") emptyList() else candidates.take(40)
        val words = normalized.split(Regex("\\s+")).filter { it.length > 1 }
        return candidates.mapNotNull { memory ->
            val fullText = (listOf(memory.text) + memory.tags + memory.attributes).joinToString(" ").lowercase(Locale.ROOT)
            var score = if (fullText.contains(normalized)) 8 else 0
            words.forEach { word -> if (fullText.contains(word)) score += if (memory.text.lowercase(Locale.ROOT).contains(word)) 3 else 1 }
            if (score > 0) memory to score else null
        }.sortedWith(compareByDescending<Pair<Memory, Int>> { it.second }.thenByDescending { it.first.created })
            .take(40).map { it.first }
    }
}
