package it.ancientrealms.listener

import com.palmergames.bukkit.towny.TownyAPI
import com.palmergames.bukkit.towny.TownyUniverse
import com.palmergames.bukkit.towny.`object`.Town
import com.palmergames.bukkit.towny.event.NationUpkeepCalculationEvent
import com.palmergames.bukkit.towny.event.NewDayEvent
import com.palmergames.bukkit.towny.event.TownUpkeepCalculationEvent
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePVPEvent
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException
import it.ancientrealms.Fortress
import it.ancientrealms.utils.Utils
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

class TownyListener : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onNewDay(@Suppress("UNUSED_PARAMETER") event: NewDayEvent) {
        Fortress.INSTANCE.fortressesManager.getFortresses().filter { fortress -> fortress.owner != null }.forEach {
            it.owner?.collect(it.dailyMoneyBonus.toDouble())
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onTownUpkeep(event: TownUpkeepCalculationEvent) {
        val town = event.town
        var totalDiscount = 0f
        Fortress.INSTANCE.fortressesManager.getFortresses()
            .filter { fortress ->
                fortress.owner != null && (fortress.owner == town || (Utils.Companion.getNation(fortress.owner!!) == Utils.Companion.getNation(
                    town
                ) && !fortress.onlyCapitalGetsBonus))
            }
            .forEach { fortress -> totalDiscount += fortress.townUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount / 100
    }

    @EventHandler
    fun onNationUpkeep(event: NationUpkeepCalculationEvent) {
        var totalDiscount = 0f
        Fortress.INSTANCE.fortressesManager.getFortresses()
            .filter { fortress -> fortress.owner != null && fortress.owner!!.hasNation() && fortress.owner!!.nation.getUUID() == event.nation.getUUID() }
            .forEach { fortress -> totalDiscount += fortress.nationUpkeepDiscount }
        event.upkeep -= event.upkeep * totalDiscount / 100
    }

    @EventHandler
    fun onTownDisband(event: TownRuinedEvent){
        removeOwnedFortress(event.town)
    }

    @EventHandler
    fun onTownTogglePVP(event: TownTogglePVPEvent){
        removeOwnedFortress(event.town)
    }

    private fun removeOwnedFortress(town: Town){
        Fortress.INSTANCE.fortressesManager.getFortresses().forEach{
            if(it.owner == town){
                it.owner = null
                Fortress.INSTANCE.fortressesConfig.config.set(it.name, it)
            }
        }
    }

}