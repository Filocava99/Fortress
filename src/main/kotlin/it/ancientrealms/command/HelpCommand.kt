package it.ancientrealms.command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class HelpCommand : SubCommand() {
    override fun getPermission(): String = "fortress.help"

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        TODO("Not yet implemented")
    }
}