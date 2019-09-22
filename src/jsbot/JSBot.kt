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
    private val scopemap: MutableMap<Long, Scriptable>,
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
                    if(message.hasText() && message.text !== null) {
                        val text = message.text
                        if (text.isNotEmpty()) {
                            println("Received: '$text' from ${message.chatId}:${message.from?.userName}")
                            val bot = this
                            val scope = scopeGet(it, message.chatId, bot)!!

                            doInTime(scope, text, message, 10)

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
        timeout: Long
    ) {
        val executor = Executors.newSingleThreadExecutor()
        println("Started Job...")

        val invokeAll = executor.invokeAll(
            mutableListOf(
                Callable {
                    withContext { it2 ->

                        if(text == "GETOUT"){
                            exitProcess(2)
                        }

                        val showError = text.startsWith("JS ")
                        try {
                            val result = if(showError){
                                try {
                                    Context.toString(it2.evaluateString(scope, text.substring(3), "<cmd>", 1, null))
                                }catch(e:RhinoException){
                                    println(e.message)
                                    e.message
                                }
                            }else {
                                Context.toString(it2.evaluateString(scope, text, "<cmd>", 1, null))
                            }

                            execute(SendMessage()
                                .setChatId(message.chatId)
                                .setText(result)
                                .disableNotification()
                            )
                        } catch (e: TelegramApiException) {
                            e.printStackTrace()
                        } catch (e: RhinoException){
                            println(e.message)
                        }
                        println("Ended Job.")
                    }
                }),
            timeout,
            TimeUnit.SECONDS
        )

        if (invokeAll[0].isCancelled) {
            println("Cancelled Job.")
        }
        executor.shutdown()
    }

    private fun scopeGet(cx: Context, chatID: Long?, bot: JSBot): Scriptable? {
        val result = if (chatID !== null) {
            if (scopemap.containsKey(chatID)) {
                scopemap[chatID]
            } else {
                val scope = cx.initStandardObjects()
                scopemap[chatID] = scope
                scope
            }
        } else {
            null
        }
        result?.addStuff(bot, chatID)
        return result
    }

}


fun Scriptable.addStuff(bot: JSBot, chatID: Long?) {

    //defines native "message" function
    val message1 = object : BaseFunction() {
        override fun call(cx: Context, scope: Scriptable, thisObj: Scriptable, args: Array<Any>): Any? {
            if (args.isNotEmpty()) {
                var text = Context.toString(args[0])
                text = if (text === null || text.isEmpty()) "_" else text
                bot.execute(SendMessage()
                    .setChatId(chatID)
                    .setText(text)
                    .disableNotification()
                )
            }
            return Undefined.instance
        }
        override fun getArity(): Int {
            return 1
        }
    }
    //adds the message function to the scope
    ScriptableObject.defineProperty(
        this, "message", message1,
        ScriptableObject.DONTENUM or ScriptableObject.PERMANENT or ScriptableObject.READONLY
    )

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
