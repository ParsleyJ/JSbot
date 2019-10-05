package jsbot

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.json.JSONObject
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
import kotlin.math.min



class JSBot(
    public val creator: String,
    public val creatorId: Int,
    public val scopemap: MutableMap<Long, Scriptable>,// maps chat IDs to the scope reserved to such chats
    public val usernamesMap: MutableMap<String, Int>, // maps usernames their user IDs
    public val userRoles: MutableMap<Int, Role>,      // maps user IDs to their Role
    private val username: String,
    private val botToken: String
) : TelegramLongPollingBot() {

    public val handlersMap: MutableMap<Long, MutableMap<String, Function>> = mutableMapOf()

    companion object {
        val logger: Logger = LogManager.getRootLogger()
        const val defaultRole = Role.NOT_AUTHORIZED_ROLE
    }

    override fun getBotUsername(): String = username
    override fun getBotToken(): String = botToken


    override fun onUpdateReceived(update: Update?) {
        withContext wc@{
            try {
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


                    if (message.isUserMessage && message.from.userName !== null) {
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
                                    Event(Event.TEXT_MESSAGE_EVENT_TYPE, text),
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

                            saveUsernameMap()
                            saveUserRoles()
                        }
                    } else if (SimpleMedia.hasMedia(message)) {

                        val m = SimpleMedia.fromMessage(message)!!
                        logger.debug("Media: { mediaType:\"${m.mediaType}\", fileID:\"${m.fileID}\" }")

                        val scope = retrieveScope(it, message.chatId, message)!!


                        handleEventInTime(
                            scope,
                            Event(Event.MEDIA_MESSAGE_EVENT_TYPE, m),
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
                } else if (update.hasInlineQuery()) {
                    val inlineQuery = update.inlineQuery!!

                    val from = inlineQuery.from!!

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

                            saveUsernameMap()
                            saveUserRoles()
                        }
                    }

                }else if(update.hasEditedMessage()){
                    
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
                            val media = SimpleMedia.fromJS(result)
                            if (media !== null) {
                                SimpleMedia.giveInlineQueryResult(text, media, inlineQuery, this)
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
                                val media = SimpleMedia.fromJS(result)
                                if (media !== null) {
                                    replyWithSimpleMedia(media, message)
                                } else {
                                    replyWithText(result, message)
                                }
                            } else {
                                replyWithText(result, message)
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
                                val media = SimpleMedia.fromJS(result)
                                if (media !== null) {
                                    replyWithSimpleMedia(media, message)
                                } else {
                                    replyWithText(result, message)
                                }
                            } else {
                                replyWithText(result, message)
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
        val fileout = File("userchats.jsbot")
        fileout.bufferedWriter().use {
            usernamesMap.forEach { (userName, userId) ->
                it.append("$userName $userId")
                it.newLine()
            }
        }
    }


    fun loadUsernames() {
        val filein = File("userchats.jsbot")
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


    private fun saveUserRoles() {
        logger.debug("save user roles, size:${userRoles.size}")
        val fileout = File("userroles.jsbot")
        fileout.bufferedWriter().use {
            userRoles.forEach { (userId, role) ->
                it.append("$userId ${role.getRoleName()}")
                it.newLine()
            }
        }
    }


    fun loadUserRoles() {
        val filein = File("userroles.jsbot")
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


    private fun replyWithText(result: Any?, message: Message) {
        var textResult = Context.toString(result)
        textResult = if (textResult === null) "" else textResult
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


    private fun replyWithSimpleMedia(media: SimpleMedia?, message: Message) {
        SimpleMedia.send(media, message.chatId, this)
    }


    fun retrieveScope(cx: Context, scopeID: Long, message: Message? = null): Scriptable? {

        logger.debug("retrieving scope :$scopeID")
        val result = if (scopemap.containsKey(scopeID)) {
            scopemap[scopeID]
        } else {
            val scope = cx.initSafeStandardObjects()

            val file = File(scopeFileName(scopeID))
            if (file.exists() && file.isFile) {
                val loadedScope = loadScope(scopeID, cx, cx.newObject(scope))
                ScriptableObject.defineProperty(
                    scope,
                    "saved",
                    loadedScope,
                    ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                )

                if (loadedScope is Scriptable && loadedScope != Undefined.instance) {
                    when (val onBoot = loadedScope.get("onBoot", loadedScope)) {
                        is Function -> {
                            try {
                                val onBootResult = onBoot.call(cx, scope, scope, arrayOf(loadedScope))
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
                //TODO: invoke autoload?
            } else {
                ScriptableObject.defineProperty(
                    scope,
                    "saved",
                    cx.newObject(scope),
                    ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                )
            }

            scopemap[scopeID] = scope
            scope
        }

        if (result != null) {

            if (message !== null) {
                logger.debug("retrieving scope :$scopeID ---: adding message-dependent properties")
                addMessageDependentStuff(cx, result, this, message)
                logger.debug("retrieving scope :$scopeID ---: adding user-dependent properties, user: ${message.from.id}")
                addUserStuff(cx, result, message.from.id, message)
            } else {
                if (scopeID > 0) {
                    logger.debug("retrieving scope :$scopeID ---: adding user-dependent properties (user chat)")
                    addUserStuff(cx, result, scopeID.toInt())
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


    private fun addUserStuff(cx: Context, to: Scriptable, userId: Int, jobMessage: Message? = null) {

        // adds the "my" dynamic reference
        ScriptableObject.putProperty(to, "my", scopemap[userId.toLong()] ?: retrieveScope(cx, userId.toLong()))

        // gives user acces to its own role name
        val retrievedRole = retrieveRoleFromId(userId)
        ScriptableObject.putProperty(to, "role", retrievedRole.getRoleName())


        // adds "java" classpath for authorized people
        if (retrieveRoleFromId(userId).isAuthorized(Role.JAVA_ABILITY)) {
            ScriptableObject.putProperty(to, "java", cx.initStandardObjects())
        } else {
            ScriptableObject.putProperty(to, "java", null)
        }


        // adds access to this bot object
        if (retrieveRoleFromId(userId).isAuthorized(Role.BOT_ACCESS_ABILITY)) {
            ScriptableObject.putProperty(to, "bot", Context.javaToJS(this, to))
        } else {
            ScriptableObject.putProperty(to, "bot", null)
        }

        // adds ability to set other people's roles
        var remainingRole = 1
        ScriptableObject.putProperty(to, "setRole", JSFunction(2) f@{ _, _, _, args ->
            if (!retrievedRole.isAuthorized(Role.SET_ROLES_ABILITY)) {
                throw JSBotException("Not authorized to set role.")
            }

            if (args.size >= 2 && remainingRole > 0) {
                val argumentRole = args[1]
                if (argumentRole is String) {
                    val createdRole = Role.create(argumentRole)

                    if (createdRole === null) {
                        return@f false
                    }

                    val argUser = args[0]
                    if (argUser is String) {
                        if (argUser == creator) {
                            throw JSBotException("Creator's role is immutable.")
                        }
                        val id = usernamesMap[argUser]
                        if (id !== null) {
                            remainingRole--
                            if (retrievedRole.isChangeRoleAuthorized(
                                    userRoles[id] ?: Role.create(defaultRole)!!,
                                    createdRole
                                )
                            ) {
                                userRoles[id] = createdRole
                                saveUserRoles()
                                return@f true
                            } else {
                                throw JSBotException("Unsufficient privileges.")
                            }
                        }

                        throw JSBotException("Illegal target user argument.")
                    } else if (argUser is Int) {
                        if (argUser == creatorId) {
                            throw JSBotException("Creator's role is immutable.")
                        }
                        remainingRole--
                        if (retrievedRole.isChangeRoleAuthorized(
                                userRoles[argUser.toInt()] ?: Role.create(defaultRole)!!,
                                createdRole
                            )
                        ) {
                            userRoles[argUser.toInt()] = createdRole
                            saveUserRoles()
                            return@f true
                        } else {
                            throw JSBotException("Unsufficient privileges.")
                        }
                    } else if (argUser is Scriptable) {
                        val fromJS = jsbot.User.fromJS(argUser)
                        if (fromJS !== null) {
                            if (fromJS.id == creatorId) {
                                throw JSBotException("Creator's role is immutable.")
                            }
                            if (retrievedRole.isChangeRoleAuthorized(
                                    userRoles[fromJS.id] ?: Role.create(defaultRole)!!,
                                    createdRole
                                )
                            ) {
                                userRoles[fromJS.id] = createdRole
                                saveUserRoles()
                                return@f true
                            } else {
                                throw JSBotException("Unsufficient privileges.")
                            }
                        } else {
                            throw JSBotException("Illegal target user argument.")
                        }
                    } else {
                        throw JSBotException("Illegal target user argument.")
                    }
                } else {
                    throw JSBotException("Illegal role argument.")
                }


            } else if (remainingRole <= 0) {
                throw JSBotException("Operation limit reached.")
            }
            return@f false
        })

        // adds a vector of the user's own ability names
        val abilityList = mutableListOf<Any>()
        retrievedRole.getAbilites().forEach {
            abilityList.add(it)
        }
        ScriptableObject.putProperty(to, "abilities", cx.newArray(to, abilityList.toTypedArray()))

        // allows an user (if authorized) to read from the host's FS
        ScriptableObject.putProperty(to, "readFileFS", JSFunction(1) f@{ _, _, _, args ->
            if (!retrieveRoleFromId(userId).isAuthorized(Role.LOAD_FILE_ABILITY)) {
                throw JSBotException("Not authorized to load files from server disk.")
            }
            if (args.isNotEmpty()) {
                val text = Context.toString(args[0])
                if (text === null || text.isEmpty()) {
                    throw JSBotException("Invalid filename")
                }
                val file = File(text)
                if (!file.exists() || !file.isFile) {
                    throw JSBotException("File not found or not a file")
                }

                return@f file.readText()
            } else {
                throw JSBotException("Missing argument")
            }

        })

        // allows an user (if authorized) to get the user object of any user in bot's db
        val bot = this
        ScriptableObject.putProperty(to, "getUser", JSFunction(1) f@{ cx2, _, _, args ->
            if (!retrieveRoleFromId(userId).isAuthorized(Role.USER_DATABASE_READ_ABILITY)) {
                throw JSBotException("Not authorized to read users database.")
            }
            if (args.isNotEmpty()) {
                val argument = args[0]
                return@f when (argument) {
                    is String -> {
                        val id = usernamesMap[argument]
                        when {
                            id !== null -> jsbot.User(id, argument).toJS(cx2, to, bot, jobMessage)
                            else -> null
                        }
                    }
                    is Int -> jsbot.User(argument).toJS(cx2, to, bot, jobMessage)
                    else -> null
                }
            } else {
                throw JSBotException("Missing argument")
            }
        })

        if (Emoji.isEmojiLoaded()) {
            ScriptableObject.putProperty(to, "findEmoji", JSFunction(1) f@{ cx2, scope, _, args ->
                if (args.isNotEmpty()) {
                    return@f when (val argument = args[0]) {
                        is String -> Emoji.findEmoji(argument).toScriptable(cx2, scope)
                        else -> null
                    }
                } else {
                    throw JSBotException("Missing argument")
                }
            })
        }
    }


    private fun addMessageDependentStuff(cx: Context, to: Scriptable, bot: JSBot, message: Message) {

        val chatID = message.chatId

        var remaining = 10

        //adds the message function to the scope
        //defines native "message" function
        ScriptableObject.putProperty(to, "message", JSFunction(1) f@{ _, _, _, args ->
            if (args.isNotEmpty() && remaining > 0) {
                var text = Context.toString(args[0])
                text = if (text === null || text.isEmpty()) "_" else text
                bot.execute(
                    SendMessage()
                        .setChatId(chatID)
                        .setText(text)
                        .disableNotification()
                )
                --remaining
            } else if (remaining <= 0) {
                throw JSBotException("Message limit reached.")
            }
            return@f ""

        })

        //defines native "message" function with markdown formatting
        ScriptableObject.putProperty(to, "mdMessage", JSFunction(1) f@{ _, _, _, args ->
            if (args.isNotEmpty() && remaining > 0) {
                var text = Context.toString(args[0])
                text = if (text === null || text.isEmpty()) "_" else text
                bot.execute(
                    SendMessage()
                        .setChatId(chatID)
                        .setText(text)
                        .enableMarkdown(true)
                        .disableNotification()
                )
                --remaining
            } else if (remaining <= 0) {
                throw JSBotException("Message limit reached.")
            }
            return@f ""

        })

        //defines native "message" function with html formatting
        ScriptableObject.putProperty(to, "htmlMessage", JSFunction(1) f@{ _, _, _, args ->
            if (args.isNotEmpty() && remaining > 0) {
                var text = Context.toString(args[0])
                text = if (text === null || text.isEmpty()) "_" else text
                bot.execute(
                    SendMessage()
                        .setChatId(chatID)
                        .setText(text)
                        .enableHtml(true)
                        .disableNotification()
                )
                --remaining
            } else if (remaining <= 0) {
                throw JSBotException("Message limit reached.")
            }
            return@f ""

        })

        //adds the sendMedia function to the scope
        ScriptableObject.putProperty(to, "sendMedia", JSFunction(1) f@{ _, _, _, args ->
            if (args.isNotEmpty() && remaining > 0) {
                val argument = args[0]
                if (argument is Scriptable) {
                    val media = SimpleMedia.fromJS(argument)
                    if (media !== null) {
                        SimpleMedia.send(media, chatID, bot)
                        --remaining
                    }
                }
            } else if (remaining <= 0) {
                throw JSBotException("Message limit reached.")
            }
            return@f ""
        })


        ScriptableObject.putProperty(to, "toFile", JSFunction(2) f@{ _, scope, _, args ->
            if (args.isEmpty() || args.size > 2) {
                throw JSBotException("Wrong number of arguments")
            } else {
                val name = if (args.size == 1) {
                    "file"
                } else when (args[1]) {
                    is String -> args[1] as String
                    else -> "file"
                }

                SimpleMedia.generateAndSendFileForObject(
                    name,
                    args[0],
                    bot,
                    chatID,
                    cx,
                    scope
                )
                return@f ""
            }
        })


        ScriptableObject.putProperty(to, "readFile", JSFunction(1) f@{ _, _, _, args ->
            if (args.isNotEmpty() && remaining > 0) {
                val argument = args[0]

                if (argument is Scriptable) {
                    val fromJS = SimpleMedia.fromJS(argument)
                    if (fromJS !== null && fromJS.mediaType == SimpleMedia.DOCUMENT) {
                        return@f fromJS.getDocumentContents(bot)
                    }
                }

                throw JSBotException("Invalid argument type.")
            } else if (remaining <= 0) {
                throw JSBotException("Operation limit reached.")
            } else {
                throw JSBotException("Illegal number of arguments.")
            }
        })


        //adds the media in the message the user is referring to (or null if not present)
        val referredMedia = SimpleMedia.fromMessage(message.replyToMessage)?.toJS(cx, to)
        ScriptableObject.putProperty(to, "refMedia", referredMedia)


        //adds the text in the message the user is referring to (or null if not present)
        val referredText =
            if (message.replyToMessage !== null && message.replyToMessage.hasText() && message.replyToMessage.text !== null) {
                Context.javaToJS(message.replyToMessage.text, to)
            } else {
                Context.javaToJS(null, to)
            }
        ScriptableObject.putProperty(to, "refText", referredText)

        if (referredMedia !== null) {
            ScriptableObject.putProperty(to, "that", referredMedia)
        } else {
            ScriptableObject.putProperty(to, "that", referredText)
        }

        ScriptableObject.putProperty(
            to,
            "me",
            jsbot.User.fromTgUser(message.from)?.toJS(cx, to, this, message)
        )

        ScriptableObject.putProperty(
            to,
            "thatUser",
            jsbot.User.fromTgUser(message.replyToMessage?.from)?.toJS(cx, to, this, message)
        )

        ScriptableObject.putProperty(to, "putHandler", JSFunction(2) f@{ _, _, _, args ->
            if (!retrieveRoleFromUser(message.from).isAuthorized(Role.CHANGE_HANDLERS_ABILITY)) {
                throw JSBotException("Not authorized to modify handlers.")
            }
            if (args.isEmpty() || args.size > 2) {
                throw JSBotException("Wrong number of arguments")
            } else {
                var name = when (args[0]) {
                    is String -> args[0] as String
                    else -> throw JSBotException("Invalid handler name.")
                }
                name = name.substring(0, min(name.length, 20))

                val func = when (args[1]) {
                    is Function -> args[1] as Function
                    else -> throw JSBotException("Second argument must be function.")
                }

                return@f putHandler(chatID, name, func)
            }
        })

        ScriptableObject.putProperty(to, "removeHandler", JSFunction(1) f@{ _, _, _, args ->
            if (!retrieveRoleFromUser(message.from).isAuthorized(Role.CHANGE_HANDLERS_ABILITY)) {
                throw JSBotException("Not authorized to modify handlers.")
            }
            if (args.size != 1) {
                throw JSBotException("Wrong number of arguments")
            }
            val name = when (args[0]) {
                is String -> args[0] as String
                else -> throw JSBotException("Invalid handler name.")
            }

            return@f removeHandler(chatID, name)
        })

        ScriptableObject.putProperty(to, "getHandlers", JSFunction(0) f@{ _, _, _, _ ->
            val result = cx.newObject(to)
            val handlers = retrieveHandlers(chatID)
            handlers.forEach { (name, func) ->
                result.put(name, result, func)
            }
            return@f result
        })

    }


    private fun scopeFileName(chatId: Long) = "scope$chatId.js"


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
        ctx.optimizationLevel = -1
        try {
            x.invoke(ctx)
        } finally {
            Context.exit()
        }
    }


    class JSBotException(message: String) : RuntimeException(message)


}




