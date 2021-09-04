package it.ancientrealms.command

import it.ancientrealms.ARFortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class ReloadCommand : SubCommand() {
    override fun getPermission(): String? {
        return "arfortress.admin.reload"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        sender?.sendMessage(ARFortress.INSTANCE.languageManager.getMessage("configs-reloaded"))
        ARFortress.INSTANCE.loadConfig()
        ARFortress.INSTANCE.loadData()
        ARFortress.INSTANCE.languageManager.loadLanguage()
    }
}