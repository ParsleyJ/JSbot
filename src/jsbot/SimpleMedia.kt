package jsbot

import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.Message

/**
 * Created on 22/09/2019.
 *
 */
class SimpleMedia(var mediaType:String, var fileID:String) {

    fun toJS(cx : Context, scope:Scriptable): Scriptable? {
        val result = cx.newObject(scope)
        ScriptableObject.putProperty(result, "mediaType", mediaType)
        ScriptableObject.putProperty(result, "fileID", fileID)
        return result
    }

    companion object {
        val ANIMATION = "ANIMATION"
        val AUDIO =  "AUDIO"
        val DOCUMENT = "DOCUMENT"
        val PHOTO = "PHOTO"
        val STICKER = "STICKER"
        val VIDEO = "VIDEO"
        val VOICE = "VOICE"

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

        fun fromJS(obj: Scriptable) : SimpleMedia?{
            val mediaType = obj.get("mediaType", obj)
            val fileID = obj.get("fileID", obj)
            return if(mediaType is String && fileID is String){
                SimpleMedia(mediaType, fileID)
            }else{
                null
            }
        }

        fun send(media: SimpleMedia?, chatID:Long, bot:JSBot){
            if(media == null){
                bot.execute(SendMessage()
                    .setText("null")
                    .setChatId(chatID)
                    .disableNotification())
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
    }
}