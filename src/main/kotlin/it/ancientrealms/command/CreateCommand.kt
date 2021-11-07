package it.ancientrealms.command

import it.ancientrealms.Fortress
import it.ancientrealms.FortressModel
import it.ancientrealms.NamespacedKeys
import org.bukkit.Chunk
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.text.SimpleDateFormat
import java.util.*

class CreateCommand : SubCommand() {

    override fun getPermission(): String {
        return "fortress.admin.create"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if (sender is Player) {
            if (args.size < 6) {
                sender.sendMessage(Fortress.INSTANCE.languageManager.getMessage("create-invalid-parameters"))
            }
            val radius = Integer.parseInt(args[5])
            val location = sender.location.chunk
            val chunks = HashSet<Chunk>()
            for (x in (location.x.toInt() - radius)..(location.x.toInt() + radius)) {
                for (z in (location.z.toInt() - radius)..(location.z.toInt() + radius)) {
                    chunks.add(location.world.getChunkAt(x, z).also {
                        it.persistentDataContainer.set(
                            NamespacedKeys.FORTRESS.namespacedKey,
                            PersistentDataType.STRING,
                            args[1]
                        )
                    })
                }
            }
            val name = args[1]
            val besiegeHour = Integer.parseInt(args[2])
            val besiegeDays = args[3].split(",").map { it -> Integer.parseInt(it) }.toSet()
            val besiegePeriod = Integer.parseInt(args[4])
            val lastTimeBesieged = SimpleDateFormat("yyyy-MM-dd").format(
                Date.from(
                    Calendar.getInstance().time.toInstant().minusMillis(100000)
                )
            )
            val fortress = FortressModel(
                name,
                null,
                chunks,
                besiegeHour,
                besiegeDays,
                besiegePeriod,
                lastTimeBesieged,
                radius = radius
            )
            Fortress.INSTANCE.run{
                fortressesManager.addFortress(args[1], fortress)
                fortressesConfig.config.set(args[1], fortress)
                fortressesConfig.save()
            }
            sender.sendMessage(Fortress.INSTANCE.languageManager.getMessage("fortress-created", name))
        } else {
            sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("only-players-can-run-command"))
        }
    }
}