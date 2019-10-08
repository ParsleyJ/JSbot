package jsbot.jsapi

import jsbot.Emoji
import jsbot.JSBot
import jsbot.answerInlineQuery
import jsbot.toScriptable
import org.mozilla.javascript.*
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.*

/**
 * Created on 24/09/2019.
 *
 */
fun JSBot.addJSBotAPI(
    scope: Scriptable,
    jobMessage: Message? = null
) {
    this.addStuffToStringProto(scope, jobMessage)
    this.addMediaClassToScope(scope)
    this.addEventClassToScope(scope)
    this.addMessageClassToScope(scope)
    this.addUserClassToScope(scope, jobMessage)
}


fun JSBot.addStuffToStringProto(
    scope: Scriptable,
    jobMessage: Message? = null
) {
    val stringProto = ScriptableObject.getClassPrototype(scope, "String")
    if (jobMessage != null) {
        var remaining = 10
        ScriptableObject.defineProperty(stringProto, "send", JSFunction(1) { _, _, thisObj, _ ->
            if (remaining > 0) {
                execute(
                    SendMessage()
                        .setChatId(jobMessage.chatId)
                        .disableNotification()
                        .setText(Context.toString(thisObj))
                )
                remaining--
                return@JSFunction ""
            } else {
                throw JSBot.JSBotException("Message limit reached.")
            }
        }, ScriptableObject.DONTENUM)


    } else {
        ScriptableObject.defineProperty(stringProto, "send", Undefined.instance, ScriptableObject.DONTENUM)
    }


    ScriptableObject.defineProperty(
        stringProto, "findEmojis", JSFunction(0) { cx2, scope2, thisObj, _ ->
            return@JSFunction when (val argument = Context.jsToJava(thisObj, String::class.java)) {
                is String -> Emoji.findEmoji(argument).toScriptable(cx2, scope2)
                else -> null
            }
        },
        ScriptableObject.DONTENUM
    )
}


fun JSBot.addUserClassToScope(
    scope: Scriptable,
    jobMessage: Message? = null
) {
    ScriptableObject.defineClass(scope, User::class.java)

    val userProto = ScriptableObject.getClassPrototype(scope, "User")

    ScriptableObject.defineProperty(userProto, "getRole", JSFunction(0) { _, _, thisObj, _ ->

        val fromJS = when (thisObj) {
            is User -> thisObj
            else -> throw JSBot.JSBotException("Invalid user object.")
        }

        return@JSFunction userRoles[fromJS.id]?.getRoleName()
    }, ScriptableObject.DONTENUM)

    ScriptableObject.defineProperty(
        userProto, "getAbilities", JSFunction(0) { cx2, scope2, thisObj, _ ->
            val fromJS = when (thisObj) {
                is User -> thisObj
                else -> throw JSBot.JSBotException("Invalid user object.")
            }
            val abilityList = mutableListOf<Any>()
            userRoles[fromJS.id]?.getAbilites()?.forEach {
                abilityList.add(it)
            }
            return@JSFunction cx2.newArray(scope2, abilityList.toTypedArray())

        },
        ScriptableObject.DONTENUM
    )



    ScriptableObject.defineProperty(
        userProto, "getPublic", JSFunction(0) { cx2, _, thisObj, _ ->
            val fromJS = when (thisObj) {
                is User -> thisObj
                else -> throw JSBot.JSBotException("Invalid user object.")
            }
            val userScope = retrieveScope(cx2, fromJS.id.toLong())
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
        ScriptableObject.defineProperty(
            userProto, "message",
            JSFunction(1) { _, _, thisObj, args ->
                val fromJS = when (thisObj) {
                    is User -> thisObj
                    else -> throw JSBot.JSBotException("Invalid user object.")
                }
                if (!retrieveRoleFromId(jobMessage.from.id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                    throw JSBot.JSBotException("Not authorized to send private messages.")
                }

                if (args.isNotEmpty() && remaining > 0) {
                    var text = Context.toString(args[0])
                    text = if (text === null || text.isEmpty()) "_" else text
                    execute(
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
            val fromJS = when (thisObj) {
                is User -> thisObj
                else -> throw JSBot.JSBotException("Invalid user object.")
            }
            if (!retrieveRoleFromId(jobMessage.from.id).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                throw JSBot.JSBotException("Not authorized to send private messages.")
            }
            if (args.isNotEmpty() && remaining > 0) {
                val argument = args[0]
                if (argument is Scriptable) {
                    val media = Media.fromJS(argument)
                    if (media !== null) {
                        execute(
                            SendMessage()
                                .setChatId(fromJS.id.toLong())
                                .setText("From: " + (jobMessage.from.userName ?: jobMessage.from.id))
                        )
                        media.send(
                            fromJS.id.toLong(),
                            this
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


fun JSBot.addEventClassToScope(
    scope: Scriptable
) {
    ScriptableObject.defineClass(scope, Event::class.java)
}

fun JSBot.addMediaClassToScope(
    scope: Scriptable
) {
    ScriptableObject.defineClass(scope, Media::class.java)
}

fun JSBot.addMessageClassToScope(scope: Scriptable, jobMessage: Message? = null) {

    ScriptableObject.defineClass(scope, jsbot.jsapi.Message::class.java)
    val messageProto = ScriptableObject.getClassPrototype(scope, "Message")



    ScriptableObject.defineProperty(messageProto, "editText", JSFunction(1) { _, _, thisObj, args ->

        /*if(!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.TAMPER_MESSAGES_ABILITY)){
            throw JSBot.JSBotException("Not Authorized to tamper messages that have been already sent.")
        }*/
        if (args.isEmpty()) {
            throw JSBot.JSBotException("Missing argument")
        }

        if (thisObj !is jsbot.jsapi.Message) {
            throw JSBot.JSBotException("Invalid type of receiver object")
        }

        this.execute(
            EditMessageText()
                .setChatId(thisObj.chatID)
                .setMessageId(thisObj.messageID)
                .setText(Context.toString(args[0]))
        )
        return@JSFunction ""

    }, ScriptableObject.DONTENUM)







    ScriptableObject.defineProperty(messageProto, "delete", JSFunction.JSMethod<jsbot.jsapi.Message> { _, _, thisObj ->
        /*if(!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.TAMPER_MESSAGES_ABILITY)){
            throw JSBot.JSBotException("Not Authorized to tamper messages that have been already sent.")
        }*/
        return@JSMethod execute(

            DeleteMessage()
                .setChatId(thisObj.chatID)
                .setMessageId(thisObj.messageID)
        )
    }, ScriptableObject.DONTENUM)


}

fun Message?.toMedia(): Media? {
    return when {
        this == null -> null
        hasAnimation() -> Media().init(Media.ANIMATION, animation.fileId)
        hasAudio() -> Media().init(Media.AUDIO, audio.fileId)
        hasDocument() -> Media().init(Media.DOCUMENT, document.fileId)
        hasPhoto() -> Media().init(Media.PHOTO, photo[0].fileId)
        hasSticker() -> Media().init(Media.STICKER, sticker.fileId)
        hasVideo() -> Media().init(Media.VIDEO, video.fileId)
        hasVoice() -> Media().init(Media.VOICE, voice.fileId)
        else -> null
    }
}

fun Media?.send(chatID: Long, bot: JSBot, withText: String? = null) {
    if (this == null) {
        val send = SendMessage()
            .setText("null")
            .setChatId(chatID)
            .disableNotification()
        if (withText !== null) {
            send.text = withText
        }
        bot.execute(send)
        return
    }


    when (mediaType) {
        Media.ANIMATION -> bot.execute(SendAnimation().setAnimation(fileID).setChatId(chatID).disableNotification())
        Media.AUDIO -> bot.execute(SendAudio().setAudio(fileID).setChatId(chatID).disableNotification())
        Media.DOCUMENT -> bot.execute(SendDocument().setDocument(fileID).setChatId(chatID).disableNotification())
        Media.PHOTO -> bot.execute(SendPhoto().setPhoto(fileID).setChatId(chatID).disableNotification())
        Media.STICKER -> bot.execute(SendSticker().setSticker(fileID).setChatId(chatID).disableNotification())
        Media.VIDEO -> bot.execute(SendVideo().setVideo(fileID).setChatId(chatID).disableNotification())
        Media.VOICE -> bot.execute(SendVoice().setVoice(fileID).setChatId(chatID).disableNotification())
    }
}


fun Media?.giveInlineQueryResult(
    command: String,
    inlineQuery: InlineQuery,
    bot: JSBot,
    withText: String? = null
) {
    if (this == null) {
        bot.execute(
            if (withText !== null) {
                inlineQuery.answerInlineQuery(command, withText)
            } else {
                inlineQuery.answerInlineQuery(command, "null")
            }
        )
        return
    }
    val fileID = fileID

    val queryId = inlineQuery.id

    when (mediaType) {
        Media.ANIMATION -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedGif().setGifFileId(fileID).setId(queryId)
            )
        )
        Media.AUDIO -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedAudio().setAudioFileId(fileID).setId(queryId)
            )
        )
        Media.DOCUMENT -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedDocument().setDocumentFileId(fileID).setId(queryId)
            )
        )
        Media.PHOTO -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedPhoto().setPhotoFileId(fileID).setId(queryId)
            )
        )
        Media.STICKER -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedSticker().setStickerFileId(fileID).setId(queryId)
            )
        )
        Media.VIDEO -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedVideo().setVideoFileId(fileID).setId(queryId)
            )
        )
        Media.VOICE -> bot.execute(
            AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                InlineQueryResultCachedVoice().setVoiceFileId(fileID).setId(queryId)
            )
        )
    }

}



