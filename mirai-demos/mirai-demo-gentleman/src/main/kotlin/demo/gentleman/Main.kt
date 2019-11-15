@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS", "EXPERIMENTAL_API_USAGE")

package demo.gentleman

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotAccount
import net.mamoe.mirai.addFriend
import net.mamoe.mirai.event.Subscribable
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.event.subscribeMessages
import net.mamoe.mirai.login
import net.mamoe.mirai.message.Image
import net.mamoe.mirai.network.protocol.tim.packet.action.downloadAsByteArray
import net.mamoe.mirai.network.protocol.tim.packet.event.FriendMessage
import net.mamoe.mirai.network.protocol.tim.packet.login.requireSuccess
import net.mamoe.mirai.utils.currentTime
import java.io.File
import kotlin.random.Random

private fun readTestAccount(): BotAccount? {
    val file = File("testAccount.txt")
    if (!file.exists() || !file.canRead()) {
        return null
    }

    val lines = file.readLines()
    return try {
        BotAccount(lines[0].toUInt(), lines[1])
    } catch (e: Exception) {
        null
    }
}

@Suppress("UNUSED_VARIABLE")
suspend fun main() {
    val bot = Bot(
        readTestAccount() ?: BotAccount(
            id = 913366033u,
            password = "a18260132383"
        )
    ).apply { login().requireSuccess() }

    /**
     * 监听所有事件
     */
    subscribeAlways<Subscribable> {
        //bot.logger.verbose("收到了一个事件: ${it::class.simpleName}")
    }

    bot.subscribeMessages {
        "你好" reply "你好!"

        "profile" reply {
            sender.profile.await().toString()
        }

        has<Image> {
            if (this is FriendMessage) {
                withContext(IO) {
                    reply(message[Image] + " downloading")
                    sender.downloadAsByteArray(message[Image]).inputStream()
                        .transferTo(File(System.getProperty("user.dir", "testDownloadedImage${currentTime}.png")).outputStream())
                    reply(message[Image] + " downloaded")
                }
            }
        }

        startsWith("随机图片", removePrefix = true) {
            repeat(it.toIntOrNull() ?: 1) {
                GlobalScope.launch {
                    delay(Random.Default.nextLong(100, 1000))
                    Gentlemen.provide(subject).receive().image.await().send()
                }
            }
        }

        startsWith("添加好友", removePrefix = true) {
            reply(bot.addFriend(it.toUInt()).toString())
        }

    }

    bot.network.awaitDisconnection()//等到直到断开连接
}