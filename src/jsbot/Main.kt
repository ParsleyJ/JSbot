package jsbot

import org.json.JSONObject
import org.json.JSONTokener
import org.mozilla.javascript.*
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Created on 11/09/2019.
 *
 */



fun main(args: Array<String>) {









    val propFile: File


    if (args.size > 1) {
        println("Usage: java -jar JSBot (properties-file)")
        return
    }

    if (args.isEmpty()) {
        propFile = File("./JSBot.properties")
        if (!propFile.exists() || !propFile.isFile) {
            println("Usage: java -jar JSBot (properties-file)")
            return
        }
        println("Working Directory = ${System.getProperty("user.dir")}")

    } else {
        propFile = File(args[0])
        if (!propFile.exists() || !propFile.isFile) {
            println("Invalid properties file: ${args[0]}")
            return
        }
    }

    val propertyFileDirectory = propFile.parent
    println("Properties file = ${propFile.absolutePath}")
    println("Directory of property file = $propertyFileDirectory")
    Emoji.loadEmojis("$propertyFileDirectory/emojis.json")

    if(Emoji.isEmojiLoaded()) {
        Emoji.findEmoji("rofl").forEach {
            println(" $it emojis loaded $it ")
        }
    }else{
        println("Could not load emojis.")
    }
    FileInputStream(propFile).use { input ->
        val properties = Properties()
        properties.load(input)
        val token = properties.getProperty("tgapi.token")
        val username = properties.getProperty("tgapi.botUsername")
        val creator = properties.getProperty("creator.username")
        val creatorId = Integer.parseInt(properties.getProperty("creator.id"))
        println("Token loaded = $token")
        println("Username loaded = $username")
        println("Creator = $creatorId:$creator")


        val scopemap = mutableMapOf<Long, Scriptable>()
        val usernamesMap = mutableMapOf<String, Int>()
        val userRoles = mutableMapOf<Int, Role>()
        if (creator !== null) {
            usernamesMap[creator] = creatorId
            userRoles[creatorId] = Role.Companion.SuperAdmin()
        }
        ApiContextInitializer.init()

        val botsApi = TelegramBotsApi()
        try {

            val jsbot = JSBot(
                creator,
                creatorId,
                scopemap,
                usernamesMap,
                userRoles,
                username,
                token
            )
            jsbot.loadUsernames()
            jsbot.loadUserRoles()

            botsApi.registerBot(jsbot)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }

    }


}




