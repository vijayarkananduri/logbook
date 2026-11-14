package app.logbook

import java.util.Calendar
import java.util.Locale
import java.util.UUID

internal data class EntityHub(val name: String, val type: String, val mentions: Int, val memories: List<Memory>)
internal data class SimilarMemory(val memory: Memory, val score: Float)

/** Predictable, deterministic helpers. This object never performs network or remote-model calls. */
object SmartNlp {
    val categories = listOf("ideas", "projects", "work", "learning", "health", "emotions", "relationships", "family", "travel", "places", "money", "food", "events", "goals", "habits", "links", "code", "life", "tasks", "persons", "refs", "media")
    private val stopWords = setOf(
        "about", "after", "again", "also", "and", "are", "because", "been", "before", "being", "but", "could", "did", "does", "doing", "down", "each", "for", "from", "have", "here", "into", "just", "like", "more", "most", "need", "note", "notes", "only", "other", "our", "out", "over", "same", "some", "than", "that", "the", "their", "them", "then", "there", "these", "they", "this", "those", "through", "today", "tomorrow", "under", "very", "was", "were", "what", "when", "where", "which", "while", "with", "would", "your", "idea", "ideas", "project", "projects", "thought", "thoughts", "logbook"
    )

    private val signals = linkedMapOf(
        "code" to listOf("code", "bug", "function", "api", "python", "kotlin", "javascript", "compile", "repository", "github", "stack trace"),
        "links" to listOf("http://", "https://", "www.", "link to", "bookmark"),
        "ideas" to listOf("idea", "imagine", "what if", "maybe", "concept", "i think", "could be", "would be nice"),
        "projects" to listOf("project", "launch", "design", "roadmap", "ship", "prototype", "milestone", "working on"),
        "work" to listOf("work", "client", "colleague", "deadline", "meeting", "office", "business", "team", "manager"),
        "learning" to listOf("learn", "study", "course", "class", "lesson", "practice", "research", "understand"),
        "health" to listOf("health", "doctor", "medicine", "workout", "exercise", "sleep", "therapy", "symptom"),
        "emotions" to listOf("feel", "feeling", "happy", "sad", "anxious", "excited", "worried", "love", "overwhelmed", "grateful"),
        "relationships" to listOf("friend", "partner", "relationship", "date", "talk with", "conversation with", "said to me"),
        "family" to listOf("family", "mom", "mum", "dad", "sister", "brother", "daughter", "son", "grandma", "grandpa"),
        "travel" to listOf("travel", "trip", "flight", "hotel", "vacation", "holiday", "itinerary", "passport"),
        "places" to listOf("place", "address", "restaurant", "cafe", "city", "neighborhood", "venue", "map"),
        "money" to listOf("money", "budget", "price", "cost", "expense", "save for", "invoice", "bill", "payment"),
        "food" to listOf("food", "recipe", "cook", "lunch", "dinner", "breakfast", "coffee", "meal"),
        "events" to listOf("event", "birthday", "wedding", "concert", "conference", "party", "celebration"),
        "goals" to listOf("goal", "aim", "target", "long-term", "one day", "aspire", "milestone"),
        "habits" to listOf("habit", "routine", "every day", "daily", "weekly", "consistency", "streak"),
        "life" to listOf("remember", "need to", "call", "appointment", "buy", "today", "tomorrow", "family", "home"),
        "persons" to listOf("met ", "said ", "email from", "friend", "mom", "dad", "sarah", "alex", "sam"),
        "refs" to listOf("quote", "book", "article", "reference", "source", "read later", "paper", "citation"),
        "media" to listOf("movie", "film", "show", "podcast", "song", "album", "video", "playlist")
    )
    private val taskStarts = Regex("(?i)^\\s*(?:(?:could|can) you\\s+|please\\s+)?(remind me (?:to|about)|remember to|don['’]t forget to|make sure i|i need to|i have to|i should|todo:|task:|add (?:a )?task(?: to)?|create (?:a )?todo(?: to)?|put .+ on my (?:task|todo|to-do) list(?: to)?)\\s*")
    private val taskHints = listOf("need to", "have to", "must ", "don’t forget", "don't forget", "remind me", "remember to", "todo:", "task:", "call ", "buy ", "schedule ", "email ", "finish ", "make sure i", "add this to my list")
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
        if (taskCandidate(text) && "tasks" !in tags) tags += "tasks"
        if (tags.isEmpty()) tags += "ideas"
        return tags.distinct() to attributes(text)
    }

    /** Makes automatic multi-tag suggestions understandable without hiding their local rules. */
    fun tagEvidence(text: String): List<Pair<String, List<String>>> {
        val normalized = text.lowercase(Locale.ROOT)
        val evidence = signals.mapNotNull { (tag, phrases) ->
            val matched = phrases.map(String::trim).filter { phrase ->
                if (phrase.contains("://") || phrase == "www.") normalized.contains(phrase)
                else Regex("(?<![\\p{L}\\p{N}])" + Regex.escape(phrase) + "(?![\\p{L}\\p{N}])").containsMatchIn(normalized)
            }.distinct().take(3)
            if (matched.isEmpty()) null else tag to matched
        }.toMutableList()
        if (Regex("https?://\\S+", RegexOption.IGNORE_CASE).containsMatchIn(text)) evidence.add("links" to listOf("web address"))
        val names = entities(text)
        if (names.isNotEmpty()) evidence.add("persons" to names.take(3))
        if (taskCandidate(text)) evidence.add("tasks" to listOf("action wording"))
        return evidence.distinctBy { it.first }
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

    fun namedEntities(text: String): List<Pair<String, String>> {
        val ignored = setOf("I", "A", "An", "The", "This", "That", "These", "Those", "Today", "Tomorrow", "Tonight", "Yesterday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday", "Remember", "Maybe", "Note", "Idea", "Logbook", "Meeting", "Met", "Project", "Task", "Call", "Read", "Buy", "Finish", "Next", "Last")
        val pattern = Regex("\\b[A-Z][a-z]+(?:[-'][A-Z]?[a-z]+)?(?:\\s+[A-Z][a-z]+){0,2}\\b")
        return pattern.findAll(text).mapNotNull { match ->
            val name = match.value.trim()
            if (name in ignored || name.length < 2) return@mapNotNull null
            val context = text.substring((match.range.first - 48).coerceAtLeast(0), (match.range.last + 49).coerceAtMost(text.length)).lowercase(Locale.ROOT)
            val type = when {
                Regex("\\b(project|prototype|roadmap|launch|design|build|ship|deploy|feature|app|product)\\b").containsMatchIn(context) -> "projects"
                else -> "people"
            }
            name to type
        }.distinctBy { it.first.lowercase(Locale.ROOT) }.take(16).toList()
    }

    internal fun similarMemories(text: String, items: List<Memory>, excludeId: String? = null, limit: Int = 3): List<SimilarMemory> {
        val queryWords = keywords(text)
        if (queryWords.size < 2) return emptyList()
        return items.asSequence().filter { it.id != excludeId }.mapNotNull { memory ->
            val noteWords = keywords((listOf(memory.text) + memory.tags + memory.attributes).joinToString(" "))
            if (noteWords.isEmpty()) return@mapNotNull null
            val shared = queryWords.intersect(noteWords).size
            var score = shared.toFloat() / maxOf(queryWords.size, noteWords.size).toFloat()
            if (memory.text.contains(text.trim(), ignoreCase = true) && text.trim().length >= 8) score += 0.24f
            if ("ideas" in memory.tags && queryWords.size >= 2 && shared >= 2) score += 0.06f
            if (score < 0.24f) null else SimilarMemory(memory, score.coerceAtMost(1f))
        }.sortedWith(compareByDescending<SimilarMemory> { it.score }.thenByDescending { it.memory.created })
            .take(limit.coerceIn(1, 5)).toList()
    }

    internal fun hubs(items: List<Memory>): List<EntityHub> {
        val grouped = linkedMapOf<String, Pair<String, MutableList<Memory>>>()
        items.forEach { memory ->
            namedEntities(memory.text).forEach { (name, type) ->
                val key = name.lowercase(Locale.ROOT)
                val entry = grouped.getOrPut(key) { name to mutableListOf() }
                if (entry.second.none { it.id == memory.id }) entry.second += memory
                if (type == "projects") grouped[key] = entry.first to entry.second
            }
        }
        val entityHubs = grouped.map { (key, value) ->
            val type = items.asSequence().filter { memory -> namedEntities(memory.text).any { it.first.equals(key, true) } }
                .flatMap { memory -> namedEntities(memory.text).asSequence().filter { it.first.equals(key, true) }.map { it.second } }
                .firstOrNull() ?: "people"
            EntityHub(value.first, type, value.second.size, value.second.sortedByDescending { it.created })
        }
        val topicCounts = linkedMapOf<String, MutableList<Memory>>()
        items.forEach { memory ->
            keywords(memory.text).filter { it.length >= 5 && it !in stopWords }.forEach { word ->
                val list = topicCounts.getOrPut(word) { mutableListOf() }
                if (list.none { it.id == memory.id }) list += memory
            }
        }
        val topics = topicCounts.filterValues { it.size >= 3 }.entries
            .sortedWith(compareByDescending<Map.Entry<String, MutableList<Memory>>> { it.value.size }.thenBy { it.key })
            .take(12).map { (name, linked) -> EntityHub(name, "topics", linked.size, linked.sortedByDescending { it.created }) }
        return (entityHubs + topics).sortedWith(compareBy<EntityHub> { if (it.type == "people") 0 else if (it.type == "projects") 1 else 2 }.thenByDescending { it.mentions }.thenBy { it.name.lowercase(Locale.ROOT) })
    }

    private fun keywords(text: String): Set<String> = Regex("[\\p{L}\\p{N}]{3,}")
        .findAll(text.lowercase(Locale.ROOT)).map { it.value }.filter { it !in stopWords }.toSet()

    fun taskCandidate(text: String): Boolean {
        val normalized = text.trim().lowercase(Locale.ROOT)
        return taskStarts.containsMatchIn(text) || taskHints.any { normalized.contains(it) }
    }

    fun isTaskCommand(text: String): Boolean = taskStarts.containsMatchIn(text) ||
        Regex("(?i)\\b(?:can|could) you (?:add|put) .+ (?:to|on) my (?:tasks|todos|to-do list)\\b").containsMatchIn(text) ||
        Regex("(?i)\\b(?:make|create) (?:me )?(?:a )?(?:task|reminder|todo)\\b").containsMatchIn(text)

    fun taskText(text: String): String {
        val body = text.trim().replaceFirst(taskStarts, "").trim()
            .replaceFirst(Regex("(?i)^(?:to|that i|for me to)\\s+"), "")
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
