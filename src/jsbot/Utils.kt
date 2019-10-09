package jsbot

import jsbot.jsapi.Event
import jsbot.jsapi.Media
import jsbot.jsapi.User
import org.json.JSONObject
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.send.SendDocument
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle

/**
 * Created on 28/09/2019.
 *
 */
fun List<String>.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.newArray(scope, this.map { Context.javaToJS(it, scope) }.toTypedArray())
}

fun JSONObject.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.evaluateString(scope, "(${this})", "toScriptable", 1, null)
}

fun String.toScriptable(cx: Context, scope: Scriptable): Any {
    return cx.evaluateString(scope, "\"${this}\"", "toScriptable", 1, null)
}


fun Scriptable.serialize(context: Context? = null): String {
    if (this is User) {
        return this.jsFunction_toSource()
    }
    if (this is Event) {
        return this.jsFunction_toSource()
    }
    val cx = context ?: Context.getCurrentContext()

    val result = cx.evaluateString(
        this,
        "toSource()",
        "<save>",
        1,
        null
    )
    return Context.toString(result)
}


fun InlineQuery.answerInlineQuery(
    title: String,
    textResult: String?
): AnswerInlineQuery? {
    return AnswerInlineQuery()
        .setInlineQueryId(id)
        .setResults(
            InlineQueryResultArticle()
                .setId(id)
                .setTitle(title)
                .setDescription(textResult)
                .setInputMessageContent(
                    InputTextMessageContent()
                        .setMessageText(textResult)
                )
        )
}


fun generateAndSendFile(name: String, contents: String, bot: JSBot, chatID: Long) {
    val sendDoc = SendDocument()
        .setDocument(name, contents.byteInputStream())
        .setChatId(chatID)
        .disableNotification()

    bot.execute(sendDoc)
}

fun Message.hasMedia(): Boolean {
    return hasAnimation()
            || hasAudio()
            || hasDocument()
            || hasPhoto()
            || hasSticker()
            || hasVideo()
            || hasVoice()
}


fun Any.serializeToFileAndSend(
    name: String,
    bot: JSBot,
    chatID: Long,
    cx: Context,
    scope: Scriptable
) {
    val jscontents = if (this is Scriptable) {
        this
    } else {
        Context.javaToJS(this, scope)
    }

    if (jscontents is Scriptable) {
        generateAndSendFile(name, jscontents.serialize(cx), bot, chatID)
    } else {
        generateAndSendFile(name, jscontents.toString(), bot, chatID)
    }
}

fun Any.toJS(cx: Context, scope: Scriptable) = when (this) {
    is Media -> this.toJS(cx, scope)
    is Event -> this.toJS(cx, scope)
    is User -> this.toJS(cx, scope)
    is jsbot.jsapi.Message -> this.toJS(cx, scope)
    else -> Context.javaToJS(this, scope)
}



fun String?.clipForMessage(limit: Int = 4096, replyEmptyString: Boolean = true): String {
    var textResult = this
    textResult = if (textResult === null) "" else textResult

    if (replyEmptyString && textResult.isBlank()) {
        textResult = "< empty string >"
    }

    if (textResult.length >= limit) {
        var lenghtMark = "\n...< message length: ${textResult.length} >..."
        if (lenghtMark.length >= 4096) {
            lenghtMark = lenghtMark.substring(0, limit - 1)
        }
        textResult = textResult.substring(0, limit - 1 - lenghtMark.length - 1) + lenghtMark
    }
    return textResult
}
