package jsbot

import com.fasterxml.jackson.annotation.JsonAlias
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
    private val creator: String,
    private val creatorId: Int,
    private val scopemap: MutableMap<Long, Scriptable>, //maps chat IDs to the scope reserved to such chats
    private val usernamesMap: MutableMap<String, Int>, //maps usernames their user IDs
    private val userRoles: MutableMap<Int, Role>, //maps user IDs to their Role
    private val username: String,
    private val botToken: String
) : TelegramLongPollingBot() {


    companion object{
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

                    if(message.isUserMessage && message.chatId!== null
                        && !retrieveRoleFromId(message.chatId.toInt()).isAuthorized(Role.PRIVATE_USE_BOT_ABILITY)){
                        println("Not authorized: "+Role.PRIVATE_USE_BOT_ABILITY)
                        return@wc
                    }

                    if((message.isGroupMessage || message.isSuperGroupMessage)){
                        val role = retrieveRoleFromUser(message.from)

                        if(!role.isAuthorized(Role.GROUP_USE_BOT_ABILITY)){
                            println("Not authorized: "+Role.GROUP_USE_BOT_ABILITY)
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
                            val bot = this
                            val scope = retrieveScope(it, message, bot)!!



                            doInTime(scope, text, message, 3)

                            val saved = scope.get("saved", scope)
                            if(saved != Scriptable.NOT_FOUND
                                && saved != Context.getUndefinedValue()
                                && saved is ScriptableObject){
                                saveScope(message.chatId, it, saved)
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
        if(filein.exists() && filein.isFile) {
            filein.bufferedReader().useLines {
                it.forEach { line ->
                    val split = line.split(" ")
                    val userName = split[0]
                    val userId = split[1]
                    usernamesMap[userName] = Integer.parseInt(userId)
                }
            }
        }
    }

    private fun saveUserRoles(){
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
    }


    private fun retrieveRoleFromUserName(username: String):Role? {
        val userid = usernamesMap[username]
        return if(userid!==null){
            retrieveRoleFromId(userid)
        }else{
            null
        }
    }

    private fun retrieveRoleFromId(userid: Int):Role{
        var role = userRoles[userid]
        if (role === null) {
            role = Role.create(defaultRole)!!
            userRoles[userid] = role
        }
        return role
    }

    private fun retrieveRoleFromUser(user:User):Role{
        val fromUN = user.userName
        val fromID = user.id
        return if(fromUN !== null && usernamesMap.containsKey(fromUN)) {
            retrieveRoleFromUserName(fromUN)!!
        }else if(fromID !==null && fromID>0){
            retrieveRoleFromId(fromID)
        }else {
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
                            if(retrieveRoleFromUser(message.from).isAuthorized(Role.PANIC_ABILITY)) {
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

                            if(result is Scriptable){
                                val media = SimpleMedia.fromJS(result)
                                if(media !== null){
                                    replyWithSimpleMedia(media, message)
                                }else{
                                    replyWithText(result, message)
                                }
                            }else{
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

    private fun retrieveScope(cx: Context, message: Message, bot: JSBot): Scriptable? {
        val chatID = message.chatId
        val result = if (chatID !== null) {
            if (scopemap.containsKey(chatID)) {
                scopemap[chatID]
            } else {
                val scope = cx.initSafeStandardObjects()

                val file = File(scopeFileName(message.chatId))
                if(file.exists() && file.isFile){
                    ScriptableObject.defineProperty(
                        scope,
                        "saved",
                        loadScope(chatID, cx, cx.newObject(scope)),
                        ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                    )

                }else {
                    ScriptableObject.defineProperty(
                        scope,
                        "saved",
                        cx.newObject(scope),
                        ScriptableObject.READONLY or ScriptableObject.PERMANENT or ScriptableObject.DONTENUM
                    )
                }



                scopemap[chatID] = scope
                scope
            }
        } else {
            null
        }
        if (result != null) {
            addStuffToScope(cx, result, bot, message)

            if(!ScriptableObject.hasProperty(result, "saved")){
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

    private fun addStuffToScope(cx: Context, to: Scriptable, bot: JSBot, message: Message) {

        val chatID = message.chatId
        val userId = message.from.id
        //defines native "message" function

        var remaining = 10

        //adds the message function to the scope
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
                    if(argument is Scriptable){
                        val media = SimpleMedia.fromJS(argument)
                        if(media !== null) {
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

        //adds the "me" dynamic reference
        ScriptableObject.putProperty(to, "my", scopemap[userId.toLong()] ?: Undefined.instance)


        //adds the media in the message the user is referring to (or null if not present)
        val referredMedia =  SimpleMedia.fromMessage(message.replyToMessage)?.toJS(cx, to)
        ScriptableObject.putProperty(to, "refMedia", referredMedia)


        //adds the text in the message the user is referring to (or null if not present)
        val referredText =
            if (message.replyToMessage !== null && message.replyToMessage.hasText() && message.replyToMessage.text !== null) {
                Context.javaToJS(message.replyToMessage.text, to)
            } else {
                Context.javaToJS(null, to)
            }
        ScriptableObject.putProperty(to, "refText", referredText)

        if(referredMedia!==null){
            ScriptableObject.putProperty(to, "that", referredMedia)
        }else{
            ScriptableObject.putProperty(to, "that", referredText)
        }

        val retrievedRole = retrieveRoleFromUser(message.from)
        ScriptableObject.putProperty(to, "role", retrievedRole.getRoleName())

        if(retrieveRoleFromUser(message.from).isAuthorized(Role.JAVA_ABILITY)) {
            ScriptableObject.putProperty(to, "java", cx.initStandardObjects())
        }else{
            ScriptableObject.putProperty(to, "java", null)
        }


        var remainingRole = 1
        ScriptableObject.putProperty(to, "setRole", object:BaseFunction(){
            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if(!retrieveRoleFromUser(message.from).isAuthorized(Role.SET_ROLES_ABILITY)){
                    throw JSBotException("Not authorized to set role.")
                }
                if (args.size == 2 && remainingRole > 0) {
                    val argumentRole = args[1]
                    if(argumentRole is String){
                        val createdRole = Role.create(argumentRole)

                        if(createdRole===null){
                            return false
                        }

                        val argumentUN = args[0]
                        if(argumentUN is String){
                            if(argumentUN == creator){
                                throw JSBotException("Creator's role is immutable.")
                            }
                            val id = usernamesMap[argumentUN]
                            if (id !== null) {
                                remainingRole--
                                userRoles[id] = createdRole
                                saveUserRoles()
                                return true
                            }

                            return false
                        }else if(argumentUN is Int){
                            if(argumentUN == creatorId){
                                throw JSBotException("Creator's role is immutable.")
                            }
                            remainingRole--
                            userRoles[argumentUN] = createdRole
                            saveUserRoles()
                            return true
                        }
                    }

                } else if (remainingRole <= 0) {
                    throw JSBotException("Operation limit reached.")
                }
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 2
            }
        })


        val abilityList = mutableListOf<Any>()
        retrievedRole.getAbilites().forEach{
            abilityList.add(it)
        }
        ScriptableObject.putProperty(to, "abilities", cx.newArray(to, abilityList.toTypedArray()))

    }



    private fun scopeFileName(chatId: Long) = "scope$chatId.js"

    private fun saveScope(chatId:Long, context: Context, saved:Scriptable){
        val filename = scopeFileName(chatId)
        println("Saving Scope $filename")

        PrintWriter(FileOutputStream(filename)).use{
            val source = serialize(context, saved)
            it.println(source)
        }
    }

    private fun serialize(context: Context, theObject: Scriptable) :String{
        val result = context.evaluateString(
            theObject,
            "toSource()",
            "<save>",
            1,
            null
        )
        return Context.toString(result)
    }


    private fun loadScope(chatId:Long, context: Context, saved:Scriptable): Any{
        val filename = scopeFileName(chatId)
        val file = File(filename)
        if(file.exists() && file.isFile) {
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


