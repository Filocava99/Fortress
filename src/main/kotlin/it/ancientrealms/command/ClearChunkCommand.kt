package it.ancientrealms.command

import it.ancientrealms.NamespacedKeys
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class ClearChunkCommand : SubCommand() {
    override fun getPermission(): String = "fortress.clearchunk"

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(sender is Player){
            sender.location.chunk.persistentDataContainer.remove(NamespacedKeys.FORTRESS.namespacedKey)
        }
    }
}