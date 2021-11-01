package it.ancientrealms.command

import it.ancientrealms.Fortress
import it.ancientrealms.NamespacedKeys
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.text.SimpleDateFormat
import java.util.*
import it.ancientrealms.FortressModel

class CreateCommand : SubCommand() {

    override fun getPermission(): String {
        return "fortress.admin.create"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(sender is Player){
            if(args.size < 5){
                sender.sendMessage(Fortress.INSTANCE.languageManager.getMessage("create-invalid-parameters"))
            }
            sender.location.chunk.persistentDataContainer.set(NamespacedKeys.FORTRESS.namespacedKey,
                PersistentDataType.STRING, args[1])
            val name = args[1]
            val location = sender.location.chunk
            val besiegeHour = Integer.parseInt(args[2])
            val besiegeDays = args[3].split(",").map { it -> Integer.parseInt(it) }.toSet()
            val besiegePeriod = Integer.parseInt(args[4])
            val lastTimeBesieged = SimpleDateFormat("yyyy-MM-dd").format(Date.from(Calendar.getInstance().time.toInstant().minusMillis(87000)))
            val fortress = FortressModel(name, null, location, besiegeHour , besiegeDays, besiegePeriod, lastTimeBesieged)
            Fortress.INSTANCE.fortressesManager.addFortress(args[1], fortress)
            Fortress.INSTANCE.fortressesConfig.config.set(args[1], fortress)
            Fortress.INSTANCE.fortressesConfig.save()
            sender.sendMessage(Fortress.INSTANCE.languageManager.getMessage("fortress-created", name))
        }else{
            sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("only-players-can-run-command"))
        }
    }
}