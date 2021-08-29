package it.ancientrealms.command

import it.ancientrealms.ARFortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class SetHourCommand : SubCommand() {

    override fun getPermission(): String? {
        return "fortress.admin.sethour"
    }

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if(args.size < 3){
            sender?.sendMessage(ARFortress.INSTANCE.languageManager.getMessage("sethour-invalid-parameters"))
        }
        val fortressName = args[1]
        try {
            val hour = Integer.parseInt(args[2])
            val fortress = ARFortress.INSTANCE.fortressesManager.getFortress(fortressName)
            fortress?.let {
                it.besiegeHour = hour
            }
            ARFortress.INSTANCE.fortressesManager.saveFortresses()
        }catch (e: NumberFormatException){
            sender?.sendMessage(ARFortress.INSTANCE.languageManager.getMessage("invalid-hour-format"))
        }
    }
}