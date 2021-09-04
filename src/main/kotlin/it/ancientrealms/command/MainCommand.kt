package it.ancientrealms.command

import net.md_5.bungee.api.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import java.util.*
import kotlin.collections.HashMap

class MainCommand : CommandExecutor {

    private val subcommands = HashMap<List<String>, SubCommand>()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        var match = false
        if(args.isNotEmpty()){
            subcommands.filterKeys { k -> k.contains(args[0].lowercase(Locale.getDefault())) }.forEach{ (_, v) ->
                run {
                    match = true
                    if(v.getPermission().isNullOrBlank() || sender.hasPermission(v.getPermission()!!) || sender.isOp){
                        v.runCommand(sender, command, label, args)
                    }else{
                        sender.sendMessage("${ChatColor.RED}You don't have the permissions to run that command")
                    }
                }
            }
        }
        if(!match){
            subcommands.filterKeys{ k -> k.contains("help") }.forEach { (_, v) -> v.runCommand(sender, command, label, args) }
        }
        return true
    }

    fun addSubCommand(aliases: List<String>, subCommand: SubCommand) : MainCommand{
        subcommands[aliases] = subCommand
        return this
    }
}