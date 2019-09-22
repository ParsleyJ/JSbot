package jsbot

import org.mozilla.javascript.*
import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

/**
 * Created on 11/09/2019.
 *
 */

fun main() {

    println("Working Directory = ${System.getProperty("user.dir")}")

    FileInputStream("JSBot.properties").use { input ->
        val properties = Properties()
        properties.load(input)
        val token = properties.getProperty("tgapi.token")
        val username = properties.getProperty("tgapi.botUsername")
        println("token loaded = $token")
        println("username loaded = $username")

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




