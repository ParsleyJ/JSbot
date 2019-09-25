package jsbot

import org.mozilla.javascript.*
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import java.io.*


class JSBot(
    public val creator: String,
    public val creatorId: Int,
    public val scopemap: MutableMap<Long, Scriptable>, //maps chat IDs to the scope reserved to such chats
    public val usernamesMap: MutableMap<String, Int>, //maps usernames their user IDs
    public val userRoles: MutableMap<Int, Role>, //maps user IDs to their Role
    private val username: String,
    private val botToken: String
) : TelegramLongPollingBot() {


    companion object {
        const val defaultRole = Role.USER_ROLE
    }


    override fun getBotUsername(): String = username
    override fun getBotToken(): String = botToken


    override fun onUpdateReceived(update: Update?) {
        withContext wc@{
            try {
                update!!
                if (update.hasMessage()) {
                    val message = update.message!!

                    println()
                    println("From ${message.chatId}:${message.from?.id}:${message.from?.userName} --------------")

                    if (message.isUserMessage && message.chatId !== null
                        && !retrieveRoleFromId(message.chatId.toInt()).isAuthorized(Role.PRIVATE_USE_BOT_ABILITY)
                    ) {
                        println("Not authorized: " + Role.PRIVATE_USE_BOT_ABILITY)
                        return@wc
                    }

                    if ((message.isGroupMessage || message.isSuperGroupMessage)) {
                        val role = retrieveRoleFromUser(message.from)

                        if (!role.isAuthorized(Role.GROUP_USE_BOT_ABILITY)) {
                            println("Not authorized: " + Role.GROUP_USE_BOT_ABILITY)
                            return@wc
                        }
                    }


                    if (message.isUserMessage && message.from.userName !== null) {
                        usernamesMap[message.from.userName] = message.from.id
                    }

                    if (message.hasText() && message.text !== null) {
                        val text = message.text
                        if (text.isNotEmpty()) {
                            println("Text: '$text'")

                            val scope = retrieveScope(it, message.chatId, message)!!



                            doInTime(scope, text, message, 3)

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
                    }
                }
            } catch (e: KotlinNullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun saveUsernameMap() {
        println("save usernames, size:${usernamesMap.size}")
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
        println("loaded usernames, size:${usernamesMap.size}")
        usernamesMap.forEach { (name, id) ->
            println("$name:$id")
        }
    }

    private fun saveUserRoles() {
        println("save user roles, size:${userRoles.size}")
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
        println("loaded user roles, size:${userRoles.size}")
        userRoles.forEach { (name, role) ->
            println("$name:${role.getRoleName()}")
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


    /*
    TODO: try to use kotlin coroutines instead of threads
     */
    private fun doInTime(
        scope: Scriptable,
        text: String,
        message: Message,
        seconds: Long
    ) {
        val executor = Executors.newSingleThreadExecutor()
        println("Started Job...")

        val invokeAll = executor.invokeAll(
            mutableListOf(
                Callable {
                    withContext { it2 ->

                        if (text.startsWith("SEPPUKU")) {
                            if (retrieveRoleFromUser(message.from).isAuthorized(Role.PANIC_ABILITY)) {

                                execute(SendMessage(message.chatId, "cout << \"Sto morendo!\";"))

                                exitProcess(2) //HARAKIRIIIII
                            }
                        }



                        val showError = text.startsWith("JS ")
                        var command = text
                        try {
                            val result: Any? = if (showError) {
                                try {
                                    command = text.substring(3)
                                    it2.evaluateString(scope, command, "<cmd>", 1, null)
                                } catch (e: RhinoException) {
                                    println(e.message)
                                    e.message
                                } catch (e: JSBotException) {
                                    println(e.message)
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

                        } catch (e: TelegramApiException) {
                            e.printStackTrace()
                        } catch (e: RhinoException) {
                            println(e.message)
                        } catch (e: JSBotException) {
                            println(e.message)
                        }
                        println("Ended Job.")
                    }
                }),
            seconds,
            TimeUnit.SECONDS
        )

        if (invokeAll[0].isCancelled) {
            println("Cancelled Job.")
        }
        executor.shutdown()
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

        println("retrieving scope :$scopeID")
        val result = if (scopemap.containsKey(scopeID)) {
            scopemap[scopeID]
        } else {
            val scope = cx.initSafeStandardObjects()

            val file = File(scopeFileName(scopeID))
            if (file.exists() && file.isFile) {
                ScriptableObject.defineProperty(
                    scope,
                    "saved",
                    loadScope(scopeID, cx, cx.newObject(scope)),
                    ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                )

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
                println("retrieving scope :$scopeID ---: adding message-dependent properties")
                addMessageDependentStuff(cx, result, this, message)
                println("retrieving scope :$scopeID ---: adding user-dependent properties, user: ${message.from.id}")
                addUserStuff(cx, result, message.from.id, message)
            } else {
                if (scopeID > 0) {
                    println("retrieving scope :$scopeID ---: adding user-dependent properties (user chat)")
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


    private fun addUserStuff(cx: Context, to: Scriptable, userId: Int, jobMessage: Message? = null) {

        //adds the "me" dynamic reference
        ScriptableObject.putProperty(to, "my", scopemap[userId.toLong()] ?: retrieveScope(cx, userId.toLong()))


        val retrievedRole = retrieveRoleFromId(userId)
        ScriptableObject.putProperty(to, "role", retrievedRole.getRoleName())


        //adds "java" classpath for authorized people
        if (retrieveRoleFromId(userId).isAuthorized(Role.JAVA_ABILITY)) {
            ScriptableObject.putProperty(to, "java", cx.initStandardObjects())
        } else {
            ScriptableObject.putProperty(to, "java", null)
        }


        //adds access to this bot object
        if (retrieveRoleFromId(userId).isAuthorized(Role.BOT_ACCESS_ABILITY)) {
            ScriptableObject.putProperty(to, "bot", Context.javaToJS(this, to))
        } else {
            ScriptableObject.putProperty(to, "bot", null)
        }


        //adds ability to set other people's roles
        var remainingRole = 1
        ScriptableObject.putProperty(to, "setRole", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if (!retrieveRoleFromId(userId).isAuthorized(Role.SET_ROLES_ABILITY)) {
                    throw JSBotException("Not authorized to set role.")
                }

                if (args.size >= 2 && remainingRole > 0) {
                    val argumentRole = args[1]
                    if (argumentRole is String) {
                        val createdRole = Role.create(argumentRole)

                        if (createdRole === null) {
                            return false
                        }

                        val argUser = args[0]
                        if (argUser is String) {
                            if (argUser == creator) {
                                throw JSBotException("Creator's role is immutable.")
                            }
                            val id = usernamesMap[argUser]
                            if (id !== null) {
                                remainingRole--
                                userRoles[id] = createdRole
                                saveUserRoles()
                                return true
                            }

                            throw JSBotException("Illegal target user argument.")
                        } else if (argUser is Int) {
                            if (argUser == creatorId) {
                                throw JSBotException("Creator's role is immutable.")
                            }
                            remainingRole--
                            userRoles[argUser.toInt()] = createdRole
                            saveUserRoles()
                            return true
                        } else if (argUser is Scriptable){
                            val fromJS = jsbot.User.fromJS(argUser)
                            if(fromJS!==null){
                                if (fromJS.id == creatorId) {
                                    throw JSBotException("Creator's role is immutable.")
                                }
                                remainingRole--
                                userRoles[fromJS.id] = createdRole
                                saveUserRoles()
                                return true
                            }else{
                                throw JSBotException("Illegal target user argument.")
                            }
                        } else {
                            throw JSBotException("Illegal target user argument.")
                        }
                    }else{
                        throw JSBotException("Illegal role argument.")
                    }


                } else if (remainingRole <= 0) {
                    throw JSBotException("Operation limit reached.")
                }
                return false
            }

            override fun getArity(): Int {
                return 2
            }
        })


        val abilityList = mutableListOf<Any>()
        retrievedRole.getAbilites().forEach {
            abilityList.add(it)
        }
        ScriptableObject.putProperty(to, "abilities", cx.newArray(to, abilityList.toTypedArray()))


        ScriptableObject.putProperty(to, "readFile", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if (!retrieveRoleFromId(userId).isAuthorized(Role.LOAD_FILE_ABILITY)) {
                    throw JSBotException("Not authorized to load files from server disk.")
                }
                if (args.isNotEmpty()) {
                    var text = Context.toString(args[0])
                    if (text === null || text.isEmpty()) {
                        throw JSBotException("Invalid filename")
                    }
                    val file = File(text)
                    if (!file.exists() || !file.isFile) {
                        throw JSBotException("File not found or not a file")
                    }

                    return file.readText()
                } else {
                    throw JSBotException("Missing argument")
                }
            }

            override fun getArity(): Int {
                return 1
            }
        })

        val bot = this
        ScriptableObject.putProperty(to, "getUser", object : BaseFunction() {
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<out Any>): Any? {
                if (!retrieveRoleFromId(userId).isAuthorized(Role.USER_DATABASE_READ_ABILITY)) {
                    throw JSBotException("Not authorized to read users database.")
                }
                if (args.isNotEmpty()) {
                    val argument = args[0]
                    return when (argument) {
                        is String -> {
                            val id = usernamesMap[argument]
                            when {
                                id !== null -> jsbot.User(id, argument).toJS(cx, to, bot, jobMessage)
                                else -> null
                            }
                        }
                        is Int -> jsbot.User(argument).toJS(cx, to, bot, jobMessage)
                        else -> null
                    }
                } else {
                    throw JSBotException("Missing argument")
                }
            }
        })
    }

    private fun addMessageDependentStuff(cx: Context, to: Scriptable, bot: JSBot, message: Message) {

        val chatID = message.chatId

        var remaining = 10

        //adds the message function to the scope
        //defines native "message" function
        ScriptableObject.putProperty(to, "message", object : BaseFunction() {

            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
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
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
            }
        })

        //adds the sendMedia function to the scope
        ScriptableObject.putProperty(to, "sendMedia", object : BaseFunction() {

            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
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
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
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

    }


    private fun scopeFileName(chatId: Long) = "scope$chatId.js"

    private fun saveScope(chatId: Long, context: Context, saved: Scriptable) {
        val filename = scopeFileName(chatId)
        println("Saving Scope $filename")

        PrintWriter(FileOutputStream(filename)).use {
            val source = serialize(context, saved)
            it.println(source)
        }
    }

    private fun serialize(context: Context, theObject: Scriptable): String {
        val result = context.evaluateString(
            theObject,
            "toSource()",
            "<save>",
            1,
            null
        )
        return Context.toString(result)
    }


    private fun loadScope(chatId: Long, context: Context, saved: Scriptable): Any {
        val filename = scopeFileName(chatId)
        val file = File(filename)
        if (file.exists() && file.isFile) {
            val fis = FileInputStream(file)
            fis.bufferedReader().use {
                val result = context.evaluateReader(saved, it, "<load>", 1, null)
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


