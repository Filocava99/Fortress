package it.ancientrealms.command

import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class MainCommand : CommandExecutor {

    private val subcommands = HashMap<List<String>, SubCommand>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        subcommands.filterKeys { k -> k.contains(args[0]) }.forEach{(k, v) ->
            run {
                if(v.getPermission().isNullOrBlank() || sender.hasPermission(v.getPermission()!!) || sender.isOp){
                    v.runCommand(sender, command, label, args)
                }else{
                    sender.sendMessage("${ChatColor.RED}You don't have the permissions to run that command")
                }
            }
        }
        return true
    }

    fun addSubCommand(aliases: List<String>, subCommand: SubCommand) : MainCommand{
        subcommands[aliases] = subCommand
        return this
    }
}