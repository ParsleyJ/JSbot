package jsbot

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


fun main(args:Array<String>) {

    val propFile : File


    if(args.size>1){
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
    }else {
        propFile = File(args[0])
        if (!propFile.exists() || !propFile.isFile) {
            println("Invalid properties file: ${args[0]}")
            return
        }
    }

    println("Properties file = ${propFile.absolutePath}")

    FileInputStream(propFile).use { input ->
        val properties = Properties()
        properties.load(input)
        val token = properties.getProperty("tgapi.token")
        val username = properties.getProperty("tgapi.botUsername")
        println("Token loaded = $token")
        println("Username loaded = $username")

        //todo add persistence
        val scopemap = mutableMapOf<Long, Scriptable>()
        val userchatmap = mutableMapOf<Int, Long>()


        ApiContextInitializer.init()

        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(JSBot(scopemap, userchatmap, username, token))
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }


}




