package jsbot.jsapi

import jsbot.JSBot
import jsbot.serialize
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.telegram.telegrambots.meta.api.methods.AnswerInlineQuery
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.methods.GetFile
import org.telegram.telegrambots.meta.api.objects.inlinequery.InlineQuery
import org.telegram.telegrambots.meta.api.objects.inlinequery.inputmessagecontent.InputTextMessageContent
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.InlineQueryResultArticle
import org.telegram.telegrambots.meta.api.objects.inlinequery.result.cached.*
import java.io.BufferedReader
import java.io.InputStreamReader


/**
 * Created on 22/09/2019.
 *
 */
class SimpleMedia(var mediaType: String, var fileID: String) {

    fun toJS(cx: Context, scope: Scriptable): Scriptable? {
        val result = cx.newObject(scope)
        ScriptableObject.putProperty(result, "mediaType", mediaType)
        ScriptableObject.putProperty(result, "fileID", fileID)
        return result
    }


    fun getFilePath(bot: JSBot) = bot.execute(GetFile().setFileId(fileID)).filePath

    fun getFileSize(bot: JSBot) = bot.execute(GetFile().setFileId(fileID)).fileSize

    fun downloadByFilePath(bot: JSBot, filePath: String) = bot.downloadFile(filePath)


    fun getDocumentContents(bot: JSBot): String {
        if (mediaType == DOCUMENT) {
            val tgFile = bot.execute(GetFile().setFileId(fileID))

            val fileSize = tgFile.fileSize
            if (fileSize > FILE_LIMIT) {
                throw JSBot.JSBotException("File too big: $fileSize")
            }
            val stream = bot.downloadFileAsStream(tgFile)

            return BufferedReader(InputStreamReader(stream)).readText()
        } else {
            throw JSBot.JSBotException("Invalid media type")
        }
    }

    fun evaluateDocument(m: SimpleMedia, bot: JSBot, cx: Context, scope: Scriptable): Any? {
        return cx.evaluateString(scope, m.getDocumentContents(bot), "evalDocument", 1, null)
    }


    companion object {
        const val FILE_LIMIT = 500000
        const val ANIMATION = "ANIMATION"
        const val AUDIO = "AUDIO"
        const val DOCUMENT = "DOCUMENT"
        const val PHOTO = "PHOTO"
        const val STICKER = "STICKER"
        const val VIDEO = "VIDEO"
        const val VOICE = "VOICE"

        fun fromMessage(msg: Message?): SimpleMedia? {
            return when {
                msg == null -> null
                msg.hasAnimation() -> SimpleMedia(
                    ANIMATION,
                    msg.animation.fileId
                )
                msg.hasAudio() -> SimpleMedia(AUDIO, msg.audio.fileId)
                msg.hasDocument() -> SimpleMedia(
                    DOCUMENT,
                    msg.document.fileId
                )
                msg.hasPhoto() -> SimpleMedia(PHOTO, msg.photo[0].fileId)
                msg.hasSticker() -> SimpleMedia(
                    STICKER,
                    msg.sticker.fileId
                )
                msg.hasVideo() -> SimpleMedia(VIDEO, msg.video.fileId)
                msg.hasVoice() -> SimpleMedia(VOICE, msg.voice.fileId)
                else -> null
            }
        }

        fun fromJS(obj: Scriptable): SimpleMedia? {
            val mediaType = obj.get("mediaType", obj)
            val fileID = obj.get("fileID", obj)
            return if (mediaType is String && fileID is String) {
                SimpleMedia(mediaType, fileID)
            } else {
                null
            }
        }

        fun send(media: SimpleMedia?, chatID: Long, bot: JSBot, withText: String? = null) {
            if (media == null) {
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
            val fileID = media.fileID

            when (media.mediaType) {
                ANIMATION -> bot.execute(SendAnimation().setAnimation(fileID).setChatId(chatID).disableNotification())
                AUDIO -> bot.execute(SendAudio().setAudio(fileID).setChatId(chatID).disableNotification())
                DOCUMENT -> bot.execute(SendDocument().setDocument(fileID).setChatId(chatID).disableNotification())
                PHOTO -> bot.execute(SendPhoto().setPhoto(fileID).setChatId(chatID).disableNotification())
                STICKER -> bot.execute(SendSticker().setSticker(fileID).setChatId(chatID).disableNotification())
                VIDEO -> bot.execute(SendVideo().setVideo(fileID).setChatId(chatID).disableNotification())
                VOICE -> bot.execute(SendVoice().setVoice(fileID).setChatId(chatID).disableNotification())
            }
        }

        fun giveInlineQueryResult(
            command: String,
            media: SimpleMedia?,
            inlineQuery: InlineQuery,
            bot: JSBot,
            withText: String? = null
        ) {
            if (media == null) {
                bot.execute(
                    if (withText !== null) {
                        answerInlineQuery(inlineQuery, command, withText)
                    } else {
                        answerInlineQuery(inlineQuery, command, "null")
                    }
                )
                return
            }
            val fileID = media.fileID

            val queryId = inlineQuery.id

            when (media.mediaType) {
                ANIMATION -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedGif().setGifFileId(fileID).setId(queryId)
                    )
                )
                AUDIO -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedAudio().setAudioFileId(fileID).setId(queryId)
                    )
                )
                DOCUMENT -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedDocument().setDocumentFileId(fileID).setId(queryId)
                    )
                )
                PHOTO -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedPhoto().setPhotoFileId(fileID).setId(queryId)
                    )
                )
                STICKER -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedSticker().setStickerFileId(fileID).setId(queryId)
                    )
                )
                VIDEO -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedVideo().setVideoFileId(fileID).setId(queryId)
                    )
                )
                VOICE -> bot.execute(
                    AnswerInlineQuery().setInlineQueryId(queryId).setResults(
                        InlineQueryResultCachedVoice().setVoiceFileId(fileID).setId(queryId)
                    )
                )
            }

        }

        fun answerInlineQuery(
            inlineQuery: InlineQuery,
            title: String,
            textResult: String?
        ): AnswerInlineQuery? {
            return AnswerInlineQuery()
                .setInlineQueryId(inlineQuery.id)
                .setResults(
                    InlineQueryResultArticle()
                        .setId(inlineQuery.id)
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


        fun generateAndSendFileForObject(
            name: String,
            contents: Any,
            bot: JSBot,
            chatID: Long,
            cx: Context,
            scope: Scriptable
        ) {
            val jscontents = if (contents is Scriptable) {
                contents
            } else {
                Context.javaToJS(contents, scope)
            }

            if (jscontents is Scriptable) {
                generateAndSendFile(name, jscontents.serialize(cx), bot, chatID)
            } else {
                generateAndSendFile(name, jscontents.toString(), bot, chatID)
            }
        }

        fun hasMedia(message: Message): Boolean {
            return message.hasAnimation()
                    || message.hasAudio()
                    || message.hasDocument()
                    || message.hasPhoto()
                    || message.hasSticker()
                    || message.hasVideo()
                    || message.hasVoice()
        }
    }
}