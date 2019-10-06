package jsbot.jsapi

import jsbot.JSBot
import jsbot.Role
import org.mozilla.javascript.*
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message

/**
 * Created on 24/09/2019.
 *
 */
class UserUtils {
    companion object {

        fun addUserClassToScope(cx: Context, scope: Scriptable, bot: JSBot, jobMessage: Message? = null) {
            ScriptableObject.defineClass(scope, User::class.java)

            val userProto = ScriptableObject.getClassPrototype(scope, "User")

            ScriptableObject.defineProperty(userProto, "getRole", JSFunction(0) { _, _, thisObj, _ ->

                val fromJS = when(thisObj){
                    is User -> thisObj
                    else -> throw JSBot.JSBotException("Invalid user object.")
                }

                return@JSFunction bot.userRoles[fromJS.id]?.getRoleName()
            }, ScriptableObject.DONTENUM)

            ScriptableObject.defineProperty(
                userProto, "getAbilities", JSFunction(0) { cx2, scope2, thisObj, _ ->
                    val fromJS = when(thisObj){
                        is User -> thisObj
                        else -> throw JSBot.JSBotException("Invalid user object.")
                    }
                    val abilityList = mutableListOf<Any>()
                    bot.userRoles[fromJS.id]?.getAbilites()?.forEach {
                        abilityList.add(it)
                    }
                    return@JSFunction cx2.newArray(scope2, abilityList.toTypedArray())

                },
                ScriptableObject.DONTENUM
            )



            ScriptableObject.defineProperty(
                userProto, "getPublic", JSFunction(0) { cx2, _, thisObj, _ ->
                    val fromJS = when(thisObj){
                        is User -> thisObj
                        else -> throw JSBot.JSBotException("Invalid user object.")
                    }
                    val userScope = bot.retrieveScope(cx2, fromJS.id.toLong())
                    val saved = userScope?.get("saved", userScope)
                    if (saved is Scriptable) {
                        val public = saved.get("public", saved)
                        if (public is Scriptable) {
                            return@JSFunction cx2.evaluateString(public, "eval(toSource())", "<clone>", 1, null)
                        }
                    }
                    return@JSFunction null
                },
                ScriptableObject.DONTENUM
            )


            var remaining = 10

            if (jobMessage !== null) {
                ScriptableObject.defineProperty(userProto, "message",
                    JSFunction(1) { _, _, thisObj, args ->
                        val fromJS = when(thisObj){
                            is User -> thisObj
                            else -> throw JSBot.JSBotException("Invalid user object.")
                        }
                        if (!bot.retrieveRoleFromId(jobMessage.from.id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                            throw JSBot.JSBotException("Not authorized to send private messages.")
                        }

                        if (args.isNotEmpty() && remaining > 0) {
                            var text = Context.toString(args[0])
                            text = if (text === null || text.isEmpty()) "_" else text
                            bot.execute(
                                SendMessage()
                                    .setChatId(fromJS.id.toLong())
                                    .setText(
                                        "From: " + (jobMessage.from.userName ?: jobMessage.from.id) + "\n" + text
                                    )
                            )
                            --remaining
                            return@JSFunction ""
                        } else if (remaining <= 0) {
                            throw JSBot.JSBotException("Message limit reached.")
                        } else {
                            throw JSBot.JSBotException("Missing argument.")
                        }
                    }, ScriptableObject.DONTENUM
                )

                ScriptableObject.defineProperty(userProto, "sendMedia", JSFunction(1) { _, _, thisObj, args ->
                    val fromJS = when(thisObj){
                        is User -> thisObj
                        else -> throw JSBot.JSBotException("Invalid user object.")
                    }
                    if (!bot.retrieveRoleFromId(jobMessage.from.id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                        throw JSBot.JSBotException("Not authorized to send private messages.")
                    }
                    if (args.isNotEmpty() && remaining > 0) {
                        val argument = args[0]
                        if (argument is Scriptable) {
                            val media = SimpleMedia.fromJS(argument)
                            if (media !== null) {
                                bot.execute(
                                    SendMessage()
                                        .setChatId(fromJS.id.toLong())
                                        .setText("From: " + (jobMessage.from.userName ?: jobMessage.from.id))
                                )
                                SimpleMedia.send(
                                    media,
                                    fromJS.id.toLong(),
                                    bot
                                )
                                --remaining
                                --remaining
                                return@JSFunction ""
                            }
                        }
                        throw JSBot.JSBotException("Argument seems not to be a valid Media object")
                    } else if (remaining <= 0) {
                        throw JSBot.JSBotException("Message limit reached.")
                    } else {
                        throw JSBot.JSBotException("Missing argument.")
                    }

                }, ScriptableObject.DONTENUM)
            } else {
                ScriptableObject.defineProperty(userProto, "message", null, ScriptableObject.DONTENUM)
                ScriptableObject.defineProperty(userProto, "sendMedia", null, ScriptableObject.DONTENUM)
            }
        }

    }
}