package jsbot.jsapi

import jsbot.*
import jsbot.JSBot.Companion.defaultRole
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import org.mozilla.javascript.ScriptableObject.defineProperty
import org.mozilla.javascript.ScriptableObject.putProperty
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.*
import java.io.File
import kotlin.math.min
import org.telegram.telegrambots.meta.api.objects.Message as TgMessage

/**
 * Created on 24/09/2019.
 *
 */
fun JSBot.addJSBotAPI(
    scope: Scriptable,
    jobMessage: TgMessage? = null
) {
    this.addStuffToStringProto(scope, jobMessage)
    this.addMediaClassToScope(scope)
    this.addEventClassToScope(scope)
    this.addMessageClassToScope(scope, jobMessage)
    this.addUserClassToScope(scope, jobMessage)

    putAllDocs()

    val docsObj = Context.getCurrentContext().newObject(scope)
    docsMap.forEach { (k, v) -> putProperty(docsObj, k, v) }

    putProperty(scope, "docs", docsObj)
}

fun JSBot.putDocs(entryName: String, doc: String) {
    docsMap[entryName] = doc

}

fun JSBot.putAllDocs() {
    putDocs(
        "my",
        "<object> pointer to the sender's scope"
    )
    putDocs(
        "java",
        "<object> java packages and classes [SUPER ADMIN]"
    )
    putDocs(
        "bot",
        "<object> bot object for debug purposes [SUPER ADMIN]"
    )
    putDocs(
        "setRole",
        "<function> setRole(user, roleName) -> sets user's role to \"roleName\" [MODERATOR]"
    )
    putDocs(
        "readFileFS",
        "<function> readFileFS(fileRelativePath) -> read a file from the host file system [SUPER_ADMIN]"
    )
    putDocs(
        "getUser",
        "<function> getUser(idOrUsername) -> retrieves an user from the bot user db [MODERATOR]"
    )
    putDocs(
        "findEmoji",
        "<function> findEmoji(keyword)  -> returns an array of emojis related to keyword"
    )
    putDocs(
        "String.prototype.send",
        "<function> (string).send() -> sends this string to the current chat"
    )
    putDocs(
        "String.prototype.findEmoji",
        "<function> (string).findEmoji() -> returns an array of emojis related to this string"
    )
    putDocs(
        "sendtext",
        "<function> sendtext(obj) -> sends the string representation of obj to the current chat"
    )
    putDocs(
        "sendmd",
        "<function> sendmd(obj) -> sends the string representation of obj to the current chat, parsed as Markdown"
    )
    putDocs(
        "sendhtml",
        "<function> sendhtml(obj) -> sends the string representation of obj to the current chat, parsed as simple HTML"
    )
    putDocs(
        "sendmedia",
        "<function> sendmedia(obj) -> sends a media to the current chat if obj conforms to a Media object"
    )
    putDocs(
        "send",
        "<function> send(obj) -> if obj is a message, it will send it to the current chat; if obj conforms to a media object, it will send the media, otherwise, it behaves like sendmd"
    )
    putDocs(
        "toFile",
        "<function> toFile(obj [, filename]) -> generates a file containing the result of the .toSource() invocation on obj and uploads it to the current chat"
    )
    putDocs(
        "readFile",
        "<function> readFile(obj) ->  if obj conforms to a Media object with mediaType == DOCUMENT, the bot will download the document from the chat and return its raw content"
    )
    putDocs(
        "thatMedia",
        "<Media> contains the Media-conforming-value of the media in the message being replied, if this exists and cointains a bot-compatible media"
    )
    putDocs(
        "thatText",
        "<String> contains the text value in the message being replied"
    )
    putDocs(
        "that",
        "<Media/String> contains the value of 'thatMedia' if present, the value of 'thatText' otherwise"
    )
    putDocs(
        "me",
        "<User> User instance of the sender of the message"
    )
    putDocs(
        "thatUser",
        "<User> User instance of the sender of the message being replied"
    )
    putDocs(
        "putHandler",
        "<function> putHandler(name, handlerStringOrFunction) -> puts an handler in the current chat, returns true if a new handler has been added, false if an entry with same name has been overwritten [ADMIN]"
    )
    putDocs(
        "removeHandler",
        "<function> removeHandler(name) -> removes the handler from this chat"
    )
    putDocs(
        "getHandlers",
        "<function> getHandlers() -> returns a copy of the handlers map of this chat, for inspection purposes"
    )
    putDocs(
        "thisMessage",
        "<Message> message object representing the message that triggered this job"
    )
    putDocs(
        "thatMessage",
        "<Message> message object representing the message being replied"
    )
    putDocs(
        "User.prototype.id",
        "<integer> id of the user, always present"
    )
    putDocs(
        "User.prototype.getRole",
        "<function> getRole() -> returns the name of the role of this user (NOTE: this will be changed - it will return a copy of the Role instance of this user)"
    )
    putDocs(
        "User.prototype.getAbilities",
        "<function> getAbilities() -> returns an array containing the names of the abilites granted to this user"
    )
    putDocs(
        "User.prototype.getPublic",
        "<function> getPublic() -> returns a clone of the object in {this user's 'my' scope}.saved.public"
    )
    putDocs(
        "User.prototype.sendtext",
        "<function> sendtext(obj) -> sends the string representation of obj on this user's private chat with the bot"
    )
    putDocs(
        "User.prototype.sendmd",
        "<function> sendmd(obj) -> sends the string representation of obj on this user's private chat with the bot, parsed as Markdown"
    )
    putDocs(
        "User.prototype.sendhtml",
        "<function> sendhtml(obj) -> sends the string representation of obj on this user's private chat with the bot, parsed as simple HTML"
    )
    putDocs(
        "User.prototype.sendmedia",
        "<function> sendmedia(obj) -> sends a media to the current chat if obj conforms to a Media object, on this user's private chat with the bot"
    )
    putDocs(
        "User.prototype.send",
        "<function> send(obj) -> if obj is a message, it will send it; if obj conforms to a media object, it will send the media, otherwise, it behaves like sendmd; the message will be sent on this user's private chat with the bot"
    )
    putDocs(
        "Event.prototype.eventType",
        "<String> the kind of event represented by this object; can be 'msg_text' or 'msg_media'"
    )
    putDocs(
        "Event.prototype.content",
        "<String|Media> the content of the message that triggered this event"
    )
    putDocs(
        "Message.prototype.messageID",
        "<integer> telegram's id for the message in the chat"
    )
    putDocs(
        "Message.prototype.chatID",
        "<integer> telegram's id of the chat where this message is"
    )
    putDocs(
        "Message.prototype.mediaContent",
        "<Media> media content of this message, if present"
    )
    putDocs(
        "Message.prototype.textContent",
        "<String> text content of this message, if present"
    )
    putDocs(
        "Message.prototype.from",
        "<User> sender of the message"
    )
    putDocs(
        "Message.prototype.textFormat",
        "<integer> integer indicating the text format of this message; it is 0 if PLAINTEXT, 1 if MARKDOWN, 2 if HTML"
    )
    putDocs(
        "Message.prototype.editText",
        "<function> editText(newText) -> changes the text content of this message on the chat, if the message exists, it has not been deleted, and it is a message from the bot"
    )
    putDocs(
        "Message.prototype.delete",
        "<function> delete() -> deletes this message from the chat, if present; in private chats, it works only with messages from the bot. In group chats it works with every message, if the bot has the right permissions from the chat's settings"
    )
    putDocs(
        "Message.prototype.withMessageID",
        "<function> withMessageID(id) -> returns a copy of this message object with the specified id"
    )
    putDocs(
        "Message.prototype.withChatID",
        "<function> withChatID(id) -> returns a copy of this message object with the specified destination chat id"
    )
    putDocs(
        "Message.prototype.withMediaContent",
        "<function> withMediaContent(obj) -> returns a copy of this message object with a media content obtained by attempting to convert obj in a Media object"
    )
    putDocs(
        "Message.prototype.withTextContent",
        "<function> withTextContent(obj) -> returns a copy of this message object with the string representation of obj as text content of the message"
    )
    putDocs(
        "Message.prototype.withFrom",
        "<function> withFrom(user) -> returns a copy of this message object with the sender set to user"
    )
    putDocs(
        "Message.prototype.asMarkdown",
        "<function> asMarkdown() -> returns a copy of this message object with markdown parse mode enabled"
    )
    putDocs(
        "Message.prototype.asHtml",
        "<function> asHtml() -> returns a copy of this message object with HTML parse mode enabled"
    )
    putDocs(
        "Message.prototype.asPlaintext",
        "<function> asPlaintext() -> returns a copy of this message object with no parse mode enabled"
    )

}

fun JSBot.addUserStuff(cx: Context, to: Scriptable, userId: Int) {
    val submitterRole = retrieveRoleFromId(userId)

    putProperty(to, "my", this.scopemap[userId.toLong()] ?: retrieveScope(cx, userId.toLong()))


    putProperty(
        to,
        "java",
        if (retrieveRoleFromId(userId).isAuthorized(Role.JAVA_ABILITY)) cx.initStandardObjects() else null
    )

    putProperty(
        to, "bot",
        if (retrieveRoleFromId(userId).isAuthorized(Role.BOT_ACCESS_ABILITY)) Context.javaToJS(this, to) else null
    )


    var remainingRole = 1
    putProperty(to, "setRole", jsFun { t1: Any, t2: String ->
        if (!submitterRole.isAuthorized(Role.SET_ROLES_ABILITY)) {
            throw JSBotException("Not authorized to set role.")
        }
        if (remainingRole <= 0) {
            throw JSBotException("setRole invocation limit reached.")
        }
        val success = when (t1) {
            is String -> setRole(submitterRole, t1, t2)
            is Number -> setRole(submitterRole, t1, t2)
            is User -> setRole(submitterRole, t1, t2)
            else -> false
        }
        if (success) {
            remainingRole--
        }
        return@jsFun success
    })


    putProperty(to, "readFileFS", jsFun { arg: Any ->
        if (!submitterRole.isAuthorized(Role.LOAD_FILE_ABILITY)) {
            throw JSBotException("Not authorized to load files from server disk.")
        }
        val text = Context.toString(arg)
        if (text === null || text.isEmpty()) {
            throw JSBotException("Invalid filename")
        }
        val file = File("$workDir/$text")
        if (!file.exists() || !file.isFile) {
            throw JSBotException("File not found or not a file")
        }
        return@jsFun file.readText()
    })


    putProperty(to, "getUser", jsFun { cx2: Context, scope2: Scriptable, argument: Any ->
        if (!retrieveRoleFromId(userId).isAuthorized(Role.USER_DATABASE_READ_ABILITY)) {
            throw JSBotException("Not authorized to read users database.")
        }
        return@jsFun when (argument) {
            is String -> {
                val id = usernamesMap[argument]
                when {
                    id !== null -> User().init(id, argument).toJS(cx2, scope2)
                    else -> null
                }
            }
            is Number -> User().init(argument.toInt(), null).toJS(cx2, scope2)
            else -> null
        }
    })

    if (Emoji.isEmojiLoaded()) {

        putProperty(to, "findEmoji", jsFun { cx2, scope, argument: String ->
            return@jsFun Emoji.findEmoji(argument).toScriptable(cx2, scope)
        })
    }

}


fun JSBot.addMessageDependentStuff(cx: Context, to: Scriptable, bot: JSBot, jobMessage: TgMessage) {
    val chatID = jobMessage.chatId
    val jobRequester = User.fromTgUser(jobMessage.from)
    var remaining = 10


    putProperty(to, "sendtext", jsFun { arg: Any ->
        if (remaining > 0) {
            remaining -= Context.toString(arg).send(chatID, this, jobRequester)
        } else {
            throw JSBotException("Message limit reached.")
        }
        return@jsFun ""
    })


    putProperty(to, "sendmd", jsFun { arg: Any ->
        if (remaining > 0) {
            Message()
                .setSilent(true)
                .setTextContent(Context.toString(arg))
                .setChatID(chatID)
                .setMarkdown()
                .setFrom(jobRequester)
                .send(this)
            --remaining
        } else {
            throw JSBotException("Message limit reached.")
        }
        return@jsFun ""

    })


    putProperty(to, "sendhtml", jsFun { arg: Any ->
        if (remaining > 0) {
            Message()
                .setSilent(true)
                .setTextContent(Context.toString(arg))
                .setChatID(chatID)
                .setHTML()
                .setFrom(jobRequester)
                .send(this)
            --remaining
        } else {
            throw JSBotException("Message limit reached.")
        }
        return@jsFun ""

    })


    val sendMedia = f@{ argument: Scriptable, caption: Any? ->
        if (remaining > 0) {
            val media = Media.fromJS(argument)
            if (media !== null) {
                remaining -= media.send(
                    chatID,
                    bot,
                    from = jobRequester,
                    withText = if (caption !== null) Context.toString(caption) else null
                )
            }
        } else {
            throw JSBotException("Message limit reached.")
        }
        return@f ""
    }

    putProperty(to, "sendmedia", jsFun(
        ifOneArg = { argument: Scriptable -> sendMedia(argument, null) },
        ifTwoArgs = { argument: Scriptable, caption: Any -> sendMedia(argument, caption) }
    ))


    val submitterRole = retrieveRoleFromUser(jobMessage.from)
    putProperty(to, "send", jsFun { argument: Any ->
        if (remaining > 0) {
            if (argument is Scriptable) {
                val message: Message? = Message.fromJS(argument)
                if (message !== null) {
                    if (message.chatID != chatID) {
                        if (!submitterRole.isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                            throw JSBotException("Not athorized to send private messages")
                        }
                        if (jobMessage.from.id != message.from.id
                            && !submitterRole.isAuthorized(Role.DISGUISE_ABILITY)
                        ) {
                            throw JSBotException("Not authorized to disguise")
                        }
                    }
                    remaining -= message.send(bot, forceShowFrom = message.chatID != chatID)
                    return@jsFun ""
                } else {
                    val media = Media.fromJS(argument)
                    if (media !== null) {
                        remaining -= media.send(chatID, bot)
                        return@jsFun ""
                    }
                }
            }
            Message()
                .setSilent(true)
                .setFrom(jobRequester)
                .setTextContent(Context.toString(argument))
                .setChatID(chatID)
                .setMarkdown()
                .send(this, false)
            remaining--
            return@jsFun ""
        } else {
            throw JSBotException("Message limit reached.")
        }
    })


    putProperty(to, "toFile", JSFunction(2) { _, _, _, args ->
        if (args.isEmpty() || args.size > 2) {
            throw JSBotException("Wrong number of arguments")
        } else {
            val name = if (args.size == 1) {
                "file"
            } else when (args[1]) {
                is String -> args[1] as String
                else -> "file"
            }
            args[0].serializeToFileAndSend(name, bot, chatID, cx, to)
            return@JSFunction ""
        }
    })


    putProperty(to, "readFile", jsFun { argument: Scriptable ->
        val fromJS = Media.fromJS(argument)
        if (fromJS !== null && fromJS.mediaType == Media.DOCUMENT) {
            return@jsFun fromJS.getDocumentContents(bot)
        } else {
            throw JSBotException("Invalid argument type.")
        }
    })


    //adds the media in the message the user is referring to (or null if not present)
    val referredMedia = jobMessage.replyToMessage.toMedia()?.toJS(cx, to)
    putProperty(to, "thatMedia", referredMedia)


    //adds the text in the message the user is referring to (or null if not present)
    val referredText =
        if (jobMessage.replyToMessage !== null && jobMessage.replyToMessage.hasText() && jobMessage.replyToMessage.text !== null) {
            Context.javaToJS(jobMessage.replyToMessage.text, to)
        } else if (jobMessage.replyToMessage !== null && jobMessage.replyToMessage.caption !== null) {
            Context.javaToJS(jobMessage.replyToMessage.caption, to)
        } else {
            Context.javaToJS(null, to)
        }
    putProperty(to, "thatText", referredText)

    if (referredMedia !== null) {
        putProperty(to, "that", referredMedia)
    } else {
        putProperty(to, "that", referredText)
    }

    val referredMessage = if (jobMessage.replyToMessage !== null) {
        Message.fromTgMessage(jobMessage.replyToMessage).toJS(cx, to)
    } else {
        null
    }
    putProperty(to, "thatMessage", referredMessage)


    val thisMessage = Message.fromTgMessage(jobMessage).toJS(cx, to)
    putProperty(to, "thisMessage", thisMessage)



    putProperty(
        to,
        "me",
        User.fromTgUser(jobMessage.from).toJS(cx, to)
    )

    putProperty(
        to,
        "thatUser",
        User.fromTgUser(jobMessage.replyToMessage?.from)?.toJS(cx, to)
    )


    putProperty(to, "putHandler", jsFun { name: String, func: Function ->
        if (!submitterRole.isAuthorized(Role.CHANGE_HANDLERS_ABILITY)) {
            throw JSBotException("Not authorized to modify handlers.")
        }
        return@jsFun putHandler(chatID, name.substring(0, min(name.length, 20)), func)
    })


    putProperty(to, "removeHandler", jsFun { name: String ->
        if (!submitterRole.isAuthorized(Role.CHANGE_HANDLERS_ABILITY)) {
            throw JSBotException("Not authorized to modify handlers.")
        }
        return@jsFun removeHandler(chatID, name)
    })


    putProperty(to, "getHandlers", jsFun {
        val result = cx.newObject(to)
        val handlers = retrieveHandlers(chatID)
        handlers.forEach { (name, func) ->
            result.put(name, result, func)
        }
        return@jsFun result
    })
}

fun JSBot.addStuffToStringProto(
    scope: Scriptable,
    jobMessage: TgMessage? = null
) {
    val stringProto = ScriptableObject.getClassPrototype(scope, "String")
    var remaining = 10

    defineProperty(stringProto, "send", if (jobMessage === null) null else jsMethod { thisObj: Any ->
        if (remaining > 0) {
            remaining -= Context.toString(thisObj).send(jobMessage.chatId, this)
            return@jsMethod ""
        } else {
            throw JSBotException("Message limit reached.")
        }
    }, ScriptableObject.DONTENUM)


    defineProperty(stringProto, "findEmoji", jsMethod { thisObj: Any ->
        Emoji.findEmoji(Context.toString(thisObj)).toScriptable(Context.getCurrentContext(), scope)
    }, ScriptableObject.DONTENUM)
}

fun JSBot.addUserClassToScope(
    scope: Scriptable,
    jobMessage: TgMessage? = null
) {
    ScriptableObject.defineClass(scope, User::class.java)

    val userProto = ScriptableObject.getClassPrototype(scope, "User")

    defineProperty(userProto, "getRole", jsMethod { thisObj: Any ->

        val fromJS = when (thisObj) {
            is User -> thisObj
            else -> throw JSBotException("Invalid user object.")
        }

        return@jsMethod userRoles[fromJS.id]?.getRoleName()
    }, ScriptableObject.DONTENUM)

    defineProperty(
        userProto, "getAbilities", jsMethod { thisObj: Any ->
            val fromJS = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            val abilityList = mutableListOf<Any>()
            userRoles[fromJS.id]?.getAbilites()?.forEach {
                abilityList.add(it)
            }
            return@jsMethod Context.getCurrentContext().newArray(userProto, abilityList.toTypedArray())
        },
        ScriptableObject.DONTENUM
    )


    defineProperty(
        userProto, "getPublic", jsMethod { thisObj: Any ->
            val fromJS = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            val userScope = retrieveScope(Context.getCurrentContext(), fromJS.id.toLong())
            val saved = userScope?.get("saved", userScope)
            if (saved is Scriptable) {
                val public = saved.get("public", saved)
                if (public is Scriptable) {
                    return@jsMethod Context.getCurrentContext().evaluateString(
                        public,
                        "eval(toSource())",
                        "<clone>",
                        1,
                        null
                    )
                }
            }
            return@jsMethod null
        },
        ScriptableObject.DONTENUM
    )

    var remaining = 10
    if (jobMessage !== null) {
        val jobRequester = User.fromTgUser(jobMessage.from)
        putProperty(userProto, "sendtext", jsMethod { thisObj: Any, arg: Any ->
            val thisUsr = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            if (remaining > 0) {
                remaining -= Context.toString(arg)
                    .send(thisUsr.id.toLong(), this, from = jobRequester, forceShowFrom = true)
            } else {
                throw JSBotException("Message limit reached.")
            }
            return@jsMethod ""
        })


        putProperty(userProto, "sendmd", jsMethod { thisObj: Any, arg: Any ->
            val thisUsr = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            if (remaining > 0) {
                Message()
                    .setSilent(true)
                    .setTextContent(Context.toString(arg))
                    .setChatID(thisUsr.id)
                    .setMarkdown()
                    .setFrom(jobRequester)
                    .send(this, true)
                --remaining
            } else {
                throw JSBotException("Message limit reached.")
            }
            return@jsMethod ""

        })


        putProperty(userProto, "sendhtml", jsMethod { thisObj: Any, arg: Any ->
            val thisUsr = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            if (remaining > 0) {
                Message()
                    .setSilent(true)
                    .setTextContent(Context.toString(arg))
                    .setChatID(thisUsr.id)
                    .setHTML()
                    .setFrom(jobRequester)
                    .send(this)
                --remaining
            } else {
                throw JSBotException("Message limit reached.")
            }
            return@jsMethod ""

        })


        putProperty(userProto, "sendmedia", jsMethod { thisObj: Any, argument: Scriptable ->
            val thisUsr = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            if (remaining > 0) {
                val media = Media.fromJS(argument)
                if (media !== null) {
                    remaining -= media.send(thisUsr.id.toLong(), this, from = jobRequester)
                }
            } else {
                throw JSBotException("Message limit reached.")
            }
            return@jsMethod ""
        })


        putProperty(userProto, "send", jsMethod { thisObj:Any, argument:Any ->
            val thisUsr = when (thisObj) {
                is User -> thisObj
                else -> throw JSBotException("Invalid user object.")
            }
            if (remaining > 0) {
                if (argument is Scriptable) {
                    val message: Message? = Message.fromJS(argument)
                    if (message !== null) {
                        if (!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                            throw JSBotException("Not athorized to send private messages")
                        }
                        if (jobMessage.from.id != message.from.id &&
                            !retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.DISGUISE_ABILITY)
                        ) {
                            throw JSBotException("Not authorized to disguise")
                        }
                        remaining -= message.send(this, forceShowFrom = true)
                        return@jsMethod ""
                    }
                    val media = Media.fromJS(argument)
                    if (media !== null) {
                        remaining -= media.send(thisUsr.id.toLong(), this, from = jobRequester, forceShowFrom = true)
                        return@jsMethod ""
                    }
                }
                Message()
                    .setFrom(jobRequester)
                    .setChatID(thisUsr.id)
                    .setTextContent(Context.toString(argument))
                    .setMarkdown()
                    .setSilent(true)
                    .send(this, true)
                remaining--

                return@jsMethod ""
            } else {
                throw JSBotException("Message limit reached.")
            }
        })
    } else {
        putProperty(userProto, "sendtext", null)
        putProperty(userProto, "sendmd", null)
        putProperty(userProto, "sendhtml", null)
        putProperty(userProto, "sendmedia", null)
        putProperty(userProto, "send", null)
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

fun JSBot.addMessageClassToScope(scope: Scriptable, jobMessage: TgMessage? = null) {

    ScriptableObject.defineClass(scope, Message::class.java)
    val messageProto = ScriptableObject.getClassPrototype(scope, "Message")


    defineProperty(messageProto, "editText", jsMethod { thisObj:Any, argument:Any ->

        /*if(!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.TAMPER_MESSAGES_ABILITY)){
            throw JSBot.JSBotException("Not Authorized to tamper messages that have been already sent.")
        }*/

        if (thisObj !is Message) {
            throw JSBotException("Invalid type of receiver object")
        }

        this.execute(
            EditMessageText()
                .setChatId(thisObj.chatID)
                .setMessageId(thisObj.messageID)
                .setText(Context.toString(argument))
        )
        return@jsMethod ""

    }, ScriptableObject.DONTENUM)


    defineProperty(messageProto, "delete", jsMethod { thisObj:Any ->
        /*if(!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.TAMPER_MESSAGES_ABILITY)){
            throw JSBot.JSBotException("Not Authorized to tamper messages that have been already sent.")
        }*/
        if (thisObj !is Message) {
            throw JSBotException("Invalid type of receiver object")
        }
        return@jsMethod execute(
            DeleteMessage()
                .setChatId(thisObj.chatID)
                .setMessageId(thisObj.messageID)
        )
    }, ScriptableObject.DONTENUM)


    if (jobMessage != null) {
        var remaining = 10
        putProperty(messageProto, "send", jsMethod { thisObj:Any ->
            if (thisObj !is Message) {
                throw JSBotException("Invalid type of receiver object")
            }
            if (remaining > 0) {
                val message: Message? = Message.fromJS(thisObj)
                if (message !== null) {
                    if (message.chatID == jobMessage.chatId) {
                        remaining -= message.send(this)
                        return@jsMethod ""
                    } else {
                        if (!retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.PRIVATE_MESSAGING_ABILITY)) {
                            throw JSBotException("Not athorized to send private messages")
                        }
                        if (jobMessage.from.id != message.from.id &&
                            !retrieveRoleFromUser(jobMessage.from).isAuthorized(Role.DISGUISE_ABILITY)
                        ) {
                            throw JSBotException("Not authorized to disguise")
                        }
                        remaining -= message.send(this, forceShowFrom = true)

                        return@jsMethod ""
                    }
                } else {
                    throw JSBotException("Invalid message object.")
                }


            } else if (remaining <= 0) {
                throw JSBotException("Message limit reached.")
            }
            return@jsMethod ""
        })
    } else {
        putProperty(messageProto, "send", null)
    }

}

fun TgMessage?.toMedia(): Media? {
    return when {
        this == null -> null
        hasAnimation() -> Media().init(Media.ANIMATION, animation.fileId)
        hasAudio() -> Media().init(Media.AUDIO, audio.fileId)
        hasDocument() -> Media().init(Media.DOCUMENT, document.fileId)
        hasPhoto() -> Media().init(Media.PHOTO, photo[0].fileId)
        hasSticker() -> Media().init(Media.STICKER, sticker.fileId)
        hasVideo() -> Media().init(Media.VIDEO, video.fileId)
        hasVoice() -> Media().init(Media.VOICE, voice.fileId)
        hasVideoNote() -> Media().init(Media.VIDEO_NOTE, videoNote.fileId)
        else -> null
    }
}

fun Message.send(bot: JSBot, forceShowFrom: Boolean = false): Int {
    var creditsSpent = 0
    val fromMark = if (from?.username !== null && from.username.isNotBlank()) {
        from.username
    } else if (from?.id !== null) {
        from.id.toString()
    } else {
        if (forceShowFrom) {
            throw JSBotException("Cannot send message without sender info specified")
        }
        "null"
    }
    if (chatID == null) {
        return creditsSpent
    }
    if (mediaContent != null) {
        when (mediaContent.mediaType) {
            Media.ANIMATION -> {
                val send = SendAnimation()
                send.setAnimation(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)
                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++
            }
            Media.AUDIO -> {
                val send = SendAudio()
                send.setAudio(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)

                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++

            }
            Media.DOCUMENT -> {
                val send = SendDocument()
                send.setDocument(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)
                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++

            }
            Media.PHOTO -> {
                val send = SendPhoto()
                send.setPhoto(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)
                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++

            }
            Media.STICKER -> {
                val send = SendSticker()
                send.setSticker(mediaContent.fileID)
                send.setChatId(chatID)
                if (isSilent) {
                    send.disableNotification()
                }
                if (textContent !== null && textContent.isNotBlank()) {
                    bot.execute(
                        SendMessage().setChatId(chatID).setText(
                            ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(
                                1024,
                                false
                            )
                        ).disableNotification()
                    )
                    creditsSpent++
                }
                bot.execute(send)
                creditsSpent++
            }
            Media.VIDEO -> {
                val send = SendVideo()
                send.setVideo(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)
                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++
            }
            Media.VIDEO_NOTE -> {
                val send = SendVideoNote()
                send.setVideoNote(mediaContent.fileID)
                send.setChatId(chatID)
                if (isSilent) {
                    send.disableNotification()
                }
                if (textContent !== null && textContent.isNotBlank()) {
                    bot.execute(
                        SendMessage().setChatId(chatID).setText(
                            ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(
                                1024,
                                false
                            )
                        ).disableNotification()
                    )
                    creditsSpent++
                }
                bot.execute(send)
                creditsSpent++
            }
            Media.VOICE -> {
                val send = SendVoice()
                send.setVoice(mediaContent.fileID)
                send.setChatId(chatID)
                if (textContent !== null && textContent.isNotBlank()) {
                    send.caption =
                        ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage(1024, false)
                }
                if (isSilent) {
                    send.disableNotification()
                }
                bot.execute(send)
                creditsSpent++
            }
        }
    } else if (textContent !== null && textContent.isNotBlank()) {
        val send = SendMessage()
        send.setChatId(chatID)
        send.text = ((if (forceShowFrom) "from: $fromMark\n\n" else "") + textContent).clipForMessage()
        if (isSilent) {
            send.disableNotification()
        }
        send.enableMarkdown(isMarkdown)
        send.enableHtml(isHTML)
        bot.execute(send)
        creditsSpent++
    }
    return creditsSpent
}


fun String?.send(chatID: Long, bot: JSBot, from: User? = null, forceShowFrom: Boolean = false): Int {
    return Message()
        .setSilent(true)
        .setTextContent(this)
        .setChatID(chatID)
        .setPlaintext()
        .setFrom(from)
        .send(bot, forceShowFrom)
}

fun Media?.send(
    chatID: Long,
    bot: JSBot,
    withText: String? = null,
    from: User? = null,
    forceShowFrom: Boolean = false
): Int {
    if (this == null) {
        return (withText ?: "null").send(chatID, bot, from, forceShowFrom)

    }

    return Message()
        .setSilent(true)
        .setMediaContent(this)
        .setChatID(chatID)
        .setTextContent(withText)
        .setFrom(from)
        .send(bot, forceShowFrom)
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

private fun JSBot.setRole(
    submitterRole: Role,
    argUser: User,
    argumentRole: String
): Boolean {
    val createdRole = Role.create(argumentRole)
    if (createdRole === null) {
        return false
    }

    if (argUser.id == creatorId) {
        throw JSBotException("Creator's role is immutable.")
    }

    if (submitterRole.isChangeRoleAuthorized(
            userRoles[argUser.id] ?: Role.create(defaultRole)!!,
            createdRole
        )
    ) {
        userRoles[argUser.id] = createdRole
        saveUserRoles()
        return true
    } else {
        throw JSBotException("Unsufficient privileges.")
    }
}

private fun JSBot.setRole(
    submitterRole: Role,
    argUser: String,
    argumentRole: String
): Boolean {
    val createdRole = Role.create(argumentRole)
    if (createdRole === null) {
        return false
    }

    if (argUser == creator) {
        throw JSBotException("Creator's role is immutable.")
    }
    val id = usernamesMap[argUser]
    if (id !== null) {

        if (submitterRole.isChangeRoleAuthorized(
                userRoles[id] ?: Role.create(defaultRole)!!,
                createdRole
            )
        ) {
            userRoles[id] = createdRole
            saveUserRoles()
            return true
        } else {
            throw JSBotException("Unsufficient privileges.")
        }
    }
    throw JSBotException("Illegal target user argument.")
}

private fun JSBot.setRole(
    submitterRole: Role,
    argUser: Number,
    argumentRole: String
): Boolean {
    val createdRole = Role.create(argumentRole)

    if (createdRole === null) {
        return false
    }

    if (argUser.toInt() == creatorId) {
        throw JSBotException("Creator's role is immutable.")
    }

    if (submitterRole.isChangeRoleAuthorized(
            userRoles[argUser.toInt()] ?: Role.create(defaultRole)!!,
            createdRole
        )
    ) {
        userRoles[argUser.toInt()] = createdRole
        saveUserRoles()
        return true
    } else {
        throw JSBotException("Unsufficient privileges.")
    }
}

