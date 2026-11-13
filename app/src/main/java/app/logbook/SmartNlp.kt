package app.logbook

import java.util.Locale
import java.util.UUID
import java.util.Calendar

object SmartNlp {
    val categories=listOf("ideas","projects","emotions","links","code","life","persons","refs")
    fun analyze(text:String):Pair<List<String>,List<String>> {val s=text.lowercase(Locale.ROOT);val scores=linkedMapOf("code" to listOf("code","bug","function","api","python","kotlin","javascript","build","error","github").count{s.contains(it)}*1.0,"links" to listOf("http://","https://","www.","link").count{s.contains(it)}*2.0,"ideas" to listOf("idea","imagine","could","maybe","concept","what if").count{s.contains(it)}*1.0,"projects" to listOf("project","launch","design","plan","build","create").count{s.contains(it)}*1.0,"emotions" to listOf("feel","happy","sad","anxious","excited","worried","love").count{s.contains(it)}*1.0,"life" to listOf("remember","need to","call","appointment","buy","today","tomorrow").count{s.contains(it)}*1.0,"persons" to listOf("met ","said ","email","friend","mom","dad").count{s.contains(it)}*1.0,"refs" to listOf("quote","book","article","reference","source").count{s.contains(it)}*1.0)
        val thresholds=mapOf("code" to 3.0,"links" to 2.0,"ideas" to 1.8,"projects" to 1.8,"emotions" to 2.0,"life" to 1.5,"persons" to 3.0,"refs" to 2.0)
        val tags=scores.filter{(k,v)->v>=thresholds.getValue(k)}.keys.toList().ifEmpty{listOf(scores.maxByOrNull{it.value}?.takeIf{it.value>0}?.key?:"ideas")}
        val attrs=mutableListOf<String>();Regex("https?://\S+").findAll(text).forEach{attrs+=it.value};Regex("\b(today|tomorrow|tonight|next week|monday|tuesday|wednesday|thursday|friday|saturday|sunday)\b",RegexOption.IGNORE_CASE).findAll(text).forEach{attrs+=it.value};return tags to attrs.distinct().take(10)
    }
    fun due(text:String):Long? {val s=text.lowercase();val c=Calendar.getInstance();when{ "tomorrow" in s->c.add(Calendar.DAY_OF_YEAR,1);"next week" in s->c.add(Calendar.DAY_OF_YEAR,7);"today" in s||"tonight" in s->Unit;else->{val weekday=listOf("sunday","monday","tuesday","wednesday","thursday","friday","saturday").indexOfFirst{s.contains(it)};if(weekday<0)return null;val d=(weekday-c.get(Calendar.DAY_OF_WEEK)+7)%7;c.add(Calendar.DAY_OF_YEAR,if(d==0)7 else d)}};c.set(Calendar.HOUR_OF_DAY,9);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);return c.timeInMillis}
    fun id(prefix:String)="${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
    fun search(items:List<Memory>,q:String,category:String):List<Memory>{val query=q.trim().lowercase();val filtered=if(category=="all")items else items.filter{category in it.tags};if(query.isEmpty())return if(category=="all")emptyList() else filtered.take(20);val words=query.split(Regex("\s+")).filter{it.isNotBlank()};return filtered.mapNotNull{m->val text=m.text.lowercase();var score=if(text.contains(query))6 else 0;words.forEach{w->if(text.contains(w))score+=2};if(m.attributes.any{it.lowercase().contains(query)})score+=2;if(m.tags.any{it.lowercase().contains(query)})score+=1;if(score>0)m to score else null}.sortedByDescending{it.second}.take(20).map{it.first}}
}
