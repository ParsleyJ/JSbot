package jsbot

import org.mozilla.javascript.*
import org.telegram.telegrambots.meta.api.methods.send.SendMessage

/**
 * Created on 24/09/2019.
 *
 */
class User(val id: Int, var userName: String? = null) {
    fun toJS(cx: Context, scope: Scriptable, bot: JSBot): Scriptable? {
        val to = cx.newObject(scope)
        ScriptableObject.putProperty(to, "id", id)
        ScriptableObject.putProperty(to, "username", userName)
        ScriptableObject.putProperty(to, "getRole", object : BaseFunction() {
            override fun call(cx: Context?, scope: Scriptable?, thisObj: Scriptable?, args: Array<out Any>?): Any? {
                return bot.userRoles[id]?.getRoleName()
            }

            override fun getArity(): Int = 0
        })


        ScriptableObject.putProperty(to, "getAbilities", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any {
                val abilityList = mutableListOf<Any>()
                bot.userRoles[id]?.getAbilites()?.forEach {
                    abilityList.add(it)
                }
                return cx.newArray(scope, abilityList.toTypedArray())
            }

            override fun getArity(): Int = 0
        })




        val userScope = bot.retrieveScope(cx, id.toLong())
        val saved = userScope?.get("saved", userScope)

        if(saved is Scriptable) {
            val public = saved.get("public", saved)
            if(public is Scriptable){
                val safePublic = cx.evaluateString(public, "eval(toSource())", "<clone>", 1, null)
                ScriptableObject.putProperty(to, "public", safePublic)
            }else{
                ScriptableObject.putProperty(to, "public", null)
            }
        }else{
            ScriptableObject.putProperty(to, "public", null)
        }

        var remaining = 10

        ScriptableObject.putProperty(to, "message", object : BaseFunction() {

            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if(!bot.retrieveRoleFromId(id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)){
                    throw JSBot.JSBotException("Not authorized to send private messages.")
                }
                if (args.isNotEmpty() && remaining > 0) {
                    var text = Context.toString(args[0])
                    text = if (text === null || text.isEmpty()) "_" else text
                    bot.execute(
                        SendMessage()
                            .setChatId(id.toLong())
                            .setText("From: "+(userName?:id) + "\n" + text)
                            .disableNotification()
                    )
                    --remaining
                } else if (remaining <= 0) {
                    throw JSBot.JSBotException("Message limit reached.")
                }
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
            }
        })

        ScriptableObject.putProperty(to, "sendMedia", object : BaseFunction() {

            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if(!bot.retrieveRoleFromId(id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)){
                    throw JSBot.JSBotException("Not authorized to send private messages.")
                }
                if (args.isNotEmpty() && remaining > 0) {
                    val argument = args[0]
                    if (argument is Scriptable) {
                        val media = SimpleMedia.fromJS(argument)
                        if (media !== null) {
                            SimpleMedia.send(
                                media,
                                id.toLong(),
                                bot,
                                "From: "+(userName?:id)
                            )
                            --remaining
                        }
                    }
                } else if (remaining <= 0) {
                    throw JSBot.JSBotException("Message limit reached.")
                }
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
            }
        })


        return to
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