package it.ancientrealms.command

import it.ancientrealms.ARFortress
import it.ancientrealms.NamespacedKeys
import it.ancientrealms.models.Fortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.text.SimpleDateFormat
import java.util.*

class CreateCommand : SubCommand() {

    override fun getPermission(): String? {
        return "fortress.admin.create"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(sender is Player){
            val player = sender as Player
            player.location.chunk.persistentDataContainer.set(NamespacedKeys.FORTRESS.namespacedKey,
                PersistentDataType.STRING, args[1])
            val fortress = Fortress(args[1], null, player.location.chunk, Integer.parseInt(args[2]),
                SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().time))
            ARFortress.INSTANCE.fortressesManager.addFortress(args[1], fortress)
            ARFortress.INSTANCE.fortressesConfig.config.set(args[1], fortress)
            ARFortress.INSTANCE.fortressesConfig.save()
            sender.sendMessage(ARFortress.INSTANCE.languageManager.getMessage("fortress-created", args[1]))
        }
    }
}