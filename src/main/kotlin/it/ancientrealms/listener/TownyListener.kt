package it.ancientrealms.listener

import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent
import com.palmergames.bukkit.towny.event.NewDayEvent
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent
import it.ancientrealms.Fortress
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class TownyListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onNewDay(@Suppress("UNUSED_PARAMETER") event: NewDayEvent){
        Fortress.INSTANCE.fortressesManager.getFortresses().filter { fortress -> fortress.owner != null }.forEach {
            it.owner?.let {
                town -> town.depositToBank(town.mayor, it.dailyMoneyBonus)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTownUpkeep(event: TownUpkeepCalculationEvent) {
        val town = event.town
        var totalDiscount = 0f
        Fortress.INSTANCE.fortressesManager.getFortresses()
            .filter { fortress -> fortress.owner != null && (fortress.owner == town || (fortress.owner!!.nation == town.nation && !fortress.onlyCapitalGetsBonus)) }
            .forEach { fortress -> totalDiscount += fortress.townUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount/100
    }

    @EventHandler
    fun onNationUpkeep(event: NationUpkeepCalculationEvent){
        var totalDiscount = 0f
        Fortress.INSTANCE.fortressesManager.getFortresses().filter { fortress ->  fortress.owner != null && fortress.owner!!.hasNation() && fortress.owner!!.nation.getUUID() == event.nation.getUUID()}.forEach { fortress -> totalDiscount += fortress.nationUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount/100
    }

}