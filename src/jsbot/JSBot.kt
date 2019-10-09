package jsbot

import jsbot.jsapi.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import java.io.*


class JSBot(
    val workDir: String,
    val creator: String,
    val creatorId: Int,
    val username: String,
    val token: String
) : TelegramLongPollingBot() {

    val docsMap: MutableMap<String, String> = mutableMapOf()

    val handlersMap: MutableMap<Long, MutableMap<String, Function>> = mutableMapOf()
    // maps chat IDs to the scope reserved to such chats
    val scopemap: MutableMap<Long, Scriptable> = mutableMapOf()
    // maps usernames their user IDs
    val usernamesMap: MutableMap<String, Int> = mutableMapOf()
    // maps user IDs to their Role
    val userRoles: MutableMap<Int, Role> = mutableMapOf()


    init {
        usernamesMap[creator] = creatorId
        userRoles[creatorId] = Role.Companion.SuperAdmin()
    }

    companion object {
        val logger: Logger = LogManager.getRootLogger()
        const val defaultRole = Role.NOT_AUTHORIZED_ROLE
    }

    override fun getBotUsername(): String = username

    override fun getBotToken() = token

    override fun onUpdateReceived(update: Update?) {
        withContext wc@{
            try {
                logger.debug("=".repeat(80))
                logger.debug("=".repeat(80))
                logger.debug("=".repeat(80))
                update!!
                if (update.hasMessage()) {
                    val message = update.message!!


                    logger.debug("From ${message.from?.id}:${message.from?.userName}@${message.chatId} --------------")
                    logUserInfo(message.from)
                    if (message.isUserMessage && message.chatId !== null
                        && !retrieveRoleFromId(message.chatId.toInt()).isAuthorized(Role.PRIVATE_USE_BOT_ABILITY)
                    ) {
                        logger.debug("Not authorized: " + Role.PRIVATE_USE_BOT_ABILITY)
                        return@wc
                    }

                    if ((message.isGroupMessage || message.isSuperGroupMessage)) {
                        val role = retrieveRoleFromUser(message.from)

                        if (!role.isAuthorized(Role.GROUP_USE_BOT_ABILITY)) {
                            logger.debug("Not authorized: " + Role.GROUP_USE_BOT_ABILITY)
                            return@wc
                        }
                    }


                    if (message.from?.userName !== null) {
                        usernamesMap[message.from.userName] = message.from.id
                    }

                    if (message.hasText() && message.text !== null) {
                        var text = message.text
                        if (text.isNotEmpty()) {
                            logger.debug("Text: '$text'")

                            val scope = retrieveScope(it, message.chatId, message)!!


                            val isNoev = text.startsWith("NOEV ")

                            var blockEvaluation = false
                            if (!isNoev) {
                                blockEvaluation = handleEventInTime(
                                    scope,
                                    Event().init(Event.TEXT_MESSAGE_EVENT_TYPE, text),
                                    message,
                                    3
                                )
                            } else {
                                text = text.substring(5)
                            }

                            if (!blockEvaluation) {
                                doInTime(scope, text, message, 3)
                            }


                            val saved = scope.get("saved", scope)
                            if (saved != Scriptable.NOT_FOUND
                                && saved != Context.getUndefinedValue()
                                && saved is ScriptableObject
                            ) {
                                saveScope(message.chatId, it, saved)
                            }

                            if (!message.isUserMessage) {
                                val userSaved = retrieveScope(it, message.from.id.toLong())?.get("saved", scope)
                                if (userSaved !== null
                                    && userSaved != Scriptable.NOT_FOUND
                                    && userSaved != Context.getUndefinedValue()
                                    && userSaved is ScriptableObject
                                ) {
                                    saveScope(message.from.id.toLong(), it, userSaved)
                                }
                            }
                        }
                    } else if (message.hasMedia()) {

                        val m = message.toMedia()!!
                        logger.debug("Media: { mediaType:\"${m.mediaType}\", fileID:\"${m.fileID}\" }")

                        val scope = retrieveScope(it, message.chatId, message)!!


                        handleEventInTime(
                            scope,
                            Event().init(Event.MEDIA_MESSAGE_EVENT_TYPE, m),
                            message,
                            3
                        )

                        val saved = scope.get("saved", scope)
                        if (saved != Scriptable.NOT_FOUND
                            && saved != Context.getUndefinedValue()
                            && saved is ScriptableObject
                        ) {
                            saveScope(message.chatId, it, saved)
                        }

                        if (!message.isUserMessage) {
                            val userSaved = retrieveScope(it, message.from.id.toLong())?.get("saved", scope)
                            if (userSaved !== null
                                && userSaved != Scriptable.NOT_FOUND
                                && userSaved != Context.getUndefinedValue()
                                && userSaved is ScriptableObject
                            ) {
                                saveScope(message.from.id.toLong(), it, userSaved)
                            }
                        }


                    }
                    saveUsernameMap()
                    saveUserRoles()
                } else if (update.hasInlineQuery()) {
                    val inlineQuery = update.inlineQuery!!

                    val from = inlineQuery.from!!

                    if (from.userName !== null) {
                        usernamesMap[from.userName] = from.id
                    }

                    logger.debug("Query from ${from.id}:${from.userName} --------------")

                    logUserInfo(from)

                    if (!retrieveRoleFromId(inlineQuery.from.id.toInt()).isAuthorized(Role.PRIVATE_USE_BOT_ABILITY)) {
                        logger.debug("Not authorized: " + Role.PRIVATE_USE_BOT_ABILITY)
                        return@wc
                    }


                    if (inlineQuery.hasQuery() && inlineQuery.query !== null) {
                        val text = inlineQuery.query
                        if (text.isNotEmpty()) {
                            logger.debug("Text: '$text'")

                            val scope = retrieveScope(it, from.id.toLong(), null)!!

                            doQueryInTime(scope, text, inlineQuery, 3)

                            val saved = scope.get("saved", scope)
                            if (saved != Scriptable.NOT_FOUND
                                && saved != Context.getUndefinedValue()
                                && saved is ScriptableObject
                            ) {
                                saveScope(from.id.toLong(), it, saved)
                            }


                        }
                    }

                    saveUsernameMap()
                    saveUserRoles()

                } else if (update.hasEditedMessage()) {

                }
            } catch (e: KotlinNullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun logUserInfo(from: User?) {
        logger.debug("User info:")
        if (from !== null) {
            logger.debug("\tisBot = ${from.bot}")
            logger.debug("\tID    = ${from.id}")
            logger.debug("\tuName = ${from.userName}")
            logger.debug("\tfName = ${from.firstName}")
            logger.debug("\tlName = ${from.lastName}")
            logger.debug("\tlang  = ${from.languageCode}")
        }
    }

    private fun handleEventInTime(
        scope: Scriptable,
        event: Event,
        message: Message,
        seconds: Long
    ): Boolean {
        var shouldBlockEvaluation = false
        val executor = Executors.newSingleThreadExecutor()
        logger.debug("Event handlers - Started Job...")

        val invokeAll = executor.invokeAll(
            mutableListOf(Callable {
                withContext { it2 ->
                    try {
                        val jsEvent = event.toJS(it2, scope)
                        val handlers = retrieveHandlers(message.chatId)
                        handlers.forEach { (key, func) ->
                            try {
                                logger.debug("executing handler: $key")
                                val result = func.call(it2, scope, scope, arrayOf(jsEvent))

                                if (Context.toBoolean(result)) {
                                    logger.debug("An event handler function returned true -> blocking evaluation")
                                    shouldBlockEvaluation = true
                                }

                            } catch (e: RhinoException) {
                                logger.error(e)
                            } catch (e: JSBotException) {
                                logger.error(e)
                            }
                        }
                    } catch (e: RhinoException) {
                        logger.error(e)
                    } catch (e: JSBotException) {
                        logger.error(e)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    logger.debug("Ended Job.")
                }
            }),
            seconds, TimeUnit.SECONDS
        )

        if (invokeAll[0].isCancelled) {
            logger.debug("Cancelled Job.")
        }
        executor.shutdown()
        return shouldBlockEvaluation
    }


    private fun doQueryInTime(
        scope: Scriptable,
        text: String,
        inlineQuery: InlineQuery,
        seconds: Long
    ) {
        val executor = Executors.newSingleThreadExecutor()
        logger.debug("Started Job...")

        val invokeAll = executor.invokeAll(
            mutableListOf(Callable {
                withContext { it2 ->
                    try {

                        val result: Any? = try {
                            it2.evaluateString(scope, text, "<cmd>", 1, null)
                        } catch (e: Throwable) {
                            logger.error(e)
                            e.message
                        }


                        if (result is Scriptable) {
                            val media = Media.fromJS(result)
                            if (media !== null) {
                                media.giveInlineQueryResult(text, inlineQuery, this)
                            } else {
                                inlineQueryTextResult(text, result, inlineQuery)
                            }
                        } else {
                            inlineQueryTextResult(text, result, inlineQuery)
                        }


                    } catch (e: RhinoException) {
                        logger.error(e)
                    } catch (e: JSBotException) {
                        logger.error(e)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    logger.debug("Ended Job.")
                }
            }),
            seconds, TimeUnit.SECONDS
        )

        if (invokeAll[0].isCancelled) {
            logger.debug("Cancelled Job.")
        }
        executor.shutdown()
    }


    private fun doInTime(
        scope: Scriptable,
        text: String,
        message: Message,
        seconds: Long
    ) {
        val executor = Executors.newSingleThreadExecutor()
        logger.debug("Started Job...")

        val invokeAll = executor.invokeAll(
            mutableListOf(Callable {
                withContext { it2 ->

                    if (text.startsWith("SEPPUKU")) {
                        if (retrieveRoleFromUser(message.from).isAuthorized(Role.PANIC_ABILITY)) {

                            execute(SendMessage(message.chatId, "cout << \"Sto morendo!\";"))

                            exitProcess(2) //HARAKIRIIIII

                        }
                    }

                    try {
                        if (text == "JS!"
                            && message.replyToMessage !== null
                            && message.replyToMessage.hasText()
                            && message.replyToMessage.text !== null
                            && message.from.id == message.replyToMessage.from.id
                        ) {
                            val result: Any? = try {
                                var command = message.replyToMessage.text
                                if (command.startsWith("JS ")) {
                                    command = command.substring(3)
                                }
                                it2.evaluateString(scope, command, "<cmd>", 1, null)
                            } catch (e: Throwable) {
                                logger.error(e)
                                e.message
                            }

                            if (result is Scriptable) {
                                val media = Media.fromJS(result)
                                if (media !== null) {
                                    replyWithSimpleMedia(media, message)
                                } else {
                                    replyWithText(result, message, true)
                                }
                            } else {
                                replyWithText(result, message, true)
                            }
                        } else {

                            val showError = text.startsWith("JS ")
                            var command = text

                            val result: Any? = if (showError) {
                                try {
                                    command = text.substring(3)
                                    it2.evaluateString(scope, command, "<cmd>", 1, null)
                                } catch (e: Throwable) {
                                    logger.error(e)
                                    e.message
                                }
                            } else {
                                it2.evaluateString(scope, command, "<cmd>", 1, null)
                            }

                            if (result is Scriptable) {
                                val media = Media.fromJS(result)
                                if (media !== null) {
                                    replyWithSimpleMedia(media, message)
                                } else {
                                    replyWithText(result, message, showError)
                                }
                            } else {
                                replyWithText(result, message, showError)
                            }

                        }
                    } catch (e: RhinoException) {
                        logger.error(e)
                    } catch (e: JSBotException) {
                        logger.error(e)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                    logger.debug("Ended Job.")
                }
            }),
            seconds, TimeUnit.SECONDS
        )

        if (invokeAll[0].isCancelled) {
            logger.debug("Cancelled Job.")
        }
        executor.shutdown()
    }


    private fun saveUsernameMap() {
        logger.debug("save usernames, size:${usernamesMap.size}")
        val fileout = File("$workDir/userchats.jsbot")
        fileout.bufferedWriter().use {
            usernamesMap.forEach { (userName, userId) ->
                it.append("$userName $userId")
                it.newLine()
            }
        }
    }


    fun loadUsernames() {
        val filein = File("$workDir/userchats.jsbot")
        if (filein.exists() && filein.isFile) {
            filein.bufferedReader().useLines {
                it.forEach { line ->
                    val split = line.split(" ")
                    val userName = split[0]
                    val userId = split[1]
                    usernamesMap[userName] = Integer.parseInt(userId)
                }
            }
        }
        logger.debug("loaded usernames, size:${usernamesMap.size}")
        usernamesMap.forEach { (name, id) ->
            logger.debug("$name:$id")
        }
    }


    fun saveUserRoles() {
        logger.debug("save user roles, size:${userRoles.size}")
        val fileout = File("$workDir/userroles.jsbot")
        fileout.bufferedWriter().use {
            userRoles.forEach { (userId, role) ->
                it.append("$userId ${role.getRoleName()}")
                it.newLine()
            }
        }
    }


    fun loadUserRoles() {
        val filein = File("$workDir/userroles.jsbot")
        if (filein.exists() && filein.isFile) {
            filein.bufferedReader().useLines {
                it.forEach { line ->
                    val split = line.split(" ")
                    val userId = split[0]
                    val role = split[1]
                    userRoles[Integer.parseInt(userId)] = Role.genRole(role, defaultRole)
                }
            }
        }
        logger.debug("loaded user roles, size:${userRoles.size}")
        userRoles.forEach { (name, role) ->
            logger.debug("$name:${role.getRoleName()}")
        }
    }


    fun retrieveRoleFromUserName(username: String): Role? {
        val userid = usernamesMap[username]
        return if (userid !== null) {
            retrieveRoleFromId(userid)
        } else {
            null
        }
    }


    fun retrieveRoleFromId(userid: Int): Role {
        var role = userRoles[userid]
        if (role === null) {
            role = Role.create(defaultRole)!!
            userRoles[userid] = role
        }
        return role
    }


    fun retrieveRoleFromUser(user: User): Role {
        val fromUN = user.userName
        val fromID = user.id
        return if (fromUN !== null && usernamesMap.containsKey(fromUN)) {
            retrieveRoleFromUserName(fromUN)!!
        } else if (fromID !== null && fromID > 0) {
            retrieveRoleFromId(fromID)
        } else {
            Role.create(defaultRole)!!
        }
    }


    private fun inlineQueryTextResult(command: String, result: Any?, inlineQuery: InlineQuery) {
        var textResult = Context.toString(result)
        textResult = if (textResult === null) "<null text>" else textResult
        if (textResult.isNullOrBlank()) {
            textResult = "<empty string>"
        }


        execute(
            AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.id)
                .setResults(
                    InlineQueryResultArticle()
                        .setId(inlineQuery.id)
                        .setTitle(command)
                        .setDescription(textResult)
                        .setInputMessageContent(
                            InputTextMessageContent()
                                .setMessageText(textResult)
                        ),
                    InlineQueryResultArticle()
                        .setId(inlineQuery.id + "WCOMM")
                        .setTitle("Show command too")
                        .setDescription(textResult)
                        .setInputMessageContent(
                            InputTextMessageContent()
                                .setMessageText("$command ->\n$textResult")
                        )
                )
        )

    }


    private fun replyWithText(result: Any?, message: Message, replyEmptyString: Boolean = false) {
        var textResult = Context.toString(result)

        textResult = textResult.clipForMessage(replyEmptyString = replyEmptyString)

        logger.debug("Sending reply with size ${textResult.length}")


        if (textResult.isNotBlank()) {
            execute(
                SendMessage()
                    .setChatId(message.chatId)
                    .setText(textResult)
                    .setReplyToMessageId(message.messageId)
                    .disableNotification()
            )
        }
    }




    private fun replyWithSimpleMedia(media: Media?, message: Message) {
        media.send(message.chatId, this)
    }


    fun retrieveScope(cx: Context, scopeID: Long, message: Message? = null): Scriptable? {

        val firstAccess: Boolean
        logger.debug("retrieving scope :$scopeID")
        val result = if (scopemap.containsKey(scopeID)) {
            firstAccess = false
            scopemap[scopeID]
        } else {
            val scope = cx.initSafeStandardObjects()
            scopemap[scopeID] = scope
            firstAccess = true
            scope
        }

        if (result != null) {

            //add JSBot data Classes to scope
            this.addJSBotAPI(result, message)

            if (message !== null) {
                logger.debug("retrieving scope :$scopeID ---: adding message-dependent properties")
                addMessageDependentStuff(cx, result, this, message)
                logger.debug("retrieving scope :$scopeID ---: adding user-dependent properties, user: ${message.from.id}")
                addUserStuff(cx, result, message.from.id)
            } else {
                if (scopeID > 0) {
                    logger.debug("retrieving scope :$scopeID ---: adding user-dependent properties (user chat)")
                    addUserStuff(cx, result, scopeID.toInt())
                }
            }


            val file = File(scopeFileName(scopeID))
            if (file.exists() && file.isFile) {
                val loadedSavedObject = loadScope(scopeID, cx, cx.newObject(result))
                ScriptableObject.defineProperty(
                    result,
                    "saved",
                    loadedSavedObject,
                    ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                )

                if (firstAccess && loadedSavedObject is Scriptable && loadedSavedObject != Undefined.instance) {
                    when (val onBoot = loadedSavedObject.get("onBoot", loadedSavedObject)) {
                        is Function -> {
                            try {
                                val onBootResult = onBoot.call(cx, result, result, arrayOf(loadedSavedObject))
                                execute(SendMessage().setText(Context.toString(onBootResult)).setChatId(scopeID).disableNotification())
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                        is String -> try {
                            execute(SendMessage().setText(onBoot).setChatId(scopeID).disableNotification())
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    }
                }
            }


            if (!ScriptableObject.hasProperty(result, "saved")) {
                ScriptableObject.defineProperty(
                    result,
                    "saved",
                    cx.newObject(result),
                    ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                )
            }
        }
        return result
    }


    fun retrieveHandlers(scopeID: Long): MutableMap<String, Function> {
        var handlers = handlersMap[scopeID]
        return if (handlers != null) {
            handlers
        } else {
            handlers = mutableMapOf()
            handlersMap[scopeID] = handlers
            handlers
        }
    }


    fun putHandler(scopeID: Long, name: String, func: Function): Boolean {
        val handlers = retrieveHandlers(scopeID)
        val result = !handlers.containsKey(name)
        handlers[name] = func
        return result
    }


    fun removeHandler(scopeID: Long, name: String): Boolean {
        val handlers = retrieveHandlers(scopeID)
        val result = handlers.containsKey(name)
        handlers.remove(name)
        return result
    }


    private fun scopeFileName(chatId: Long) = "$workDir/scope$chatId.js"


    private fun saveScope(chatId: Long, context: Context, saved: Scriptable) {
        val filename = scopeFileName(chatId)
        logger.debug("Saving Scope $filename")

        PrintWriter(FileOutputStream(filename)).use {
            val source = saved.serialize(context)
            it.println(source)
        }
    }


    private fun loadScope(chatId: Long, context: Context, saved: Scriptable): Any {
        val filename = scopeFileName(chatId)
        val file = File(filename)
        if (file.exists() && file.isFile) {
            val fis = FileInputStream(file)
            fis.bufferedReader().use {
                val result = context.evaluateReader(saved, it, "<load_$chatId>", 1, null)
                if (result == Scriptable.NOT_FOUND || result === null) {
                    return Context.getUndefinedValue()
                }
                return result
            }
        }

        return Context.getUndefinedValue()

    }

    /*
Each thread must have its Rhino context entered; this is used as
construct for portions of code with specific contexts.
 */
    private fun withContext(x: (ctx: Context) -> Unit) {
        val ctx = Context.enter()
        ctx.languageVersion = Context.VERSION_ES6
        ctx.optimizationLevel = -1
        try {
            x.invoke(ctx)
        } finally {
            Context.exit()
        }
    }


}






