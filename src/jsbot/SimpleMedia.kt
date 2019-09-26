package jsbot

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.methods.GetFile
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



    fun getDocumentContents(bot: JSBot): String{
        if(mediaType == DOCUMENT){
            val tgFile = bot.execute(GetFile().setFileId(fileID))

            val fileSize = tgFile.fileSize
            if(fileSize > FILE_LIMIT){
                throw JSBot.JSBotException("File too big: $fileSize")
            }
            val stream = bot.downloadFileAsStream(tgFile)

            return BufferedReader(InputStreamReader(stream)).readText()
        }else{
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
                msg.hasAnimation() -> SimpleMedia(ANIMATION, msg.animation.fileId)
                msg.hasAudio() -> SimpleMedia(AUDIO, msg.audio.fileId)
                msg.hasDocument() -> SimpleMedia(DOCUMENT, msg.document.fileId)
                msg.hasPhoto() -> SimpleMedia(PHOTO, msg.photo[0].fileId)
                msg.hasSticker() -> SimpleMedia(STICKER, msg.sticker.fileId)
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
            val jscontents = if(contents is Scriptable) {
                contents
            }else {
                Context.javaToJS(contents,scope)
            }

            if(jscontents is Scriptable){
                generateAndSendFile(name, jscontents.serialize(cx), bot, chatID)
            }else{
                generateAndSendFile(name, jscontents.toString(), bot, chatID)
            }
        }
    }
}