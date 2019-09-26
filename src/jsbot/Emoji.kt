package jsbot

import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

/**
 * Created on 25/09/2019.
 *
 */
class Emoji {

    companion object{
        var emojis = JSONObject()
        private var emojiLoaded = false
        fun isEmojiLoaded() = emojiLoaded

        fun loadEmojis(filePath:String){
            try{
                emojis = JSONObject(JSONTokener(File(filePath).reader()))
                emojiLoaded = true
            }catch(e:Throwable){
                e.printStackTrace()
            }
        }

        fun findEmoji(kw:String) : List<String>{
            return searchByKeyword(kw).map { it.getString("char") }.toList()
        }

        fun searchByKeyword(kw :String) : MutableList<JSONObject>{
            val result = mutableListOf<JSONObject>()
            emojis.keySet().forEach{ emokey ->
                val emoEntry = emojis.getJSONObject(emokey)
                if(emokey.toLowerCase().contains(kw.toLowerCase())){
                    result.add(emoEntry)
                }else {
                    val keyWords = emoEntry.getJSONArray("keywords")
                    val kwStrings = keyWords.toList().map { it as String }.map { it.toLowerCase() }.toList()
                    if (kwStrings.any { it.contains(kw.toLowerCase()) }) {
                        result.add(emoEntry)
                    }
                }
            }
            return result
        }




    }
}