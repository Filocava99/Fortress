package it.ancientrealms.command

import it.ancientrealms.Fortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class SetHourCommand : SubCommand() {

    override fun getPermission(): String? {
        return "fortress.admin.sethour"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(args.size < 3){
            sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("sethour-invalid-parameters"))
        }
        val fortressName = args[1]
        try {
            val hour = Integer.parseInt(args[2])
            val fortress = Fortress.INSTANCE.fortressesManager.getFortress(fortressName)
            fortress?.let {
                it.besiegeHour = hour
            }
            Fortress.INSTANCE.fortressesManager.saveFortresses()
            sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("hour-set", args[2], fortressName))
        }catch (e: NumberFormatException){
            sender?.sendMessage(Fortress.INSTANCE.languageManager.getMessage("invalid-hour-format"))
        }
    }
}