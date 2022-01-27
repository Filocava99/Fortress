package it.ancientrealms.command

import com.palmergames.bukkit.towny.TownyUniverse
import it.ancientrealms.Fortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class SetOwnerCommand : SubCommand() {
    override fun getPermission(): String = "fortress.admin.setowner"

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(args.size < 3){
            sender?.sendMessage("Use /fortress setowner <fortress> <town>")
        }else{
            Fortress.INSTANCE.fortressesManager.getFortress(args[1])?.owner = TownyUniverse.getInstance().getTown(args[2])
            Fortress.INSTANCE.fortressesManager.saveFortresses()
            sender?.sendMessage("Fortress ${args[1]} now owned by ${args[2]}")
        }
    }
}