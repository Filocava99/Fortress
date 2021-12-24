package it.ancientrealms.command

import it.ancientrealms.Fortress
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

class InfoCommand : SubCommand() {

    private val languageManager = Fortress.INSTANCE.languageManager

    override fun getPermission(): String = "fortress.info"

    override fun onCommand(sender: CommandSender?, cmd: Command?, label: String?, args: Array<out String>) {
        if (args.size < 2) {
            sender?.sendMessage(languageManager.getMessage("info-invalid-parameters"))
        } else {
            sender?.run {
                val fortress = Fortress.INSTANCE.fortressesManager.getFortress(args[1])
                if (fortress == null) {
                    sendMessage(languageManager.getMessage("invalid-fortress-name"))
                } else {
                    sendMessage(languageManager.getMessage("info-fortress-name", fortress.name))
                    sendMessage(
                        languageManager.getMessage(
                            "info-fortress-owner",
                            fortress.owner?.name ?: languageManager.getMessage("no-one")
                        )
                    )
                    sendMessage(
                        languageManager.getMessage(
                            "info-fortress-daily-money",
                            fortress.dailyMoneyBonus.toString(),
                            languageManager.getMessage("currency-symbol")
                        )
                    )
                }
            }
        }
    }
}