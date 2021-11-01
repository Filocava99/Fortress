package it.ancientrealms.command

import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

abstract class SubCommand {

    abstract fun getPermission(): String?

    abstract fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>)

    open fun runCommand(sender: CommandSender, cmd: Command?, label: String?, args: Array<out String>): Boolean {
        if (!sender.hasPermission(getPermission()!!)) {
            return false
        }
        if (sender !is Player) {
            sender.sendMessage(ChatColor.RED.toString() + "Only players may try to execute this command!")
            return false
        }
        onCommand(sender, cmd, label, args)
        return true
    }

}