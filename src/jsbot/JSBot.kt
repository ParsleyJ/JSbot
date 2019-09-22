package jsbot

import org.mozilla.javascript.*
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class JSBot(
    private val scopemap: MutableMap<Long, Scriptable>, //maps chat IDs to the scope reserved to such chats
    private val userChatMap: MutableMap<Int, Long>, //maps user IDs to their private chats with the bot
    private val username: String,
    private val botToken: String
) : TelegramLongPollingBot() {

    override fun getBotUsername(): String = username
    override fun getBotToken(): String = botToken


    override fun onUpdateReceived(update: Update?) {
        withContext wc@{
            try {
                update!!


                if (update.hasMessage()) {
                    val message = update.message!!
                    if (!authorized(message)) {
                        return@wc
                    }

                    if (message.isUserMessage && message.from.userName !== null) {
                        userChatMap[message.from.id] = message.chatId
                    }

                    if (message.hasText() && message.text !== null) {
                        val text = message.text
                        if (text.isNotEmpty()) {
                            println("Received: '$text' from ${message.chatId}:${message.from?.userName}")
                            val bot = this
                            val scope = scopeGet(it, message, bot)!!

                            doInTime(scope, text, message, 3)

                        }
                    }
                }
            } catch (e: KotlinNullPointerException) {
                e.printStackTrace()
            }
        }
    }

    private fun authorized(message: Message) =
        message.isUserMessage
                || message.isGroupMessage
                || message.isSuperGroupMessage


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

                        if (text == "SEPPUKU") {
                            exitProcess(2) //HARAKIRIIIII
                        }

                        val showError = text.startsWith("JS ")
                        try {
                            val result: Any? = if (showError) {
                                try {
                                    it2.evaluateString(scope, text.substring(3), "<cmd>", 1, null)
                                } catch (e: RhinoException) {
                                    println(e.message)
                                    e.message
                                } catch (e: JSBotException) {
                                    println(e.message)
                                    e.message
                                }
                            } else {
                                it2.evaluateString(scope, text, "<cmd>", 1, null)
                            }

                            try{
                                val media = Context.jsToJava(result, SimpleMedia::class.java) as SimpleMedia?
                                if(media!==null){
                                    SimpleMedia.send(media, message.chatId, this)
                                }
                            }catch(e : EvaluatorException){
                                var textResult = Context.toString(result)
                                textResult = if (textResult === null) "" else textResult
                                if(textResult.isNotBlank()) {
                                    execute(
                                        SendMessage()
                                            .setChatId(message.chatId)
                                            .setText(textResult)
                                            .setReplyToMessageId(message.messageId)
                                            .disableNotification()
                                    )
                                }
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

    private fun scopeGet(cx: Context, message: Message, bot: JSBot): Scriptable? {
        val chatID = message.chatId
        val result = if (chatID !== null) {
            if (scopemap.containsKey(chatID)) {
                scopemap[chatID]
            } else {
                val scope = cx.initSafeStandardObjects()
                scopemap[chatID] = scope
                scope
            }
        } else {
            null
        }
        if (result != null) {
            addStuff(result, bot, message)
        }
        return result
    }

    fun addStuff(to: Scriptable, bot: JSBot, message: Message) {

        val chatID = message.chatId
        val userId = message.from.id
        //defines native "message" function

        var remaining = 10

        val message1 = object : BaseFunction() {

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
                }else if (remaining <= 0){
                    throw JSBotException("Message limit reached.")
                }
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
            }
        }


        val sendMedia1 = object : BaseFunction() {

            override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
                if (args.isNotEmpty() && remaining > 0) {
                    val media = Context.jsToJava(args[0], SimpleMedia::class.java)
                    SimpleMedia.send(media as SimpleMedia, chatID, bot)
                    --remaining
                }else if (remaining <= 0){
                    throw JSBotException("Message limit reached.")
                }
                return Undefined.instance
            }

            override fun getArity(): Int {
                return 1
            }
        }


        //adds the message function to the scope
        ScriptableObject.putProperty(to, "message", message1)

        //adds the sendMedia function to the scope
        ScriptableObject.putProperty(to, "sendMedia", sendMedia1)

        //adds the "me" dynamic reference
        if (userChatMap.containsKey(userId)) {
            ScriptableObject.putProperty(to, "me", scopemap[userChatMap[userId]] ?: Undefined.instance)
        }else{
            ScriptableObject.putProperty(to, "me", Undefined.instance)
        }

        //adds the media in the message the user is referring to (or null if not present)
        val referredMedia = Context.javaToJS(SimpleMedia.fromMessage(message.replyToMessage), to)
        ScriptableObject.putProperty(to, "refMedia", referredMedia)


        //adds the text in the message the user is referring to (or null if not present)
        val referredText =  if(message.replyToMessage!==null && message.replyToMessage.hasText() && message.replyToMessage.text !== null) {
            Context.javaToJS(message.replyToMessage.text, to)
        }else{
            Context.javaToJS(null, to)
        }
        ScriptableObject.putProperty(to, "refText", referredText)




    }

    class JSBotException(message:String) : RuntimeException(message)

}


/*
Each thread must have its Rhino context entered; this is used as
construct for portions of code with specific contexts.
 */
fun withContext(x: (ctx: Context) -> Unit) {
    val ctx = Context.enter()
    try {
        x.invoke(ctx)
    } finally {
        Context.exit()
    }
}
