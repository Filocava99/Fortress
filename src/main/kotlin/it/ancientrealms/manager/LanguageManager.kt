package it.ancientrealms.manager

import it.ancientrealms.Fortress
import it.tigierrei.configapi.Config
import org.bukkit.ChatColor
import java.nio.file.Files
import java.nio.file.Paths

class LanguageManager {

    private val plugin: Fortress = Fortress.INSTANCE
    private val messages = HashMap<String, String>()

    init {
        loadLanguage()
    }

    fun getMessage(messageKey: String, vararg args: String): String {
        var message = ChatColor.translateAlternateColorCodes('&', messages[messageKey] ?: "")
        args.forEachIndexed { index, arg ->
            message = message.replace("\${${index}}", arg)
        }
        return message
    }

    fun loadLanguage(){
        messages.clear()
        Files.createDirectories(Paths.get(plugin.dataFolder.toPath().toString(), "lang/"))
        val languageFile = Config("lang/" + plugin.pluginConfig.config.getString("language") + ".yml", plugin)
        languageFile.config.getKeys(false).forEach { key -> messages[key] = languageFile.config.getString(key)!! }
    }

}