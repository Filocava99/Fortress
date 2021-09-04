package it.ancientrealms.command

import it.ancientrealms.Fortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class ReloadCommand : SubCommand() {
    override fun getPermission(): String? {
        return "arfortress.admin.reload"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("configs-reloaded"))
        Fortress.INSTANCE.loadConfig()
        Fortress.INSTANCE.loadData()
        Fortress.INSTANCE.languageManager.loadLanguage()
    }
}