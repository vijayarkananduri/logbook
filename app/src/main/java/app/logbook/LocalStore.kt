package app.logbook

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteOpenHelper
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject

data class Memory(val id:String,val text:String,val tags:List<String>,val attributes:List<String>,val created:Long)
data class Todo(val id:String,val text:String,val due:Long?,val category:String="life",val completed:Boolean=false,val created:Long=System.currentTimeMillis(),val memoryId:String?=null)

private object KeyVault {
    private const val alias="logbook_local_db_key"
    private fun key(): SecretKey {
        val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}
        (ks.getKey(alias,null) as? SecretKey)?.let{return it}
        val gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore")
        gen.init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return gen.generateKey()
    }
    fun passphrase(ctx:Context):ByteArray {
        val prefs=ctx.getSharedPreferences("local_key",Context.MODE_PRIVATE)
        val c=Cipher.getInstance("AES/GCM/NoPadding")
        val enc=prefs.getString("wrapped",null)
        if(enc==null){val raw=ByteArray(32).also{java.security.SecureRandom().nextBytes(it)};c.init(Cipher.ENCRYPT_MODE,key());prefs.edit().putString("iv",Base64.encodeToString(c.iv,Base64.NO_WRAP)).putString("wrapped",Base64.encodeToString(c.doFinal(raw),Base64.NO_WRAP)).commit();return raw}
        c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,Base64.decode(prefs.getString("iv","") ,Base64.NO_WRAP)))
        return c.doFinal(Base64.decode(enc,Base64.NO_WRAP))
    }
}

class LocalStore(ctx:Context):SQLiteOpenHelper(ctx.applicationContext,"logbook.db",null,1){
    private val app=ctx.applicationContext
    private val pass=android.util.Base64.encodeToString(KeyVault.passphrase(app),android.util.Base64.NO_WRAP)
    init{SQLiteDatabase.loadLibs(app)}
    override fun onCreate(db:SQLiteDatabase){db.execSQL("CREATE TABLE memories(id TEXT PRIMARY KEY,text TEXT NOT NULL,tags TEXT NOT NULL,attrs TEXT NOT NULL,created INTEGER NOT NULL)");db.execSQL("CREATE TABLE todos(id TEXT PRIMARY KEY,text TEXT NOT NULL,due INTEGER,category TEXT NOT NULL,completed INTEGER NOT NULL,created INTEGER NOT NULL,memoryId TEXT)")}
    override fun onUpgrade(db:SQLiteDatabase,o:Int,n:Int){}
    @Synchronized private fun db()=getWritableDatabase(pass)
    fun memories():List<Memory>{val out=mutableListOf<Memory>();db().rawQuery("SELECT id,text,tags,attrs,created FROM memories ORDER BY created DESC",null).use{while(it.moveToNext())out+=Memory(it.getString(0),it.getString(1),strList(it.getString(2)),strList(it.getString(3)),it.getLong(4))};return out}
    fun todos():List<Todo>{val out=mutableListOf<Todo>();db().rawQuery("SELECT id,text,due,category,completed,created,memoryId FROM todos",null).use{while(it.moveToNext())out+=Todo(it.getString(0),it.getString(1),if(it.isNull(2))null else it.getLong(2),it.getString(3),it.getInt(4)==1,it.getLong(5),if(it.isNull(6))null else it.getString(6))};return out}
    fun save(m:Memory){db().execSQL("INSERT OR REPLACE INTO memories VALUES(?,?,?,?,?)",arrayOf(m.id,m.text,JSONArray(m.tags).toString(),JSONArray(m.attributes).toString(),m.created))}
    fun deleteMemory(id:String){db().execSQL("DELETE FROM memories WHERE id=?",arrayOf(id))}
    fun saveTodo(t:Todo){db().execSQL("INSERT OR REPLACE INTO todos VALUES(?,?,?,?,?,?,?)",arrayOf(t.id,t.text,t.due,t.category,if(t.completed)1 else 0,t.created,t.memoryId))}
    fun deleteTodo(id:String){db().execSQL("DELETE FROM todos WHERE id=?",arrayOf(id))}
    fun clear(){db().beginTransaction();try{db().execSQL("DELETE FROM memories");db().execSQL("DELETE FROM todos");db().setTransactionSuccessful()}finally{db().endTransaction()}}
    fun export(name:String):String{val ms=JSONArray();memories().forEach{m->ms.put(JSONObject().put("id",m.id).put("text",m.text).put("tags",JSONArray(m.tags)).put("tag",m.tags.firstOrNull()?:"ideas").put("attributes",JSONArray(m.attributes)).put("created",m.created))};val ts=JSONArray();todos().forEach{t->val j=JSONObject().put("id",t.id).put("text",t.text).put("dueDate",t.due?:JSONObject.NULL).put("category",t.category).put("completed",t.completed).put("created",t.created).put("memoryId",t.memoryId?:JSONObject.NULL);ts.put(j)};return JSONObject().put("memories",ms).put("todos",ts).put("userName",name).put("exported",java.time.Instant.now().toString()).toString(2)}
    fun import(json:String,currentName:String):String {val root=JSONObject(json);require(root.has("memories")&&root.get("memories") is JSONArray){"The JSON file must contain a memories array."};val incoming=root.getJSONArray("memories");val todoArray=root.optJSONArray("todos");val db=db();db.beginTransaction();try{val seen=hashSetOf<String>();for(i in 0 until incoming.length()){val j=incoming.getJSONObject(i);val id=j.getString("id");require(id.isNotBlank()&&seen.add(id)){"Duplicate or empty memory ID."};val text=j.getString("text").trim();require(text.isNotEmpty());val tags=j.optJSONArray("tags")?:JSONArray().put(j.optString("tag","ideas"));require(tags.length()>0);val attrs=j.optJSONArray("attributes")?:JSONArray();val tagList=(0 until tags.length()).map{tags.getString(it)};val atList=(0 until attrs.length()).map{attrs.getString(it)};db.execSQL("INSERT OR IGNORE INTO memories VALUES(?,?,?,?,?)",arrayOf(id,text,JSONArray(tagList).toString(),JSONArray(atList).toString(),j.getLong("created")))};if(todoArray!=null){db.execSQL("DELETE FROM todos");val ids=hashSetOf<String>();for(i in 0 until todoArray.length()){val j=todoArray.getJSONObject(i);val id=j.getString("id");require(ids.add(id)){"Duplicate todo ID."};db.execSQL("INSERT INTO todos VALUES(?,?,?,?,?,?,?)",arrayOf(id,j.getString("text"),if(j.isNull("dueDate"))null else j.getLong("dueDate"),j.optString("category","life"),if(j.optBoolean("completed"))1 else 0,j.getLong("created"),if(j.isNull("memoryId"))null else j.optString("memoryId")))}};db.setTransactionSuccessful()}finally{db.endTransaction()};return if(currentName.isBlank())root.optString("userName","") else currentName}
}
private fun strList(s:String)=try{val a=JSONArray(s);(0 until a.length()).map{a.getString(it)}}catch(_:Exception){emptyList()}
