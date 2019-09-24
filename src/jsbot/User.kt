package jsbot

import org.mozilla.javascript.BaseFunction
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * Created on 24/09/2019.
 *
 */
class User(val id: Int, var userName: String? = null) {
    fun toJS(cx: Context, scope: Scriptable, bot: JSBot): Scriptable? {
        val result = cx.newObject(scope)
        ScriptableObject.putProperty(result, "id", id)
        ScriptableObject.putProperty(result, "username", userName)
        ScriptableObject.putProperty(result, "getRole", object : BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any? {
                return bot.userRoles[id]?.getRoleName()
            }

            override fun getArity(): Int = 0
        })

        val userScope = bot.retrieveScope(cx, id.toLong())
        val saved = userScope?.get("saved", userScope)

        if(saved is Scriptable) {
            val public = saved.get("public", saved)
            if(public is Scriptable){
                val safePublic = cx.evaluateString(public, "eval(toSource())", "<clone>", 1, null)
                ScriptableObject.putProperty(result, "public", safePublic)
            }else{
                ScriptableObject.putProperty(result, "public", null)
            }
        }else{
            ScriptableObject.putProperty(result, "public", null)
        }
        return result
    }

    companion object {

        fun fromJS(obj: Scriptable): User? {
            val id = obj.get("id", obj)
            val username = obj.get("username", obj)
            return if (id is Int && username is String) {
                User(id, username)
            } else if (id is Int && username === null) {
                User(id)
            } else {
                null
            }
        }

        fun fromTgUser(user: org.telegram.telegrambots.meta.api.objects.User?): User? = if (user !== null) {
            User(user.id, user.userName)
        } else {
            null
        }
    }
}