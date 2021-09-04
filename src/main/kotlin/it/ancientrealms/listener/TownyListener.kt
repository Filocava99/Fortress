package it.ancientrealms.listener

import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent
import it.ancientrealms.ARFortress
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class TownyListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTownUpkeep(event: TownUpkeepCalculationEvent) {
        val town = event.town
        var totalDiscount = 0f
        val isCapital = town.hasNation() && town.nation.capital == town
        ARFortress.INSTANCE.fortressesManager.getFortresses()
            .filter { fortress -> (fortress.owner == town || fortress.owner == town.nation) && fortress.onlyCapitalGetsBonus == isCapital }
            .forEach { fortress -> totalDiscount += fortress.townUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount/100
    }

    @EventHandler
    fun onNationUpkeep(event: NationUpkeepCalculationEvent){
        val nation = event.nation
        var totalDiscount = 0f
        ARFortress.INSTANCE.fortressesManager.getFortresses().filter { fortress ->  fortress.owner == nation}.forEach { fortress -> totalDiscount += fortress.nationUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount/100
    }

}