package jsbot.jsapi

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * Created on 28/09/2019.
 */
class Event(val type: String, val content: Any) {
    fun toJS(cx: Context, scope: Scriptable): Scriptable? {
        val result = cx.newObject(scope)
        ScriptableObject.putProperty(result, "eventType", type)

        val jscontent = if (content is SimpleMedia) {
            content.toJS(cx, scope)
        } else {
            content
        }

        ScriptableObject.putProperty(result, "content", jscontent)
        return result
    }

    companion object {
        const val TEXT_MESSAGE_EVENT_TYPE = "msg_text"
        const val MEDIA_MESSAGE_EVENT_TYPE = "msg_media"

        fun fromJS(obj: Scriptable): Event? {
            val eventType = obj.get("eventType", obj)
            val content = obj.get("content", obj)
            return if (eventType is String) {
                Event(eventType, content)
            } else {
                null
            }
        }
    }
}
