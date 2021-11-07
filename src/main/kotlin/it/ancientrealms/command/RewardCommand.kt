package it.ancientrealms.command

import it.ancientrealms.Fortress
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack

class RewardCommand : SubCommand() {

    private val languageManager = Fortress.INSTANCE.languageManager

    override fun getPermission(): String = "fortress.admin.addreward"

    // /fortress reward add <fortress> <material> <amount>
    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if (args.size >= 2) {
            when (args[1]) {
                "add" -> {
                    if (args.size < 5) {
                        sender?.sendMessage(languageManager.getMessage("add-reward-invalid-parameters"))
                    } else {
                        Fortress.INSTANCE.fortressesManager.getFortress(args[2])?.itemsReward?.add(
                            ItemStack(
                                Material.valueOf(
                                    args[3]
                                ), Integer.parseInt(args[4])
                            )
                        )
                        sender?.sendMessage(languageManager.getMessage("item-reward-added", args[4], args[3], args[2]))
                    }
                }
                "remove" -> {
                    if (args.size < 4) {
                        sender?.sendMessage("remove-reward-invalid-parameters")
                    } else {
                        Fortress.INSTANCE.fortressesManager.getFortress(args[2])?.itemsReward?.removeIf {
                            it.data?.itemType == Material.valueOf(
                                args[3]
                            )
                        }
                        sender?.sendMessage(languageManager.getMessage("item-reward-added", args[3], args[2]))
                    }
                }
            }
        }
    }
}